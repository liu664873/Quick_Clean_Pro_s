package com.quickcleanpro.phonecleaner.app.runtime.permission

import java.util.Calendar

enum class NotificationPermissionRequestSource {
    Splash,
    HomeSystem,
    HomeCustom,
}

data class NotificationPermissionSnapshot(
    val hasPermission: Boolean,
    val hasRequestedBefore: Boolean,
    val shouldShowRationale: Boolean,
    val lastCustomPromptAt: Long,
)

data class NotificationPermissionUiState(
    val isSplashVisible: Boolean = true,
    val isHomeVisible: Boolean = false,
    val hasPermission: Boolean = false,
    val hasRequestedBefore: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val lastCustomPromptAt: Long = 0L,
    val customDialogVisible: Boolean = false,
    val requestSource: NotificationPermissionRequestSource? = null,
    val settingsLaunchPending: Boolean = false,
    val suppressHomePromptUntilMillis: Long = 0L,
    val splashPaused: Boolean = false,
    val permissionUiActive: Boolean = false,
    val homeCustomPromptDeferred: Boolean = false,
) {
    val permissionFlowActive: Boolean
        get() = splashPaused || permissionUiActive
}

sealed interface NotificationPermissionAction {
    data class VisibilityChanged(
        val isSplashVisible: Boolean,
        val isHomeVisible: Boolean,
        val shouldShowRationale: Boolean,
    ) : NotificationPermissionAction

    data class Refresh(
        val returningFromSettings: Boolean,
        val shouldShowRationale: Boolean,
    ) : NotificationPermissionAction

    data class PermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean,
    ) : NotificationPermissionAction

    data object HomePromptDelayElapsed : NotificationPermissionAction
    data object HomePromptCooldownElapsed : NotificationPermissionAction
    data object CustomPromptConfirmed : NotificationPermissionAction
    data object CustomPromptDismissed : NotificationPermissionAction
    data class SettingsLaunchResult(val launched: Boolean) : NotificationPermissionAction
}

sealed interface NotificationPermissionEffect {
    data class RequestSystemPermission(
        val source: NotificationPermissionRequestSource,
    ) : NotificationPermissionEffect

    data object OpenAppSettings : NotificationPermissionEffect
    data object NotifyPermissionGranted : NotificationPermissionEffect
}

internal sealed interface NotificationPermissionSideEffect {
    data object SaveRequestedBefore : NotificationPermissionSideEffect

    data class SaveLastCustomPromptAt(val timestampMillis: Long) : NotificationPermissionSideEffect

    data class TrackPopup(val accepted: Boolean) : NotificationPermissionSideEffect

    data class TrackPermissionResult(val granted: Boolean) : NotificationPermissionSideEffect

    data class Host(val effect: NotificationPermissionEffect) : NotificationPermissionSideEffect
}

internal data class NotificationPermissionTransition(
    val state: NotificationPermissionUiState,
    val effects: List<NotificationPermissionSideEffect> = emptyList(),
)

internal fun reduceNotificationPermissionState(
    state: NotificationPermissionUiState,
    action: NotificationPermissionAction,
    nowMillis: Long,
): NotificationPermissionTransition =
    when (action) {
        is NotificationPermissionAction.VisibilityChanged ->
            onVisibilityChanged(state, action)
        is NotificationPermissionAction.Refresh ->
            refreshPermission(state, action.returningFromSettings, nowMillis)
        is NotificationPermissionAction.PermissionResult ->
            onPermissionResult(state, action.granted)
        NotificationPermissionAction.HomePromptDelayElapsed,
        NotificationPermissionAction.HomePromptCooldownElapsed,
        -> refreshPermission(state, returningFromSettings = false, nowMillis = nowMillis)
        NotificationPermissionAction.CustomPromptConfirmed ->
            NotificationPermissionTransition(
                state =
                    state.copy(
                        customDialogVisible = false,
                        requestSource = NotificationPermissionRequestSource.HomeCustom,
                        permissionUiActive = true,
                    ),
                effects =
                    listOf(
                        NotificationPermissionSideEffect.TrackPopup(accepted = true),
                        NotificationPermissionSideEffect.Host(NotificationPermissionEffect.OpenAppSettings),
                    ),
            )
        NotificationPermissionAction.CustomPromptDismissed ->
            NotificationPermissionTransition(
                state = state.copy(customDialogVisible = false, permissionUiActive = false),
                effects = listOf(NotificationPermissionSideEffect.TrackPopup(accepted = false)),
            )
        is NotificationPermissionAction.SettingsLaunchResult ->
            NotificationPermissionTransition(
                state =
                    if (action.launched) {
                        state.copy(settingsLaunchPending = true)
                    } else {
                        state.copy(
                            requestSource = null,
                            settingsLaunchPending = false,
                            permissionUiActive = false,
                        )
                    },
            )
    }

private fun onVisibilityChanged(
    state: NotificationPermissionUiState,
    action: NotificationPermissionAction.VisibilityChanged,
): NotificationPermissionTransition {
    var updated =
        state.copy(
            isSplashVisible = action.isSplashVisible,
            isHomeVisible = action.isHomeVisible,
            shouldShowRationale = action.shouldShowRationale,
        )
    if (!updated.isHomeVisible) {
        updated =
            updated.copy(
                customDialogVisible = false,
                suppressHomePromptUntilMillis = 0L,
                permissionUiActive = false,
            )
    }
    if (!updated.isSplashVisible || updated.hasPermission) {
        updated = updated.copy(splashPaused = false)
    }
    if (updated.isSplashVisible &&
        !updated.hasPermission &&
        !updated.hasRequestedBefore &&
        updated.requestSource == null
    ) {
        return NotificationPermissionTransition(
            state =
                updated.copy(
                    hasRequestedBefore = true,
                    requestSource = NotificationPermissionRequestSource.Splash,
                    splashPaused = true,
                    permissionUiActive = true,
                ),
            effects =
                listOf(
                    NotificationPermissionSideEffect.SaveRequestedBefore,
                    NotificationPermissionSideEffect.Host(
                        NotificationPermissionEffect.RequestSystemPermission(
                            NotificationPermissionRequestSource.Splash,
                        ),
                    ),
                ),
        )
    }
    return NotificationPermissionTransition(updated)
}

