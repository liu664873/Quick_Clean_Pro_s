package com.quickcleanpro.phonecleaner.use.feature.startup.presentation

import androidx.lifecycle.ViewModel
import com.quickcleanpro.phonecleaner.use.feature.onboarding.domain.OnboardingPreferences

class SplashViewModel(
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {
    fun shouldShowOnboardingScan(): Boolean = !onboardingPreferences.isScanCompleted()
}
