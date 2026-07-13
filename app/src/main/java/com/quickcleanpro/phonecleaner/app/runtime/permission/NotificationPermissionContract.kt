package com.quickcleanpro.phonecleaner.app.runtime.permission

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
