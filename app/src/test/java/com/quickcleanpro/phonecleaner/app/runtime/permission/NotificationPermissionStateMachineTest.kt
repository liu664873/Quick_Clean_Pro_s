package com.quickcleanpro.phonecleaner.app.runtime.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionStateMachineTest {
    @Test
    fun `first splash visit pauses and requests system permission once`() {
        val first =
            reduceNotificationPermissionState(
                state = NotificationPermissionUiState(splashPaused = true),
                action = visibility(splash = true),
                nowMillis = NOW,
            )

        assertTrue(first.state.splashPaused)
        assertTrue(first.state.permissionUiActive)
        assertTrue(first.state.hasRequestedBefore)
        assertEquals(NotificationPermissionRequestSource.Splash, first.state.requestSource)
        assertEquals(
            listOf(
                NotificationPermissionSideEffect.SaveRequestedBefore,
                NotificationPermissionSideEffect.Host(
                    NotificationPermissionEffect.RequestSystemPermission(
                        NotificationPermissionRequestSource.Splash,
                    ),
                ),
            ),
            first.effects,
        )

        val repeated =
            reduceNotificationPermissionState(
                state = first.state,
                action = visibility(splash = true),
                nowMillis = NOW,
            )

        assertTrue(repeated.effects.isEmpty())
    }

    @Test
    fun `home does not request permission before splash has requested it`() {
        val transition =
            reduceNotificationPermissionState(
                state = homeState(hasRequestedBefore = false),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertFalse(transition.state.permissionUiActive)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `home rationale requests system permission and starts cooldown`() {
        val transition =
            reduceNotificationPermissionState(
                state = homeState(shouldShowRationale = true),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertEquals(NotificationPermissionRequestSource.HomeSystem, transition.state.requestSource)
        assertEquals(NOW + HOME_SYSTEM_REQUEST_COOLDOWN_MILLIS, transition.state.suppressHomePromptUntilMillis)
        assertTrue(transition.state.permissionUiActive)
        assertEquals(
            NotificationPermissionSideEffect.Host(
                NotificationPermissionEffect.RequestSystemPermission(
                    NotificationPermissionRequestSource.HomeSystem,
                ),
            ),
            transition.effects.last(),
        )
    }

    @Test
    fun `permanent denial shows custom prompt once per local day`() {
        val first =
            reduceNotificationPermissionState(
                state = homeState(),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertTrue(first.state.customDialogVisible)
        assertTrue(first.state.permissionUiActive)
        assertEquals(
            listOf(NotificationPermissionSideEffect.SaveLastCustomPromptAt(NOW)),
            first.effects,
        )

        val sameDay =
            reduceNotificationPermissionState(
                state = homeState(lastCustomPromptAt = NOW),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW + 1_000L,
            )

        assertFalse(sameDay.state.customDialogVisible)
        assertTrue(sameDay.effects.isEmpty())
    }

    @Test
    fun `session deferral suppresses home custom prompt`() {
        val transition =
            reduceNotificationPermissionState(
                state = homeState(homeCustomPromptDeferred = true),
                action = NotificationPermissionAction.HomePromptDelayElapsed,
                nowMillis = NOW,
            )

        assertFalse(transition.state.customDialogVisible)
        assertFalse(transition.state.permissionUiActive)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `home system denial defers custom prompt for the session`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    homeState(
                        requestSource = NotificationPermissionRequestSource.HomeSystem,
                        permissionUiActive = true,
                    ),
                action =
                    NotificationPermissionAction.PermissionResult(
                        granted = false,
                        shouldShowRationale = false,
                    ),
                nowMillis = NOW,
            )

        assertTrue(transition.state.homeCustomPromptDeferred)
        assertFalse(transition.state.permissionUiActive)
        assertEquals(null, transition.state.requestSource)
        assertEquals(permissionResultEffects(granted = false), transition.effects)
    }

    @Test
    fun `splash denial releases splash pause`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    NotificationPermissionUiState(
                        requestSource = NotificationPermissionRequestSource.Splash,
                        splashPaused = true,
                        permissionUiActive = true,
                    ),
                action =
                    NotificationPermissionAction.PermissionResult(
                        granted = false,
                        shouldShowRationale = false,
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
                    NotificationPermissionUiState(
                        requestSource = NotificationPermissionRequestSource.Splash,
                        splashPaused = true,
                        permissionUiActive = true,
                    ),
                action =
                    NotificationPermissionAction.PermissionResult(
                        granted = true,
                        shouldShowRationale = false,
                    ),
                nowMillis = NOW,
            )

        assertTrue(transition.state.hasPermission)
        assertFalse(transition.state.permissionFlowActive)
        assertEquals(
            permissionResultEffects(granted = true) +
                NotificationPermissionSideEffect.Host(NotificationPermissionEffect.NotifyPermissionGranted),
            transition.effects,
        )
    }

    @Test
    fun `custom prompt commands settings and recovers when launch fails`() {
        val confirmed =
            reduceNotificationPermissionState(
                state = homeState(customDialogVisible = true, permissionUiActive = true),
                action = NotificationPermissionAction.CustomPromptConfirmed,
                nowMillis = NOW,
            )

        assertEquals(NotificationPermissionRequestSource.HomeCustom, confirmed.state.requestSource)
        assertTrue(confirmed.state.permissionUiActive)
        assertEquals(
            listOf(
                NotificationPermissionSideEffect.TrackPopup(accepted = true),
                NotificationPermissionSideEffect.Host(NotificationPermissionEffect.OpenAppSettings),
            ),
            confirmed.effects,
        )

        val launchFailed =
            reduceNotificationPermissionState(
                state = confirmed.state,
                action = NotificationPermissionAction.SettingsLaunchResult(launched = false),
                nowMillis = NOW,
            )

        assertEquals(null, launchFailed.state.requestSource)
        assertFalse(launchFailed.state.settingsLaunchPending)
        assertFalse(launchFailed.state.permissionUiActive)
    }

    @Test
    fun `settings return tracks result and notifies when granted`() {
        val transition =
            reduceNotificationPermissionState(
                state =
                    homeState(
                        hasPermission = true,
                        requestSource = NotificationPermissionRequestSource.HomeCustom,
                        settingsLaunchPending = true,
                        permissionUiActive = true,
                    ),
                action =
                    NotificationPermissionAction.Refresh(
                        returningFromSettings = true,
                        shouldShowRationale = false,
                    ),
                nowMillis = NOW,
            )

        assertFalse(transition.state.permissionUiActive)
        assertFalse(transition.state.settingsLaunchPending)
        assertEquals(
            listOf(
                NotificationPermissionSideEffect.TrackPermissionResult(granted = true),
                NotificationPermissionSideEffect.Host(NotificationPermissionEffect.NotifyPermissionGranted),
            ),
            transition.effects,
        )
    }

    private fun visibility(
        splash: Boolean = false,
        home: Boolean = false,
    ): NotificationPermissionAction.VisibilityChanged =
        NotificationPermissionAction.VisibilityChanged(
            isSplashVisible = splash,
            isHomeVisible = home,
            shouldShowRationale = false,
        )

    private fun homeState(
        hasPermission: Boolean = false,
        hasRequestedBefore: Boolean = true,
        shouldShowRationale: Boolean = false,
        lastCustomPromptAt: Long = 0L,
        customDialogVisible: Boolean = false,
        requestSource: NotificationPermissionRequestSource? = null,
        settingsLaunchPending: Boolean = false,
        permissionUiActive: Boolean = false,
        homeCustomPromptDeferred: Boolean = false,
    ): NotificationPermissionUiState =
        NotificationPermissionUiState(
            isSplashVisible = false,
            isHomeVisible = true,
            hasPermission = hasPermission,
            hasRequestedBefore = hasRequestedBefore,
            shouldShowRationale = shouldShowRationale,
            lastCustomPromptAt = lastCustomPromptAt,
            customDialogVisible = customDialogVisible,
            requestSource = requestSource,
            settingsLaunchPending = settingsLaunchPending,
            permissionUiActive = permissionUiActive,
            homeCustomPromptDeferred = homeCustomPromptDeferred,
        )

    private fun permissionResultEffects(granted: Boolean): List<NotificationPermissionSideEffect> =
        listOf(
            NotificationPermissionSideEffect.TrackPopup(accepted = granted),
            NotificationPermissionSideEffect.TrackPermissionResult(granted = granted),
        )

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
