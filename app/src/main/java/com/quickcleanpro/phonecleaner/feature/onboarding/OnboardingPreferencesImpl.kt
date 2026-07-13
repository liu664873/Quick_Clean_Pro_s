package com.quickcleanpro.phonecleaner.feature.onboarding

import com.quickcleanpro.phonecleaner.common.storage.preferences.AppPreferences

class OnboardingPreferencesImpl(
    appPreferences: AppPreferences,
) : OnboardingPreferences {
    private val store = appPreferences.store

    override fun isScanCompleted(): Boolean =
        store.getBoolean(KEY_ONBOARDING_SCAN_COMPLETED, false)

    override fun markScanCompleted() {
        store.edit().putBoolean(KEY_ONBOARDING_SCAN_COMPLETED, true).apply()
    }
}

private const val KEY_ONBOARDING_SCAN_COMPLETED = "onboarding_scan_completed"
