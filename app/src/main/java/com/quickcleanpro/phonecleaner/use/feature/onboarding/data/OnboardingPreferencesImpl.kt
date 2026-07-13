package com.quickcleanpro.phonecleaner.use.feature.onboarding.data

import com.quickcleanpro.phonecleaner.use.core.source.local.SharedPreferencesUtils
import com.quickcleanpro.phonecleaner.use.feature.onboarding.domain.OnboardingPreferences

class OnboardingPreferencesImpl : OnboardingPreferences {
    override fun isScanCompleted(): Boolean =
        SharedPreferencesUtils.getBoolean(
            key = SharedPreferencesUtils.KEY_ONBOARDING_SCAN_COMPLETED,
            defaultValue = false,
        )

    override fun markScanCompleted() {
        SharedPreferencesUtils.putBoolean(
            key = SharedPreferencesUtils.KEY_ONBOARDING_SCAN_COMPLETED,
            value = true,
        )
    }
}
