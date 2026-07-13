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
    private var machineState = initialState()
    private val _uiState = MutableStateFlow(machineState)
    val uiState: StateFlow<NotificationPermissionUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<NotificationPermissionEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    fun onAction(action: NotificationPermissionAction) {
        if (!runtimePermissionRequired) return
        machineState = refreshSnapshot(machineState, action)
        val transition = reduceNotificationPermissionState(machineState, action, nowMillis())
        machineState = transition.state
        _uiState.value = machineState
        transition.effects.forEach(::handleSideEffect)
    }

    private fun handleSideEffect(effect: NotificationPermissionSideEffect) {
        when (effect) {
            NotificationPermissionSideEffect.SaveRequestedBefore ->
                permissionPreferences.saveNotificationRuntimePermissionRequestedBefore()
            is NotificationPermissionSideEffect.SaveLastCustomPromptAt ->
                permissionPreferences.saveLastNotificationPermissionCustomPromptAt(effect.timestampMillis)
            is NotificationPermissionSideEffect.TrackPopup ->
                AnalyticsTracker.trackNotificationPopup(currentNoticeFlag(), ifOk = effect.accepted)
            is NotificationPermissionSideEffect.TrackPermissionResult ->
                AnalyticsTracker.trackNotificationPermissionResult(effect.granted)
            is NotificationPermissionSideEffect.Host -> effectsChannel.trySend(effect.effect)
        }
    }

    private fun refreshSnapshot(
        state: NotificationPermissionUiState,
        action: NotificationPermissionAction,
    ): NotificationPermissionUiState {
        val rationale =
            when (action) {
                is NotificationPermissionAction.VisibilityChanged -> action.shouldShowRationale
                is NotificationPermissionAction.Refresh -> action.shouldShowRationale
                is NotificationPermissionAction.PermissionResult -> action.shouldShowRationale
                else -> state.shouldShowRationale
            }
        val grantedOverride =
            (action as? NotificationPermissionAction.PermissionResult)?.granted
        val snapshot = snapshot(rationale, grantedOverride)
        return state.copy(
            hasPermission = snapshot.hasPermission,
            hasRequestedBefore = snapshot.hasRequestedBefore,
            shouldShowRationale = snapshot.shouldShowRationale,
            lastCustomPromptAt = snapshot.lastCustomPromptAt,
        )
    }

    private fun initialState(): NotificationPermissionUiState {
        val snapshot = snapshot(shouldShowRationale = false)
        val shouldPauseSplash =
            runtimePermissionRequired &&
                !snapshot.hasPermission &&
                !snapshot.hasRequestedBefore
        return NotificationPermissionUiState(
            hasPermission = snapshot.hasPermission,
            hasRequestedBefore = snapshot.hasRequestedBefore,
            shouldShowRationale = snapshot.shouldShowRationale,
            lastCustomPromptAt = snapshot.lastCustomPromptAt,
            splashPaused = shouldPauseSplash,
        )
    }

    private fun snapshot(
        shouldShowRationale: Boolean,
        grantedOverride: Boolean? = null,
    ): NotificationPermissionSnapshot =
        NotificationPermissionSnapshot(
            hasPermission = grantedOverride ?: AppConfig.hasPostNotificationsPermission(application),
            hasRequestedBefore = permissionPreferences.hasRequestedNotificationRuntimePermissionBefore(),
            shouldShowRationale = shouldShowRationale,
            lastCustomPromptAt = permissionPreferences.readLastNotificationPermissionCustomPromptAt(),
        )

    private fun currentNoticeFlag(): Int =
        when {
            machineState.isSplashVisible -> 1
            AnalyticsTracker.hasCompletedCleanup() -> 3
            else -> 2
        }
}
