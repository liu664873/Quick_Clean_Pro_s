package com.quickcleanpro.phonecleaner.feature.onboarding

interface OnboardingPreferences {
    fun isScanCompleted(): Boolean

    fun markScanCompleted()
}
