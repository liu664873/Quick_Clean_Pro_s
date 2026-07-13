package com.quickcleanpro.phonecleaner.use.feature.antivirus.data

import com.quickcleanpro.phonecleaner.use.core.source.local.SharedPreferencesUtils
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.AntivirusPreferences

class AntivirusPreferencesImpl : AntivirusPreferences {
    override fun isScanNoticeAccepted(): Boolean =
        SharedPreferencesUtils.getBoolean(SharedPreferencesUtils.KEY_VIRUS_SCAN_NOTICE_ACCEPTED)

    override fun setScanNoticeAccepted() {
        SharedPreferencesUtils.putBoolean(
            SharedPreferencesUtils.KEY_VIRUS_SCAN_NOTICE_ACCEPTED,
            true,
            commit = true,
        )
    }

    override fun hasInstalledAppsAccessFailedBefore(): Boolean =
        SharedPreferencesUtils.getBoolean(SharedPreferencesUtils.KEY_VIRUS_INSTALLED_APPS_ACCESS_FAILED_ONCE)

    override fun setInstalledAppsAccessFailed(failed: Boolean) {
        if (failed) {
            SharedPreferencesUtils.putBoolean(
                SharedPreferencesUtils.KEY_VIRUS_INSTALLED_APPS_ACCESS_FAILED_ONCE,
                true,
            )
        } else {
            SharedPreferencesUtils.remove(SharedPreferencesUtils.KEY_VIRUS_INSTALLED_APPS_ACCESS_FAILED_ONCE)
        }
    }
}
