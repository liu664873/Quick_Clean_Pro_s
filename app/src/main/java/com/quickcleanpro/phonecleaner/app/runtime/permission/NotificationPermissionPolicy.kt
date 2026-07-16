package com.quickcleanpro.phonecleaner.app.runtime.permission

import java.util.Calendar

enum class NotificationPermissionRequestSource {
    Splash,
    HomeSystem,
}

data class NotificationPermissionFacts(
    val hasPermission: Boolean,
    val hasRequestedBefore: Boolean,
    val shouldShowRationale: Boolean,
    val lastCustomPromptAt: Long,
)

enum class NotificationPermissionSurface {
    Splash,
    Home,
    Other,
}

sealed interface NotificationPermissionPhase {
    data object Idle : NotificationPermissionPhase
    data object AwaitingInitialRequest : NotificationPermissionPhase

    data class RequestingSystem(
        val source: NotificationPermissionRequestSource,
    ) : NotificationPermissionPhase

    data object ShowingCustomPrompt : NotificationPermissionPhase
    data object LaunchingSettings : NotificationPermissionPhase
    data object WaitingForSettingsReturn : NotificationPermissionPhase
}

data class NotificationPermissionUiState(
    val facts: NotificationPermissionFacts,
    val surface: NotificationPermissionSurface = NotificationPermissionSurface.Splash,
    val phase: NotificationPermissionPhase = NotificationPermissionPhase.Idle,
    val customPromptDeferredForSession: Boolean = false,
) {
    val hasPermission: Boolean
        get() = facts.hasPermission

    val splashPaused: Boolean
        get() =
            surface == NotificationPermissionSurface.Splash &&
                (phase == NotificationPermissionPhase.AwaitingInitialRequest ||
                    phase ==
                    NotificationPermissionPhase.RequestingSystem(
                        NotificationPermissionRequestSource.Splash,
                    ))

    val permissionUiActive: Boolean
        get() =
            when (phase) {
                NotificationPermissionPhase.Idle,
                NotificationPermissionPhase.AwaitingInitialRequest,
                -> false
                is NotificationPermissionPhase.RequestingSystem ->
                    when (phase.source) {
                        NotificationPermissionRequestSource.Splash ->
                            surface == NotificationPermissionSurface.Splash
                        NotificationPermissionRequestSource.HomeSystem ->
                            surface == NotificationPermissionSurface.Home
                    }
                NotificationPermissionPhase.ShowingCustomPrompt ->
                    surface == NotificationPermissionSurface.Home
                NotificationPermissionPhase.LaunchingSettings,
                NotificationPermissionPhase.WaitingForSettingsReturn,
                -> surface == NotificationPermissionSurface.Home
            }

    val permissionFlowActive: Boolean
        get() = splashPaused || permissionUiActive

    val customDialogVisible: Boolean
        get() = phase == NotificationPermissionPhase.ShowingCustomPrompt
}

internal sealed interface NotificationPermissionAction {
    data class SurfaceChanged(
        val surface: NotificationPermissionSurface,
        val facts: NotificationPermissionFacts,
    ) : NotificationPermissionAction

    data object HomePromptDelayElapsed : NotificationPermissionAction

    data class AppResumed(
        val facts: NotificationPermissionFacts,
    ) : NotificationPermissionAction

    data class SystemPermissionResult(
        val facts: NotificationPermissionFacts,
    ) : NotificationPermissionAction

    data object CustomPromptConfirmed : NotificationPermissionAction
    data object CustomPromptDismissed : NotificationPermissionAction
    data class SettingsLaunchResult(val launched: Boolean) : NotificationPermissionAction
}

internal sealed interface NotificationPermissionCommand {
    sealed interface Host : NotificationPermissionCommand

    data class RequestSystemPermission(
        val source: NotificationPermissionRequestSource,
    ) : Host

    data object OpenAppSettings : Host
    data object NotifyPermissionGranted : Host
    data object SaveRequestedBefore : NotificationPermissionCommand
    data class SaveLastCustomPromptAt(val timestampMillis: Long) : NotificationPermissionCommand
    data class TrackPopup(val accepted: Boolean) : NotificationPermissionCommand
    data class TrackPermissionResult(val granted: Boolean) : NotificationPermissionCommand
}

