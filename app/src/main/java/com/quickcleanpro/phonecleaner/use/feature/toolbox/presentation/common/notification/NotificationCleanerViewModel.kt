package com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.notification

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.common.coroutines.runSuspendCatching
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.model.notification.BlockableNotificationApp
import com.quickcleanpro.phonecleaner.use.core.repository.NotificationRepository
import com.quickcleanpro.phonecleaner.use.feature.notification.data.AndroidNotificationSettingsGateway
import com.quickcleanpro.phonecleaner.use.feature.notification.data.NotificationSettingsGateway
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NotificationCleanerPage {
    Onboarding,
    Scanning,
    Status,
    Settings,
}

data class NotificationCleanerUiState(
    val hasAccess: Boolean = false,
    val enabled: Boolean = false,
    val blockedCount: Int = 0,
    val blockedCountsByPackage: Map<String, Int> = emptyMap(),
    val apps: List<BlockableNotificationApp> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val page: NotificationCleanerPage = NotificationCleanerPage.Onboarding,
    val isLoading: Boolean = true,
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,
)

class NotificationCleanerViewModel(
    application: Application,
    private val repository: NotificationRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val settingsGateway: NotificationSettingsGateway = AndroidNotificationSettingsGateway(),
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NotificationCleanerUiState())
    private val operationEventsChannel = Channel<FeatureOperationEvent>(Channel.BUFFERED)
    private var hasShownAccessScan = false
    private var scanningCompletionPending = false

    val uiState: StateFlow<NotificationCleanerUiState> = _uiState.asStateFlow()
    val operationEvents: Flow<FeatureOperationEvent> = operationEventsChannel.receiveAsFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(ioDispatcher) {
            runSuspendCatching { readSnapshot() }
                .onSuccess { snapshot ->
                    _uiState.update { state ->
                        if (!snapshot.hasAccess) {
                            hasShownAccessScan = false
                            scanningCompletionPending = false
                        }
                        val nextPage =
                            when {
                                state.page == NotificationCleanerPage.Settings -> NotificationCleanerPage.Settings
                                state.page == NotificationCleanerPage.Scanning -> NotificationCleanerPage.Scanning
                                snapshot.hasAccess && !hasShownAccessScan -> {
                                    hasShownAccessScan = true
                                    NotificationCleanerPage.Scanning
                                }
                                snapshot.hasAccess -> NotificationCleanerPage.Status
                                else -> NotificationCleanerPage.Onboarding
                            }
                        state.copy(
                            hasAccess = snapshot.hasAccess,
                            enabled = snapshot.enabled,
                            blockedCount = snapshot.blockedCount,
                            blockedCountsByPackage = snapshot.blockedCountsByPackage,
                            apps = snapshot.apps,
                            selectedPackages = snapshot.selectedPackages,
                            page = nextPage,
                            isLoading = false,
                            isInitialized = true,
                            errorMessage = null,
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isInitialized = true,
                            errorMessage = error.message ?: appString(R.string.notification_status_load_failed),
                        )
                    }
                }
        }
    }

    fun setBlockingEnabled(checked: Boolean): Boolean {
        val hasAccess = repository.hasNotificationListenerAccess()
        if (checked && !hasAccess) {
            _uiState.update {
                it.copy(hasAccess = false, enabled = false, isLoading = false)
            }
            return true
        }

        repository.setNotificationBlockingEnabled(checked)
        refreshState()
        return false
    }

    fun togglePackage(packageName: String) {
        if (!_uiState.value.enabled) return
        val selected = packageName !in _uiState.value.selectedPackages
        viewModelScope.launch(ioDispatcher) {
            runSuspendCatching {
                repository.setNotificationPackageSelected(packageName, selected)
                repository.selectedNotificationPackages()
            }.onSuccess { selectedPackages ->
                _uiState.update { it.copy(selectedPackages = selectedPackages, errorMessage = null) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: appString(R.string.notification_app_selection_update_failed))
                }
            }
        }
    }

    fun showSettings() {
        _uiState.update { it.copy(page = NotificationCleanerPage.Settings) }
    }

    fun leaveSettings() {
        _uiState.update { it.copy(page = NotificationCleanerPage.Status) }
    }

    fun finishScanning() {
        if (_uiState.value.page != NotificationCleanerPage.Scanning || scanningCompletionPending) return
        scanningCompletionPending = true
        trackOperationEvent(
            FeatureOperationEvent.OperationFinished(
                FeatureKey.NOTIFICATION_CLEANER,
                OperationAction.CLEAN,
                success = true,
            ),
        )
    }

    fun showStatusAfterCompletionAd() {
        scanningCompletionPending = false
        _uiState.update {
            if (it.page == NotificationCleanerPage.Scanning) {
                it.copy(page = NotificationCleanerPage.Status)
            } else {
                it
            }
        }
    }

    fun cancelScanning() {
        scanningCompletionPending = false
        _uiState.update {
            if (it.page == NotificationCleanerPage.Scanning) {
                it.copy(page = NotificationCleanerPage.Status)
            } else {
                it
            }
        }
    }

    fun restartScanning() {
        scanningCompletionPending = false
        _uiState.update {
            if (it.page != NotificationCleanerPage.Scanning) {
                it.copy(page = NotificationCleanerPage.Scanning)
            } else {
                it
            }
        }
    }

    fun notificationListenerSettingsIntent(): Intent = settingsGateway.notificationListenerSettingsIntent()

    fun appDetailsSettingsIntent(packageName: String): Intent = settingsGateway.appDetailsSettingsIntent(packageName)

    private fun readSnapshot(): NotificationCleanerSnapshot =
        NotificationCleanerSnapshot(
            hasAccess = repository.hasNotificationListenerAccess(),
            enabled = repository.isNotificationBlockingEnabled(),
            blockedCount = repository.blockedNotificationCount(),
            blockedCountsByPackage = repository.blockedNotificationCountsByPackage(),
            apps = repository.notificationApps(),
            selectedPackages = repository.selectedNotificationPackages(),
        )

    private fun appString(resId: Int): String = getApplication<Application>().getString(resId)

    private fun trackOperationEvent(event: FeatureOperationEvent) {
        operationEventsChannel.trySend(event)
    }
}

private data class NotificationCleanerSnapshot(
    val hasAccess: Boolean,
    val enabled: Boolean,
    val blockedCount: Int,
    val blockedCountsByPackage: Map<String, Int>,
    val apps: List<BlockableNotificationApp>,
    val selectedPackages: Set<String>,
)

