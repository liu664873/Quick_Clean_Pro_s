package com.quickcleanpro.phonecleaner.feature.antivirus

import com.quickcleanpro.phonecleaner.common.storage.preferences.AppPreferences

class AntivirusPreferencesImpl(
    appPreferences: AppPreferences,
) : AntivirusPreferences {
    private val store = appPreferences.store

    override fun isScanNoticeAccepted(): Boolean =
        store.getBoolean(KEY_VIRUS_SCAN_NOTICE_ACCEPTED, false)

    override fun setScanNoticeAccepted() {
        store.edit().putBoolean(KEY_VIRUS_SCAN_NOTICE_ACCEPTED, true).commit()
    }

    override fun hasInstalledAppsAccessFailedBefore(): Boolean =
        store.getBoolean(KEY_VIRUS_INSTALLED_APPS_ACCESS_FAILED_ONCE, false)

    override fun setInstalledAppsAccessFailed(failed: Boolean) {
        if (failed) {
            store.edit().putBoolean(KEY_VIRUS_INSTALLED_APPS_ACCESS_FAILED_ONCE, true).apply()
        } else {
            store.edit().remove(KEY_VIRUS_INSTALLED_APPS_ACCESS_FAILED_ONCE).apply()
        }
    }
}

private const val KEY_VIRUS_SCAN_NOTICE_ACCEPTED = "virus_scan_notice_accepted"
private const val KEY_VIRUS_INSTALLED_APPS_ACCESS_FAILED_ONCE = "virus_installed_apps_access_failed_once"