internal data class NotificationPermissionTransition(
    val state: NotificationPermissionUiState,
    val commands: List<NotificationPermissionCommand> = emptyList(),
)

internal fun initialNotificationPermissionState(
    runtimePermissionRequired: Boolean,
    facts: NotificationPermissionFacts,
): NotificationPermissionUiState =
    NotificationPermissionUiState(
        facts = facts,
        phase =
            if (runtimePermissionRequired && !facts.hasPermission && !facts.hasRequestedBefore) {
                NotificationPermissionPhase.AwaitingInitialRequest
            } else {
                NotificationPermissionPhase.Idle
            },
    )

internal fun reduceNotificationPermissionState(
    state: NotificationPermissionUiState,
    action: NotificationPermissionAction,
    nowMillis: Long,
): NotificationPermissionTransition =
    when (action) {
        is NotificationPermissionAction.SurfaceChanged -> onSurfaceChanged(state, action)
        NotificationPermissionAction.HomePromptDelayElapsed -> evaluateHomePrompt(state, nowMillis)
        is NotificationPermissionAction.AppResumed -> onAppResumed(state, action, nowMillis)
        is NotificationPermissionAction.SystemPermissionResult -> onSystemPermissionResult(state, action)
        NotificationPermissionAction.CustomPromptConfirmed -> onCustomPromptConfirmed(state)
        NotificationPermissionAction.CustomPromptDismissed -> onCustomPromptDismissed(state)
        is NotificationPermissionAction.SettingsLaunchResult -> onSettingsLaunchResult(state, action)
    }

private fun onSurfaceChanged(
    state: NotificationPermissionUiState,
    action: NotificationPermissionAction.SurfaceChanged,
): NotificationPermissionTransition {
    var updated = state.copy(surface = action.surface, facts = action.facts)
    if (updated.facts.hasPermission) {
        return NotificationPermissionTransition(updated.copy(phase = NotificationPermissionPhase.Idle))
    }
    if (action.surface != NotificationPermissionSurface.Home &&
        updated.phase == NotificationPermissionPhase.ShowingCustomPrompt
    ) {
        updated = updated.copy(phase = NotificationPermissionPhase.Idle)
    }
    if (action.surface != NotificationPermissionSurface.Splash &&
        updated.phase == NotificationPermissionPhase.AwaitingInitialRequest
    ) {
        updated = updated.copy(phase = NotificationPermissionPhase.Idle)
    }
    if (action.surface == NotificationPermissionSurface.Splash &&
        updated.phase == NotificationPermissionPhase.AwaitingInitialRequest
    ) {
        return NotificationPermissionTransition(
            state =
                updated.copy(
                    facts = updated.facts.copy(hasRequestedBefore = true),
                    phase =
                        NotificationPermissionPhase.RequestingSystem(
                            NotificationPermissionRequestSource.Splash,
                        ),
                ),
            commands =
                listOf(
                    NotificationPermissionCommand.SaveRequestedBefore,
                    NotificationPermissionCommand.RequestSystemPermission(
                        NotificationPermissionRequestSource.Splash,
                    ),
                ),
        )
    }
    return NotificationPermissionTransition(updated)
}

private fun onAppResumed(
    state: NotificationPermissionUiState,
    action: NotificationPermissionAction.AppResumed,
    nowMillis: Long,
): NotificationPermissionTransition {
    val updated = state.copy(facts = action.facts)
    if (state.phase == NotificationPermissionPhase.WaitingForSettingsReturn) {
        return NotificationPermissionTransition(
            state = updated.copy(phase = NotificationPermissionPhase.Idle),
            commands =
                buildList {
                    add(NotificationPermissionCommand.TrackPermissionResult(updated.hasPermission))
                    if (updated.hasPermission) {
                        add(NotificationPermissionCommand.NotifyPermissionGranted)
                    }
                },
        )
    }
    if (updated.hasPermission) {
        return NotificationPermissionTransition(
            state = updated.copy(phase = NotificationPermissionPhase.Idle),
            commands = listOf(NotificationPermissionCommand.NotifyPermissionGranted),
        )
    }
    return evaluateHomePrompt(updated, nowMillis)
}

