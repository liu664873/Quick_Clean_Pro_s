package com.quickcleanpro.phonecleaner.feature.startup

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.app.runtime.startup.NotificationLaunchSource

internal data class SplashMachineState(
    val stage: SplashStage = SplashStage.Preparing,
    val sdkBarrierFinished: Boolean = false,
    val visualReady: Boolean = false,
    val launchRequest: AppLaunchRequest? = null,
    val pauseReasons: Set<SplashPauseReason> = emptySet(),
)

internal data class SplashTransition(
    val state: SplashMachineState,
    val effects: List<SplashEffect> = emptyList(),
)

internal fun reduceSplashState(
    state: SplashMachineState,
    action: SplashAction,
    normalDestination: AppDestination,
): SplashTransition {
    if (state.stage == SplashStage.Completed) return SplashTransition(state)

    return when (action) {
        is SplashAction.LaunchRequestChanged -> {
            val request = action.request
            if (request is AppLaunchRequest.NotificationTarget &&
                request.source == NotificationLaunchSource.NewIntent
            ) {
                SplashTransition(
                    state = state.copy(stage = SplashStage.Completed, launchRequest = request),
                    effects = listOf(SplashEffect.OpenNotificationTarget(request.route)),
                )
            } else {
                advancePreparing(state.copy(launchRequest = request))
            }
        }
        SplashAction.SdkBarrierFinished ->
            advancePreparing(state.copy(sdkBarrierFinished = true))
        SplashAction.VisualReady ->
            advancePreparing(state.copy(visualReady = true))
        SplashAction.VisualFinished -> finishVisual(state, normalDestination)
        is SplashAction.PermissionPauseChanged ->
            SplashTransition(state.withPauseReason(SplashPauseReason.Permission, action.active))
        is SplashAction.OpenAdStateChanged ->
            SplashTransition(state.withPauseReason(SplashPauseReason.OpenAd, action.active))
        is SplashAction.ExternalLinkStateChanged ->
            SplashTransition(state.withPauseReason(SplashPauseReason.ExternalLink, action.active))
        SplashAction.OpenAdFinished -> {
            val resumed = state.withPauseReason(SplashPauseReason.OpenAd, active = false)
            SplashTransition(
                if (resumed.stage == SplashStage.WaitingForOpenAd) {
                    resumed.copy(stage = SplashStage.Finishing)
                } else {
                    resumed
                },
            )
        }
    }
}

private fun finishVisual(
    state: SplashMachineState,
    normalDestination: AppDestination,
): SplashTransition {
    if (state.stage != SplashStage.Finishing) return SplashTransition(state)
    val effect =
        when (val request = state.launchRequest) {
            is AppLaunchRequest.NotificationTarget -> SplashEffect.OpenNotificationTarget(request.route)
            AppLaunchRequest.Normal -> SplashEffect.Navigate(normalDestination)
            null -> return SplashTransition(state)
        }
    return SplashTransition(
        state = state.copy(stage = SplashStage.Completed),
        effects = listOf(effect),
    )
}

private fun advancePreparing(state: SplashMachineState): SplashTransition {
    if (state.stage != SplashStage.Preparing ||
        !state.sdkBarrierFinished ||
        !state.visualReady
    ) {
        return SplashTransition(state)
    }
    return when (state.launchRequest) {
        AppLaunchRequest.Normal ->
            SplashTransition(
                state = state.copy(stage = SplashStage.WaitingForOpenAd),
                effects = listOf(SplashEffect.RunColdStartAd),
            )
        is AppLaunchRequest.NotificationTarget ->
            SplashTransition(state.copy(stage = SplashStage.Finishing))
        null -> SplashTransition(state)
    }
}

private fun SplashMachineState.withPauseReason(
    reason: SplashPauseReason,
    active: Boolean,
): SplashMachineState =
    copy(
        pauseReasons =
            if (active) {
                pauseReasons + reason
            } else {
                pauseReasons - reason
            },
    )

internal fun SplashMachineState.toUiState(): SplashUiState =
    SplashUiState(
        stage = stage,
        paused = pauseReasons.isNotEmpty(),
    )
