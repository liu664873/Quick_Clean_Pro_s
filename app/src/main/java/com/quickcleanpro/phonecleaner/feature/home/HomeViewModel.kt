package com.quickcleanpro.phonecleaner.feature.home

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.BatteryInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.StorageInfo
import com.quickcleanpro.phonecleaner.feature.applock.AppLockRepository
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.DeviceInfoRepository
import com.quickcleanpro.phonecleaner.feature.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val AUTO_RATE_PROMPT_DELAY_MS = 15_000L
private const val AUTO_RATE_PROMPT_COOLDOWN_MS = 24L * 60 * 60 * 1000

data class HomeSummaryUiState(
    val storageInfo: StorageInfo = StorageInfo(0, 0, 0),
    val batteryInfo: BatteryInfo = BatteryInfo(-1, "Unknown", 0f, -1, "Unknown", 0, "Unknown"),
    val batteryStatusText: String = "Unknown",
    val deviceModel: String = "Unknown",
    val androidVersion: String = "Android --",
    val lockedAppCount: Int = 0,
    val isLoading: Boolean = true,
)

data class HomeUiState(
    val summary: HomeSummaryUiState = HomeSummaryUiState(),
    val exitPromptRoute: String? = null,
    val showAutoRateDialog: Boolean = false,
    val externalBlockingPromptActive: Boolean = false,
)

sealed interface HomeAction {
    data object RequestExitPrompt : HomeAction
    data object DismissExitPrompt : HomeAction
    data object DismissAutoRateDialog : HomeAction
    data object FeatureClicked : HomeAction
    data object TabInteracted : HomeAction
    data object RefreshSummary : HomeAction
    data class ExternalBlockingPromptChanged(val active: Boolean) : HomeAction
    data object ExitApp : HomeAction
    data object RateApp : HomeAction
}

class HomeViewModel(
    private val repository: DeviceInfoRepository,
    private val appLockRepository: AppLockRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val autoRateDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val autoRatePromptDelayMillis: Long = AUTO_RATE_PROMPT_DELAY_MS,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var autoRatePromptJob: Job? = null
    private var featureClicked = false
    private var externalBlockingPromptActive = false

    init {
        refreshSummary()
        scheduleAutoRatePrompt()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.RequestExitPrompt -> requestExitPrompt()
            HomeAction.DismissExitPrompt -> dismissExitPrompt()
            HomeAction.DismissAutoRateDialog -> dismissAutoRateDialog()
            HomeAction.FeatureClicked -> onFeatureClicked()
            HomeAction.TabInteracted -> onTabInteraction()
            HomeAction.RefreshSummary -> refreshSummary()
            is HomeAction.ExternalBlockingPromptChanged -> setExternalBlockingPromptActive(action.active)
            HomeAction.ExitApp -> Unit
            HomeAction.RateApp -> Unit
        }
    }

    fun requestExitPrompt() {
        if (_uiState.value.exitPromptRoute != null) return
        autoRatePromptJob?.cancel()
        _uiState.value = _uiState.value.copy(exitPromptRoute = nextExitPromptRoute())
    }

    fun dismissExitPrompt() {
        _uiState.value = _uiState.value.copy(exitPromptRoute = null)
    }

    fun dismissAutoRateDialog() {
        _uiState.value = _uiState.value.copy(showAutoRateDialog = false)
    }

    fun onFeatureClicked() {
        featureClicked = true
        autoRatePromptJob?.cancel()
        _uiState.value = _uiState.value.copy(showAutoRateDialog = false)
    }

    fun setExternalBlockingPromptActive(active: Boolean) {
        if (externalBlockingPromptActive == active) return
        externalBlockingPromptActive = active
        _uiState.value = _uiState.value.copy(externalBlockingPromptActive = active)
        if (active) {
            autoRatePromptJob?.cancel()
            _uiState.value = _uiState.value.copy(showAutoRateDialog = false)
            return
        }
        scheduleAutoRatePrompt()
    }

    fun onTabInteraction() {
        onFeatureClicked()
    }

    fun consumeExitPromptForNavigation(): String? {
        val route = _uiState.value.exitPromptRoute ?: return null
        onFeatureClicked()
        _uiState.value = _uiState.value.copy(exitPromptRoute = null)
        return route
    }

    fun refreshSummary() {
        viewModelScope.launch(ioDispatcher) {
            val currentState = _uiState.value.summary
            val loadedState =
                runCatching {
                    val hardware = repository.hardwareInfo()
                    HomeSummaryUiState(
                        storageInfo = repository.internalStorageInfo(),
                        batteryInfo = repository.batteryInfo(),
                        batteryStatusText = repository.batteryStatusInfo().statusText,
                        deviceModel = hardware.model,
                        androidVersion = hardware.androidVersion,
                        lockedAppCount = runCatching { appLockRepository.lockedAppCount() }.getOrDefault(0),
                        isLoading = false,
                    )
                }.getOrElse {
                    currentState.copy(isLoading = false)
                }
            _uiState.value = _uiState.value.copy(summary = loadedState)
        }
    }

    private fun nextExitPromptRoute(): String {
        val notificationCleanerRoute = AppDestination.NotificationCleaner.route
        if (!settingsRepository.hasShownNotificationCleanerExitPrompt()) {
            settingsRepository.saveNotificationCleanerExitPromptShown()
            return notificationCleanerRoute
        }
        val suggestions = listOf(
            AppDestination.DeviceInfo.route,
            AppDestination.JunkClean.route,
            AppDestination.BatteryInfo.route,
            AppDestination.NetworkScan.route,
            AppDestination.NetworkUsage.route,
        )
        return suggestions.randomOrNull() ?: notificationCleanerRoute
    }

    private fun scheduleAutoRatePrompt() {
        autoRatePromptJob?.cancel()
        if (!canShowAutoRatePrompt()) return
        if (externalBlockingPromptActive) return

        autoRatePromptJob =
            viewModelScope.launch(autoRateDispatcher) {
                delay(autoRatePromptDelayMillis)
                if (featureClicked) return@launch
                if (externalBlockingPromptActive) return@launch
                settingsRepository.saveLastAutoRatePromptAt(System.currentTimeMillis())
                _uiState.value = _uiState.value.copy(showAutoRateDialog = true)
            }
    }

    private fun canShowAutoRatePrompt(): Boolean {
        val lastPromptAt = settingsRepository.readLastAutoRatePromptAt()
        if (lastPromptAt <= 0L) return true
        return System.currentTimeMillis() - lastPromptAt >= AUTO_RATE_PROMPT_COOLDOWN_MS
    }

    override fun onCleared() {
        autoRatePromptJob?.cancel()
        super.onCleared()
    }
}
