package com.quickcleanpro.phonecleaner.feature.startup

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchRequest

enum class SplashStage {
    Preparing,
    WaitingForOpenAd,
    Finishing,
    Completed,
}

enum class SplashPauseReason {
    Permission,
    OpenAd,
    ExternalLink,
}

data class SplashUiState(
    val stage: SplashStage = SplashStage.Preparing,
    val paused: Boolean = false,
) {
    val finishRequested: Boolean
        get() = stage == SplashStage.Finishing
}

sealed interface SplashAction {
    data class LaunchRequestChanged(val request: AppLaunchRequest) : SplashAction

    data object SdkBarrierFinished : SplashAction

    data object VisualReady : SplashAction

    data object VisualFinished : SplashAction

    data class PermissionPauseChanged(val active: Boolean) : SplashAction

    data class OpenAdStateChanged(val active: Boolean) : SplashAction

    data class ExternalLinkStateChanged(val active: Boolean) : SplashAction

    data object OpenAdFinished : SplashAction
}

sealed interface SplashEffect {
    data object RunColdStartAd : SplashEffect

    data class OpenNotificationTarget(val route: String) : SplashEffect

    data class Navigate(val destination: AppDestination) : SplashEffect
}