private fun evaluateHomePrompt(
    state: NotificationPermissionUiState,
    nowMillis: Long,
): NotificationPermissionTransition {
    if (state.surface != NotificationPermissionSurface.Home ||
        state.phase != NotificationPermissionPhase.Idle ||
        state.hasPermission ||
        !state.facts.hasRequestedBefore
    ) {
        return NotificationPermissionTransition(state)
    }
    if (state.facts.shouldShowRationale) {
        return NotificationPermissionTransition(
            state =
                state.copy(
                    facts = state.facts.copy(hasRequestedBefore = true),
                    phase =
                        NotificationPermissionPhase.RequestingSystem(
                            NotificationPermissionRequestSource.HomeSystem,
                        ),
                ),
            commands =
                listOf(
                    NotificationPermissionCommand.SaveRequestedBefore,
                    NotificationPermissionCommand.RequestSystemPermission(
                        NotificationPermissionRequestSource.HomeSystem,
                    ),
                ),
        )
    }
    if (!state.customPromptDeferredForSession &&
        canShowNotificationPermissionCustomPrompt(state.facts.lastCustomPromptAt, nowMillis)
    ) {
        return NotificationPermissionTransition(
            state =
                state.copy(
                    facts = state.facts.copy(lastCustomPromptAt = nowMillis),
                    phase = NotificationPermissionPhase.ShowingCustomPrompt,
                ),
            commands = listOf(NotificationPermissionCommand.SaveLastCustomPromptAt(nowMillis)),
        )
    }
    return NotificationPermissionTransition(state)
}

private fun onSystemPermissionResult(
    state: NotificationPermissionUiState,
    action: NotificationPermissionAction.SystemPermissionResult,
): NotificationPermissionTransition {
    val source = (state.phase as? NotificationPermissionPhase.RequestingSystem)?.source
    val facts = action.facts.copy(hasRequestedBefore = true)
    val commands =
        buildList {
            add(NotificationPermissionCommand.TrackPopup(accepted = facts.hasPermission))
            add(NotificationPermissionCommand.TrackPermissionResult(granted = facts.hasPermission))
            if (facts.hasPermission) {
                add(NotificationPermissionCommand.NotifyPermissionGranted)
            }
        }
    return NotificationPermissionTransition(
        state =
            state.copy(
                facts = facts,
                phase = NotificationPermissionPhase.Idle,
                customPromptDeferredForSession =
                    state.customPromptDeferredForSession ||
                        source == NotificationPermissionRequestSource.HomeSystem,
            ),
        commands = commands,
    )
}

private fun onCustomPromptConfirmed(
    state: NotificationPermissionUiState,
): NotificationPermissionTransition =
    if (state.phase == NotificationPermissionPhase.ShowingCustomPrompt) {
        NotificationPermissionTransition(
            state = state.copy(phase = NotificationPermissionPhase.LaunchingSettings),
            commands =
                listOf(
                    NotificationPermissionCommand.TrackPopup(accepted = true),
                    NotificationPermissionCommand.OpenAppSettings,
                ),
        )
    } else {
        NotificationPermissionTransition(state)
    }

private fun onCustomPromptDismissed(
    state: NotificationPermissionUiState,
): NotificationPermissionTransition =
    if (state.phase == NotificationPermissionPhase.ShowingCustomPrompt) {
        NotificationPermissionTransition(
            state = state.copy(phase = NotificationPermissionPhase.Idle),
            commands = listOf(NotificationPermissionCommand.TrackPopup(accepted = false)),
        )
    } else {
        NotificationPermissionTransition(state)
    }

private fun onSettingsLaunchResult(
    state: NotificationPermissionUiState,
    action: NotificationPermissionAction.SettingsLaunchResult,
): NotificationPermissionTransition =
    if (state.phase == NotificationPermissionPhase.LaunchingSettings) {
        NotificationPermissionTransition(
            state =
                state.copy(
                    phase =
                        if (action.launched) {
                            NotificationPermissionPhase.WaitingForSettingsReturn
                        } else {
                            NotificationPermissionPhase.Idle
                        },
                ),
        )
    } else {
        NotificationPermissionTransition(state)
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
