package com.quickcleanpro.phonecleaner.feature.startup.logic

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.SdkInitializationCoordinator
import com.quickcleanpro.phonecleaner.common.startup.AppLaunchRequest
import com.quickcleanpro.phonecleaner.common.startup.NotificationLaunchSource
import com.quickcleanpro.phonecleaner.feature.onboarding.logic.OnboardingPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashViewModelTest {
    @Test
    fun `normal startup waits for sdk and animation before requesting open ad`() = runTest {
        val coordinator = readyCoordinator()
        val viewModel = SplashViewModel(FakeOnboardingPreferences(), coordinator)

        viewModel.onAnimationReady()
        advanceUntilIdle()

        assertEquals(SplashEffect.ShowOpenAd, viewModel.effects.first())
        assertEquals(SplashPhase.WaitingForOpenAd, viewModel.uiState.value.phase)
    }

    @Test
    fun `open ad completion navigates once after splash animation finishes`() = runTest {
        val preferences = FakeOnboardingPreferences().apply { markScanCompleted() }
        val viewModel = SplashViewModel(preferences, readyCoordinator())
        viewModel.onAnimationReady()
        advanceUntilIdle()
        viewModel.effects.first()

        viewModel.onOpenAdFinished()
        viewModel.onSplashFinished()

        assertEquals(SplashEffect.Navigate(AppDestination.Home), viewModel.effects.first())
        viewModel.onSplashFinished()
        assertEquals(SplashPhase.Ready, viewModel.uiState.value.phase)
    }

    @Test
    fun `new notification intent bypasses remaining startup work`() = runTest {
        val viewModel = SplashViewModel(FakeOnboardingPreferences(), readyCoordinator())

        viewModel.onLaunchRequest(
            AppLaunchRequest.NotificationTarget(
                route = AppDestination.NotificationCleaner.route,
                source = NotificationLaunchSource.NewIntent,
            ),
        )

        assertEquals(
            SplashEffect.OpenNotificationTarget(AppDestination.NotificationCleaner.route),
            viewModel.effects.first(),
        )
    }

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
