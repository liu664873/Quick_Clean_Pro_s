package com.quickcleanpro.phonecleaner.common.analytics

import com.quickcleanpro.phonecleaner.common.storage.preferences.AppPreferences

interface AnalyticsPreferences {
    fun hasCompletedCleanup(): Boolean

    fun incrementCompletedCleanup(): Long

    fun hasReportedOnceUserProperties(): Boolean

    fun markOnceUserPropertiesReported()
}

class DefaultAnalyticsPreferences(
    appPreferences: AppPreferences,
) : AnalyticsPreferences {
    private val store = appPreferences.store

    override fun hasCompletedCleanup(): Boolean =
        store.getLong(KEY_TOTAL_CLEAN_COMPLETED, 0L) > 0L

    override fun incrementCompletedCleanup(): Long {
        val next = store.getLong(KEY_TOTAL_CLEAN_COMPLETED, 0L) + 1L
        store.edit().putLong(KEY_TOTAL_CLEAN_COMPLETED, next).apply()
        return next
    }

    override fun hasReportedOnceUserProperties(): Boolean =
        store.getBoolean(KEY_ONCE_USER_PROPERTIES_REPORTED, false)

    override fun markOnceUserPropertiesReported() {
        store.edit().putBoolean(KEY_ONCE_USER_PROPERTIES_REPORTED, true).commit()
    }
}

private const val KEY_TOTAL_CLEAN_COMPLETED = "analytics_total_cleancpl_num"
private const val KEY_ONCE_USER_PROPERTIES_REPORTED = "analytics_once_user_properties_reported"
