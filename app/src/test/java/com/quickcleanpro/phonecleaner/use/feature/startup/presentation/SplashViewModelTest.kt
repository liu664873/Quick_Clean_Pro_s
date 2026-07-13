package com.quickcleanpro.phonecleaner.feature.startup

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.SdkInitializationCoordinator
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.app.runtime.startup.NotificationLaunchSource
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    @Test
    fun `sdk and visual readiness order emits one open ad effect`() {
        listOf(
            listOf(SplashAction.SdkBarrierFinished, SplashAction.VisualReady),
            listOf(SplashAction.VisualReady, SplashAction.SdkBarrierFinished),
        ).forEach { readinessActions ->
            var state = SplashMachineState(launchRequest = AppLaunchRequest.Normal)
            val effects = mutableListOf<SplashEffect>()

            readinessActions.forEach { action ->
                val transition = reduceSplashState(state, action, AppDestination.Home)
                state = transition.state
                effects += transition.effects
            }
            val duplicate = reduceSplashState(state, SplashAction.VisualReady, AppDestination.Home)
            effects += duplicate.effects

            assertEquals(SplashStage.WaitingForOpenAd, state.stage)
            assertEquals(listOf(SplashEffect.RunColdStartAd), effects)
        }
    }

    @Test
    fun `normal startup runs open ad once after sdk and visual are ready`() = runTest {
        val viewModel = SplashViewModel(FakeOnboardingPreferences(), readyCoordinator())

        viewModel.onAction(SplashAction.LaunchRequestChanged(AppLaunchRequest.Normal))
        viewModel.onAction(SplashAction.VisualReady)
        advanceUntilIdle()

        assertEquals(SplashEffect.RunColdStartAd, viewModel.effects.first())
        assertEquals(SplashStage.WaitingForOpenAd, viewModel.uiState.value.stage)
    }

    @Test
    fun `normal startup navigates home after ad and visual finish`() = runTest {
        val preferences = FakeOnboardingPreferences().apply { markScanCompleted() }
        val viewModel = SplashViewModel(preferences, readyCoordinator())
        viewModel.onAction(SplashAction.LaunchRequestChanged(AppLaunchRequest.Normal))
        viewModel.onAction(SplashAction.VisualReady)
        advanceUntilIdle()
        viewModel.effects.first()

        viewModel.onAction(SplashAction.OpenAdFinished)
        viewModel.onAction(SplashAction.VisualFinished)

        assertEquals(SplashEffect.Navigate(AppDestination.Home), viewModel.effects.first())
        assertEquals(SplashStage.Completed, viewModel.uiState.value.stage)
    }

    @Test
    fun `initial notification skips open ad and waits for visual finish`() = runTest {
        val viewModel = SplashViewModel(FakeOnboardingPreferences(), readyCoordinator())
        val request = notificationRequest(NotificationLaunchSource.InitialIntent)

        viewModel.onAction(SplashAction.LaunchRequestChanged(request))
        viewModel.onAction(SplashAction.VisualReady)
        advanceUntilIdle()

        assertEquals(SplashStage.Finishing, viewModel.uiState.value.stage)
        viewModel.onAction(SplashAction.VisualFinished)
        assertEquals(SplashEffect.OpenNotificationTarget(request.route), viewModel.effects.first())
    }

    @Test
    fun `new intent immediately completes every active stage`() {
        val request = notificationRequest(NotificationLaunchSource.NewIntent)
        listOf(
            SplashMachineState(stage = SplashStage.Preparing),
            SplashMachineState(stage = SplashStage.WaitingForOpenAd),
            SplashMachineState(stage = SplashStage.Finishing),
        ).forEach { state ->
            val transition = reduceSplashState(state, SplashAction.LaunchRequestChanged(request), AppDestination.Home)

            assertEquals(SplashStage.Completed, transition.state.stage)
            assertEquals(listOf(SplashEffect.OpenNotificationTarget(request.route)), transition.effects)
        }
    }

    @Test
    fun `pause reasons resume only after every reason is cleared`() {
        var state = SplashMachineState()

        state = transition(state, SplashAction.PermissionPauseChanged(true))
        state = transition(state, SplashAction.OpenAdStateChanged(true))
        state = transition(state, SplashAction.ExternalLinkStateChanged(true))
        assertTrue(state.toUiState().paused)

        state = transition(state, SplashAction.PermissionPauseChanged(false))
        state = transition(state, SplashAction.OpenAdStateChanged(false))
        assertTrue(state.toUiState().paused)

        state = transition(state, SplashAction.ExternalLinkStateChanged(false))
        assertFalse(state.toUiState().paused)
    }

    @Test
    fun `duplicate completion events are idempotent`() {
        var state = SplashMachineState(
            stage = SplashStage.WaitingForOpenAd,
            sdkBarrierFinished = true,
            visualReady = true,
            launchRequest = AppLaunchRequest.Normal,
        )

        var result = reduceSplashState(state, SplashAction.OpenAdFinished, AppDestination.Home)
        state = result.state
        assertEquals(SplashStage.Finishing, state.stage)

        result = reduceSplashState(state, SplashAction.OpenAdFinished, AppDestination.Home)
        assertTrue(result.effects.isEmpty())

        result = reduceSplashState(state, SplashAction.VisualFinished, AppDestination.Home)
        state = result.state
        assertEquals(listOf(SplashEffect.Navigate(AppDestination.Home)), result.effects)

        result = reduceSplashState(state, SplashAction.VisualFinished, AppDestination.Home)
        assertTrue(result.effects.isEmpty())
    }

    @Test
    fun `incomplete onboarding navigates to onboarding scan`() = runTest {
        val viewModel = SplashViewModel(FakeOnboardingPreferences(), readyCoordinator())
        viewModel.onAction(SplashAction.LaunchRequestChanged(AppLaunchRequest.Normal))
        viewModel.onAction(SplashAction.VisualReady)
        advanceUntilIdle()
        viewModel.effects.first()

        viewModel.onAction(SplashAction.OpenAdFinished)
        viewModel.onAction(SplashAction.VisualFinished)

        assertEquals(SplashEffect.Navigate(AppDestination.OnboardingScan), viewModel.effects.first())
    }

    private fun transition(state: SplashMachineState, action: SplashAction): SplashMachineState =
        reduceSplashState(state, action, AppDestination.Home).state

    private fun notificationRequest(source: NotificationLaunchSource): AppLaunchRequest.NotificationTarget =
        AppLaunchRequest.NotificationTarget(
            route = AppDestination.NotificationCleaner.route,
            source = source,
        )

    private fun TestScope.readyCoordinator(): SdkInitializationCoordinator =
        SdkInitializationCoordinator(
            scope = this,
            advertiseInitializer = {},
            analyticsInitializer = {},
            notificationDefaultsInitializer = {},
        ).also { it.start() }

    private class FakeOnboardingPreferences : OnboardingPreferences {
        private var completed = false

        override fun isScanCompleted(): Boolean = completed

        override fun markScanCompleted() {
            completed = true
        }
    }
}
