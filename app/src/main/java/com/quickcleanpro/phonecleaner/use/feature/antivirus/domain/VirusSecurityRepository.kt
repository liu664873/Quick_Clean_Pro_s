package com.quickcleanpro.phonecleaner.use.feature.antivirus.domain

interface VirusSecurityRepository {
    fun hasInstalledAppsAccess(): Boolean

    fun hasAdbRisk(): Boolean

    fun trustlookApiKeyState(): TrustlookApiKeyState
}

data class TrustlookApiKeyState(
    val isConfigured: Boolean,
    val logState: String,
)
