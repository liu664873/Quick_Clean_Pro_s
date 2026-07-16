package com.quickcleanpro.phonecleaner.app.runtime.permission

import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModel
import com.quickcleanpro.phonecleaner.app.AppConfig
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.permission.PermissionPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class NotificationPermissionViewModel(
    private val application: Application,
    private val permissionPreferences: PermissionPreferences,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val runtimePermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private var machineState =
        initialNotificationPermissionState(
            runtimePermissionRequired = runtimePermissionRequired,
            facts = readFacts(shouldShowRationale = false),
        )
    private val _uiState = MutableStateFlow(machineState)
    val uiState: StateFlow<NotificationPermissionUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<NotificationPermissionCommand.Host>(Channel.BUFFERED)
    internal val effects = effectsChannel.receiveAsFlow()

    fun onSurfaceChanged(
        surface: NotificationPermissionSurface,
        shouldShowRationale: Boolean,
    ) {
        dispatch(
            NotificationPermissionAction.SurfaceChanged(
                surface = surface,
                facts = readFacts(shouldShowRationale),
            ),
        )
    }

    fun onHomePromptDelayElapsed() {
        dispatch(NotificationPermissionAction.HomePromptDelayElapsed)
    }

    fun onAppResumed(shouldShowRationale: Boolean) {
        dispatch(NotificationPermissionAction.AppResumed(readFacts(shouldShowRationale)))
    }

    fun onSystemPermissionResult(
        granted: Boolean,
        shouldShowRationale: Boolean,
    ) {
        dispatch(
            NotificationPermissionAction.SystemPermissionResult(
                readFacts(
                    shouldShowRationale = shouldShowRationale,
                    grantedOverride = granted,
                ),
            ),
        )
    }

    fun onCustomPromptConfirmed() {
        dispatch(NotificationPermissionAction.CustomPromptConfirmed)
    }

    fun onCustomPromptDismissed() {
        dispatch(NotificationPermissionAction.CustomPromptDismissed)
    }

    fun onSettingsLaunchResult(launched: Boolean) {
        dispatch(NotificationPermissionAction.SettingsLaunchResult(launched))
    }

    private fun dispatch(action: NotificationPermissionAction) {
        if (!runtimePermissionRequired) return
        val transition = reduceNotificationPermissionState(machineState, action, nowMillis())
        machineState = transition.state
        _uiState.value = machineState
        transition.commands.forEach(::handleCommand)
    }

    private fun handleCommand(command: NotificationPermissionCommand) {
        when (command) {
            NotificationPermissionCommand.SaveRequestedBefore ->
                permissionPreferences.saveNotificationRuntimePermissionRequestedBefore()
            is NotificationPermissionCommand.SaveLastCustomPromptAt ->
                permissionPreferences.saveLastNotificationPermissionCustomPromptAt(command.timestampMillis)
            is NotificationPermissionCommand.TrackPopup ->
                AnalyticsTracker.trackNotificationPopup(currentNoticeFlag(), ifOk = command.accepted)
            is NotificationPermissionCommand.TrackPermissionResult ->
                AnalyticsTracker.trackNotificationPermissionResult(command.granted)
            is NotificationPermissionCommand.Host -> effectsChannel.trySend(command)
        }
    }

    private fun readFacts(
        shouldShowRationale: Boolean,
        grantedOverride: Boolean? = null,
    ): NotificationPermissionFacts =
        NotificationPermissionFacts(
            hasPermission = grantedOverride ?: AppConfig.hasPostNotificationsPermission(application),
            hasRequestedBefore = permissionPreferences.hasRequestedNotificationRuntimePermissionBefore(),
            shouldShowRationale = shouldShowRationale,
            lastCustomPromptAt = permissionPreferences.readLastNotificationPermissionCustomPromptAt(),
        )

    private fun currentNoticeFlag(): Int =
        when {
            machineState.surface == NotificationPermissionSurface.Splash -> 1
            AnalyticsTracker.hasCompletedCleanup() -> 3
            else -> 2
        }
}
