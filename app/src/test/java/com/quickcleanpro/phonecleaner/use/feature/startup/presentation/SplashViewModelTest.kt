package com.quickcleanpro.phonecleaner.use.feature.startup.presentation

import com.quickcleanpro.phonecleaner.use.feature.onboarding.domain.OnboardingPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashViewModelTest {
    @Test
    fun `onboarding visibility is read from feature preferences`() {
        val preferences = FakeOnboardingPreferences()
        val viewModel = SplashViewModel(preferences)

        assertTrue(viewModel.shouldShowOnboardingScan())

        preferences.markScanCompleted()

        assertFalse(viewModel.shouldShowOnboardingScan())
    }

    private class FakeOnboardingPreferences : OnboardingPreferences {
        private var completed = false

        override fun isScanCompleted(): Boolean = completed

        override fun markScanCompleted() {
            completed = true
        }
    }
}
