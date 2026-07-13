package com.quickcleanpro.phonecleaner.use.feature.onboarding.domain

interface OnboardingPreferences {
    fun isScanCompleted(): Boolean

    fun markScanCompleted()
}