private fun refreshPermission(
    state: NotificationPermissionUiState,
    returningFromSettings: Boolean,
    nowMillis: Long,
): NotificationPermissionTransition {
    val resultEffects =
        if (returningFromSettings) {
            listOf(NotificationPermissionSideEffect.TrackPermissionResult(state.hasPermission))
        } else {
            emptyList()
        }
    if (state.hasPermission) {
        return NotificationPermissionTransition(
            state =
                state.copy(
                    customDialogVisible = false,
                    requestSource = null,
                    settingsLaunchPending = false,
                    splashPaused = false,
                    permissionUiActive = false,
                ),
            effects =
                resultEffects +
                    NotificationPermissionSideEffect.Host(NotificationPermissionEffect.NotifyPermissionGranted),
        )
    }

    val base =
        state.copy(
            requestSource = if (returningFromSettings) null else state.requestSource,
            settingsLaunchPending = if (returningFromSettings) false else state.settingsLaunchPending,
            permissionUiActive = if (returningFromSettings) false else state.permissionUiActive,
        )
    if (!base.isHomeVisible || returningFromSettings || base.requestSource != null) {
        return NotificationPermissionTransition(
            state = base.copy(customDialogVisible = false, permissionUiActive = false),
            effects = resultEffects,
        )
    }
    if (nowMillis < base.suppressHomePromptUntilMillis || !base.hasRequestedBefore) {
        return NotificationPermissionTransition(
            state = base.copy(customDialogVisible = false, permissionUiActive = false),
            effects = resultEffects,
        )
    }
    if (base.shouldShowRationale) {
        return NotificationPermissionTransition(
            state =
                base.copy(
                    customDialogVisible = false,
                    hasRequestedBefore = true,
                    requestSource = NotificationPermissionRequestSource.HomeSystem,
                    suppressHomePromptUntilMillis = nowMillis + HOME_SYSTEM_REQUEST_COOLDOWN_MILLIS,
                    permissionUiActive = true,
                ),
            effects =
                resultEffects +
                    listOf(
                        NotificationPermissionSideEffect.SaveRequestedBefore,
                        NotificationPermissionSideEffect.Host(
                            NotificationPermissionEffect.RequestSystemPermission(
                                NotificationPermissionRequestSource.HomeSystem,
                            ),
                        ),
                    ),
        )
    }
    if (!base.homeCustomPromptDeferred &&
        canShowNotificationPermissionCustomPrompt(base.lastCustomPromptAt, nowMillis)
    ) {
        return NotificationPermissionTransition(
            state =
                base.copy(
                    lastCustomPromptAt = nowMillis,
                    customDialogVisible = true,
                    permissionUiActive = true,
                ),
            effects =
                resultEffects + NotificationPermissionSideEffect.SaveLastCustomPromptAt(nowMillis),
        )
    }
    return NotificationPermissionTransition(
        state = base.copy(customDialogVisible = false, permissionUiActive = false),
        effects = resultEffects,
    )
}

private fun onPermissionResult(
    state: NotificationPermissionUiState,
    granted: Boolean,
): NotificationPermissionTransition {
    val source = state.requestSource
    val commonEffects =
        listOf(
            NotificationPermissionSideEffect.TrackPopup(accepted = granted),
            NotificationPermissionSideEffect.TrackPermissionResult(granted),
        )
    if (granted) {
        return NotificationPermissionTransition(
            state =
                state.copy(
                    hasPermission = true,
                    customDialogVisible = false,
                    requestSource = null,
                    settingsLaunchPending = false,
                    splashPaused = false,
                    permissionUiActive = false,
                ),
            effects =
                commonEffects +
                    NotificationPermissionSideEffect.Host(NotificationPermissionEffect.NotifyPermissionGranted),
        )
    }
    return NotificationPermissionTransition(
        state =
            state.copy(
                hasPermission = false,
                hasRequestedBefore = true,
                customDialogVisible = false,
                requestSource = null,
                settingsLaunchPending = false,
                suppressHomePromptUntilMillis = 0L,
                splashPaused = if (source == NotificationPermissionRequestSource.Splash) false else state.splashPaused,
                permissionUiActive = false,
                homeCustomPromptDeferred =
                    state.homeCustomPromptDeferred ||
                        source == NotificationPermissionRequestSource.HomeSystem,
            ),
        effects = commonEffects,
    )
}

internal fun canShowNotificationPermissionCustomPrompt(
    lastPromptAt: Long,
    nowMillis: Long,
): Boolean {
    if (lastPromptAt <= 0L || nowMillis <= 0L) return true
    val first = Calendar.getInstance().apply { timeInMillis = lastPromptAt }
    val second = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return first.get(Calendar.ERA) != second.get(Calendar.ERA) ||
        first.get(Calendar.YEAR) != second.get(Calendar.YEAR) ||
        first.get(Calendar.DAY_OF_YEAR) != second.get(Calendar.DAY_OF_YEAR)
}

internal const val HOME_NOTIFICATION_PERMISSION_PROMPT_DELAY_MILLIS = 350L
internal const val HOME_SYSTEM_REQUEST_COOLDOWN_MILLIS = 5_000L
