package com.quickcleanpro.phonecleaner.feature.toolbox.networkusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.common.coroutines.runSuspendCatching
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkUsageApp
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkUsageInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.UNKNOWN_NETWORK_TRAFFIC_PACKAGE
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class NetworkUsageTab {
    Cellular,
    Wifi,
}

data class NetworkUsageDisplayItem(
    val appName: String,
    val packageName: String,
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long,
    val isAggregate: Boolean = false,
) {
    companion object {
        fun fromApp(app: NetworkUsageApp): NetworkUsageDisplayItem =
            NetworkUsageDisplayItem(
                appName = app.appName,
                packageName = app.packageName,
                rxBytes = app.rxBytes,
                txBytes = app.txBytes,
                totalBytes = app.totalBytes,
                isAggregate = app.packageName == UNKNOWN_NETWORK_TRAFFIC_PACKAGE,
            )

        fun systemTraffic(totalBytes: Long): NetworkUsageDisplayItem =
            NetworkUsageDisplayItem(
                appName = "System & unknown traffic",
                packageName = UNKNOWN_NETWORK_TRAFFIC_PACKAGE,
                rxBytes = totalBytes,
                txBytes = 0L,
                totalBytes = totalBytes,
                isAggregate = true,
            )
    }
}

data class NetworkUsageUiState(
    val selectedTab: NetworkUsageTab = NetworkUsageTab.Wifi,
    val hasAccess: Boolean = false,
    val usage: NetworkUsageInfo? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isScanning: Boolean = false,
    val showStopDialog: Boolean = false,
    val permissionRequestPending: Boolean = false,
) {
    val selectedIndex: Int get() = selectedTab.ordinal

    val cellularTotalBytes: Long get() = usage?.cellularTotalBytes ?: 0L

    val wifiTotalBytes: Long get() = usage?.wifiTotalBytes ?: 0L

    fun rxBytes(tab: NetworkUsageTab): Long =
        usage?.let {
            when (tab) {
                NetworkUsageTab.Cellular -> it.cellularRxBytes
                NetworkUsageTab.Wifi -> it.wifiRxBytes
            }
        } ?: 0L

    fun txBytes(tab: NetworkUsageTab): Long =
        usage?.let {
            when (tab) {
                NetworkUsageTab.Cellular -> it.cellularTxBytes
                NetworkUsageTab.Wifi -> it.wifiTxBytes
            }
        } ?: 0L

    fun totalBytes(tab: NetworkUsageTab): Long = rxBytes(tab) + txBytes(tab)

    fun apps(tab: NetworkUsageTab): List<NetworkUsageApp> =
        usage?.let {
            when {
                tab == NetworkUsageTab.Cellular && it.cellularApps.isNotEmpty() -> it.cellularApps
                tab == NetworkUsageTab.Wifi && it.wifiApps.isNotEmpty() -> it.wifiApps
                it.fallbackApps.isNotEmpty() -> it.fallbackApps
                else -> emptyList()
            }
        }.orEmpty()

    fun displayItems(tab: NetworkUsageTab): List<NetworkUsageDisplayItem> =
        buildNetworkUsageDisplayItems(
            apps = apps(tab),
            selectedTotalBytes = totalBytes(tab),
        )

    val selectedRxBytes: Long
        get() = rxBytes(selectedTab)

    val selectedTxBytes: Long
        get() = txBytes(selectedTab)

    val selectedTotalBytes: Long get() = totalBytes(selectedTab)

    val selectedApps: List<NetworkUsageApp>
        get() = apps(selectedTab)

    val displayItems: List<NetworkUsageDisplayItem>
        get() = displayItems(selectedTab)
}

enum class NetworkUsageExitReason {
    Return,
    PermissionRejected,
}

sealed interface NetworkUsageEffect {
    data class Exit(val reason: NetworkUsageExitReason) : NetworkUsageEffect
}

sealed interface NetworkUsageAction {
    data object Resumed : NetworkUsageAction
    data object PermissionRequestConsumed : NetworkUsageAction
    data object PermissionGranted : NetworkUsageAction
    data object PermissionRejected : NetworkUsageAction
    data object Back : NetworkUsageAction
    data object QuitScan : NetworkUsageAction
    data object ResumeScan : NetworkUsageAction
    data class TabSelected(val index: Int) : NetworkUsageAction
}

