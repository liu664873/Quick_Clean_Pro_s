package com.quickcleanpro.phonecleaner.app.runtime.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionStateMachineTest {
    @Test
    fun `initial state awaits first request only on Android 13 and above`() {
        val facts = facts(hasRequestedBefore = false)

        assertEquals(
            NotificationPermissionPhase.AwaitingInitialRequest,
            initialNotificationPermissionState(true, facts).phase,
        )
        assertEquals(
            NotificationPermissionPhase.Idle,
            initialNotificationPermissionState(false, facts).phase,
        )
    }

    @Test
    fun `first splash visit pauses and requests system permission once`() {
        val first =
            reduceNotificationPermissionState(
                state =
                    state(
                        facts = facts(hasRequestedBefore = false),
                        phase = NotificationPermissionPhase.AwaitingInitialRequest,
                    ),
                action = surface(NotificationPermissionSurface.Splash, hasRequestedBefore = false),
                nowMillis = NOW,
            )

        assertTrue(first.state.splashPaused)
        assertTrue(first.state.permissionUiActive)
        assertTrue(first.state.facts.hasRequestedBefore)
        assertEquals(
            NotificationPermissionPhase.RequestingSystem(NotificationPermissionRequestSource.Splash),
            first.state.phase,
        )
        assertEquals(
            listOf(
                NotificationPermissionCommand.SaveRequestedBefore,
                NotificationPermissionCommand.RequestSystemPermission(
                    NotificationPermissionRequestSource.Splash,
                ),
            ),
            first.commands,
        )

        val repeated =
            reduceNotificationPermissionState(
                state = first.state,
                action = surface(NotificationPermissionSurface.Splash),
                nowMillis = NOW,
            )

        assertTrue(repeated.commands.isEmpty())
    }

    @Test
    fun `home does not request permission before splash has requested it`() {
        val transition =
            reduceNotificationPermissionState(
                state = state(surface = NotificationPermissionSurface.Home, facts = facts(hasRequestedBefore = false)),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertFalse(transition.state.permissionUiActive)
        assertTrue(transition.commands.isEmpty())
    }

    @Test
    fun `home rationale requests system permission`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    state(
                        surface = NotificationPermissionSurface.Home,
                        facts = facts(shouldShowRationale = true),
                    ),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertEquals(
            NotificationPermissionPhase.RequestingSystem(NotificationPermissionRequestSource.HomeSystem),
            transition.state.phase,
        )
        assertTrue(transition.state.permissionUiActive)
        assertEquals(
            NotificationPermissionCommand.RequestSystemPermission(
                NotificationPermissionRequestSource.HomeSystem,
            ),
            transition.commands.last(),
        )
    }

    @Test
    fun `permanent denial shows custom prompt once per local day`() {
        val first =
            reduceNotificationPermissionState(
                state = state(surface = NotificationPermissionSurface.Home),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertTrue(first.state.customDialogVisible)
        assertTrue(first.state.permissionUiActive)
        assertEquals(
            listOf(NotificationPermissionCommand.SaveLastCustomPromptAt(NOW)),
            first.commands,
        )

        val sameDay =
            reduceNotificationPermissionState(
                state =
                    state(
                        surface = NotificationPermissionSurface.Home,
                        facts = facts(lastCustomPromptAt = NOW),
                    ),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW + 1_000L,
            )

        assertFalse(sameDay.state.customDialogVisible)
        assertTrue(sameDay.commands.isEmpty())
    }

    @Test
    fun `session deferral suppresses home custom prompt`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    state(
                        surface = NotificationPermissionSurface.Home,
                        customPromptDeferredForSession = true,
                    ),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertFalse(transition.state.customDialogVisible)
        assertFalse(transition.state.permissionUiActive)
        assertTrue(transition.commands.isEmpty())
    }

    @Test
    fun `home system denial defers custom prompt for the session`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    state(
                        surface = NotificationPermissionSurface.Home,
                        phase =
                            NotificationPermissionPhase.RequestingSystem(
                                NotificationPermissionRequestSource.HomeSystem,
                            ),
                    ),
                action =
                    NotificationPermissionAction.SystemPermissionResult(
                        facts = facts(shouldShowRationale = false),
                    ),
                nowMillis = NOW,
            )

        assertTrue(transition.state.customPromptDeferredForSession)
        assertFalse(transition.state.permissionUiActive)
        assertEquals(NotificationPermissionPhase.Idle, transition.state.phase)
        assertEquals(permissionResultCommands(granted = false), transition.commands)
    }

    @Test
    fun `splash denial releases splash pause`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    state(
                        phase =
                            NotificationPermissionPhase.RequestingSystem(
                                NotificationPermissionRequestSource.Splash,
                            ),
                    ),
                action =
                    NotificationPermissionAction.SystemPermissionResult(
                        facts = facts(shouldShowRationale = false),
                    ),
                nowMillis = NOW,
            )

        assertFalse(transition.state.splashPaused)
        assertFalse(transition.state.permissionFlowActive)
    }

    @Test
    fun `permission grant clears active state and notifies host`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    state(
                        phase =
                            NotificationPermissionPhase.RequestingSystem(
                                NotificationPermissionRequestSource.Splash,
                            ),
                    ),
                action =
                    NotificationPermissionAction.SystemPermissionResult(
                        facts = facts(hasPermission = true),
                    ),
                nowMillis = NOW,
            )

        assertTrue(transition.state.hasPermission)
        assertFalse(transition.state.permissionFlowActive)
        assertEquals(
            permissionResultCommands(granted = true),
            transition.commands,
        )
    }

    @Test
    fun `custom prompt commands settings and recovers when launch fails`() {
        val confirmed =
            reduceNotificationPermissionState(
                state =
                    state(
                        surface = NotificationPermissionSurface.Home,
                        phase = NotificationPermissionPhase.ShowingCustomPrompt,
                    ),
                action = NotificationPermissionAction.CustomPromptConfirmed,
                nowMillis = NOW,
            )

        assertEquals(NotificationPermissionPhase.LaunchingSettings, confirmed.state.phase)
        assertTrue(confirmed.state.permissionUiActive)
        assertEquals(
            listOf(
                NotificationPermissionCommand.TrackPopup(accepted = true),
                NotificationPermissionCommand.OpenAppSettings,
            ),
            confirmed.commands,
        )

        val launchFailed =
            reduceNotificationPermissionState(
                state = confirmed.state,
                action = NotificationPermissionAction.SettingsLaunchResult(launched = false),
                nowMillis = NOW,
            )

        assertEquals(NotificationPermissionPhase.Idle, launchFailed.state.phase)
        assertFalse(launchFailed.state.permissionUiActive)
    }

    @Test
    fun `settings return tracks result and notifies when granted`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    state(
                        surface = NotificationPermissionSurface.Home,
                        facts = facts(hasPermission = true),
                        phase = NotificationPermissionPhase.WaitingForSettingsReturn,
                    ),
                action =
                    NotificationPermissionAction.AppResumed(
                        facts = facts(hasPermission = true),
                    ),
                nowMillis = NOW,
            )

        assertFalse(transition.state.permissionUiActive)
        assertEquals(NotificationPermissionPhase.Idle, transition.state.phase)
        assertEquals(
            listOf(
                NotificationPermissionCommand.TrackPermissionResult(granted = true),
                NotificationPermissionCommand.NotifyPermissionGranted,
            ),
            transition.commands,
        )
    }

    @Test
    fun `leaving home closes custom prompt`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    state(
                        surface = NotificationPermissionSurface.Home,
                        phase = NotificationPermissionPhase.ShowingCustomPrompt,
                    ),
                action = surface(NotificationPermissionSurface.Other),
                nowMillis = NOW,
            )

        assertEquals(NotificationPermissionPhase.Idle, transition.state.phase)
        assertFalse(transition.state.customDialogVisible)
    }

    @Test
    fun `permission flow is not active on other surfaces`() {
        val state =
            state(
                surface = NotificationPermissionSurface.Other,
                phase =
                    NotificationPermissionPhase.RequestingSystem(
                        NotificationPermissionRequestSource.HomeSystem,
                    ),
            )

        assertFalse(state.permissionUiActive)
        assertFalse(state.permissionFlowActive)
    }

    private fun surface(
        surface: NotificationPermissionSurface,
        hasRequestedBefore: Boolean = true,
    ): NotificationPermissionAction.SurfaceChanged =
        NotificationPermissionAction.SurfaceChanged(
            surface = surface,
            facts = facts(hasRequestedBefore = hasRequestedBefore),
        )

    private fun facts(
        hasPermission: Boolean = false,
        hasRequestedBefore: Boolean = true,
        shouldShowRationale: Boolean = false,
        lastCustomPromptAt: Long = 0L,
    ): NotificationPermissionFacts =
        NotificationPermissionFacts(
            hasPermission = hasPermission,
            hasRequestedBefore = hasRequestedBefore,
            shouldShowRationale = shouldShowRationale,
            lastCustomPromptAt = lastCustomPromptAt,
        )

    private fun state(
        surface: NotificationPermissionSurface = NotificationPermissionSurface.Splash,
        facts: NotificationPermissionFacts = facts(),
        phase: NotificationPermissionPhase = NotificationPermissionPhase.Idle,
        customPromptDeferredForSession: Boolean = false,
    ): NotificationPermissionUiState =
        NotificationPermissionUiState(
            facts = facts,
            surface = surface,
            phase = phase,
            customPromptDeferredForSession = customPromptDeferredForSession,
        )

    private fun permissionResultCommands(granted: Boolean): List<NotificationPermissionCommand> =
        buildList {
            add(NotificationPermissionCommand.TrackPopup(accepted = granted))
            add(NotificationPermissionCommand.TrackPermissionResult(granted = granted))
            if (granted) add(NotificationPermissionCommand.NotifyPermissionGranted)
        }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
