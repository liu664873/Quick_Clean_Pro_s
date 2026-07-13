package com.quickcleanpro.phonecleaner.use.feature.antivirus.domain

interface AntivirusPreferences {
    fun isScanNoticeAccepted(): Boolean

    fun setScanNoticeAccepted()

    fun hasInstalledAppsAccessFailedBefore(): Boolean

    fun setInstalledAppsAccessFailed(failed: Boolean)
}