class NetworkUsageViewModel(
    private val repository: NetworkRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            NetworkUsageUiState(
                hasAccess = repository.hasNetworkUsageAccess(),
            ),
        )

    val uiState: StateFlow<NetworkUsageUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<NetworkUsageEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<NetworkUsageEffect> = _effects.asSharedFlow()
    private var scanDelayFinished = false
    private var scanJob: Job? = null
    private var permissionRequestAttempted = false

    init {
        refreshUsage(startScan = true)
    }

    fun onAction(action: NetworkUsageAction) {
        when (action) {
            NetworkUsageAction.Resumed -> refreshUsage(startScan = true)
            NetworkUsageAction.PermissionRequestConsumed -> {
                permissionRequestAttempted = true
                _uiState.update { it.copy(permissionRequestPending = false) }
            }
            NetworkUsageAction.PermissionGranted -> refreshUsage(startScan = true)
            NetworkUsageAction.PermissionRejected -> exit(NetworkUsageExitReason.PermissionRejected)
            NetworkUsageAction.Back -> {
                val state = _uiState.value
                when {
                    state.hasAccess && state.isScanning -> _uiState.update { it.copy(showStopDialog = true) }
                    !state.hasAccess -> exit(NetworkUsageExitReason.PermissionRejected)
                    else -> exit(NetworkUsageExitReason.Return)
                }
            }
            NetworkUsageAction.QuitScan -> {
                _uiState.update { it.copy(showStopDialog = false) }
                exit(NetworkUsageExitReason.Return)
            }
            NetworkUsageAction.ResumeScan -> startScanDelay()
            is NetworkUsageAction.TabSelected -> selectTab(action.index)
        }
    }

    fun selectTab(index: Int) {
        val tab = NetworkUsageTab.entries.getOrNull(index) ?: return
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun refreshUsage(startScan: Boolean) {
        val hasAccess = repository.hasNetworkUsageAccess()
        _uiState.update {
            it.copy(
                hasAccess = hasAccess,
                usage = if (hasAccess) it.usage else null,
                isLoading = hasAccess,
                errorMessage = null,
                isScanning = hasAccess && (startScan || it.isScanning),
                showStopDialog = false,
                permissionRequestPending = !hasAccess && !permissionRequestAttempted,
            )
        }
        if (!hasAccess) {
            scanJob?.cancel()
            return
        }
        if (startScan) startScanDelay()

        viewModelScope.launch(ioDispatcher) {
            runSuspendCatching {
                repository.readNetworkUsage()
            }.onSuccess { usage ->
                _uiState.update {
                    it.copy(
                        hasAccess = !usage.needsUsageAccess,
                        usage = if (usage.needsUsageAccess) null else usage,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                finishScanIfReady()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        hasAccess = repository.hasNetworkUsageAccess(),
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
                finishScanIfReady()
            }
        }
    }

    private fun startScanDelay() {
        scanJob?.cancel()
        scanDelayFinished = false
        _uiState.update { it.copy(isScanning = true, showStopDialog = false) }
        scanJob =
            viewModelScope.launch {
                delay(TOOLBOX_SCAN_DURATION_MILLIS)
                scanDelayFinished = true
                finishScanIfReady()
            }
    }

    private fun finishScanIfReady() {
        if (scanDelayFinished && !_uiState.value.isLoading) {
            _uiState.update { it.copy(isScanning = false, showStopDialog = false) }
        }
    }

    private fun exit(reason: NetworkUsageExitReason) {
        _effects.tryEmit(NetworkUsageEffect.Exit(reason))
    }

    private companion object {
        private const val TOOLBOX_SCAN_DURATION_MILLIS = 1_500L
    }
}

fun buildNetworkUsageDisplayItems(
    apps: List<NetworkUsageApp>,
    selectedTotalBytes: Long,
): List<NetworkUsageDisplayItem> {
    if (selectedTotalBytes <= 0L) return emptyList()

    val items =
        apps
            .filter { it.totalBytes > 0L }
            .map { NetworkUsageDisplayItem.fromApp(it) }
            .sortedByDescending { it.totalBytes }

    val normalizedItems = mutableListOf<NetworkUsageDisplayItem>()
    var remainingBytes = selectedTotalBytes
    for (item in items) {
        if (remainingBytes <= 0L) break
        val displayBytes = item.totalBytes.coerceAtMost(remainingBytes)
        normalizedItems +=
            item.copy(
                rxBytes = displayBytes,
                txBytes = 0L,
                totalBytes = displayBytes,
            )
        remainingBytes -= displayBytes
    }

    if (remainingBytes > 0L) {
        normalizedItems += NetworkUsageDisplayItem.systemTraffic(remainingBytes)
    }

    val aggregateBytes =
        normalizedItems
            .filter { it.isAggregate }
            .sumOf { it.totalBytes }
    val nonAggregateItems = normalizedItems.filterNot { it.isAggregate }

    return (
        nonAggregateItems +
            listOfNotNull(
                aggregateBytes
                    .takeIf { it > 0L }
                    ?.let { NetworkUsageDisplayItem.systemTraffic(it) },
            )
    ).sortedByDescending { it.totalBytes }
}
