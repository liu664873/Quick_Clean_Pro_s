package com.quickcleanpro.phonecleaner.use.feature.settings.data

import android.content.Context
import com.quickcleanpro.phonecleaner.use.core.source.local.hasCompletedOnboardingScan
import com.quickcleanpro.phonecleaner.use.core.source.local.hasDeniedLocationRuntimePermission
import com.quickcleanpro.phonecleaner.use.core.source.local.hasDeniedNotificationRuntimePermission
import com.quickcleanpro.phonecleaner.use.core.source.local.hasRequestedLocationRuntimePermissionBefore
import com.quickcleanpro.phonecleaner.use.core.source.local.hasRequestedNotificationRuntimePermissionBefore
import com.quickcleanpro.phonecleaner.use.core.source.local.hasShownNotificationCleanerExitPrompt
import com.quickcleanpro.phonecleaner.use.core.source.local.readLastNotificationPermissionCustomPromptAt
import com.quickcleanpro.phonecleaner.use.core.source.local.readLastAutoRatePromptAt
import com.quickcleanpro.phonecleaner.use.core.source.local.readTemperatureUnit
import com.quickcleanpro.phonecleaner.use.core.source.local.saveLastNotificationPermissionCustomPromptAt
import com.quickcleanpro.phonecleaner.use.core.source.local.saveLastAutoRatePromptAt
import com.quickcleanpro.phonecleaner.use.core.source.local.saveLocationRuntimePermissionDenied
import com.quickcleanpro.phonecleaner.use.core.source.local.saveLocationRuntimePermissionRequestedBefore
import com.quickcleanpro.phonecleaner.use.core.source.local.saveNotificationCleanerExitPromptShown
import com.quickcleanpro.phonecleaner.use.core.source.local.saveNotificationRuntimePermissionRequestedBefore
import com.quickcleanpro.phonecleaner.use.core.source.local.saveNotificationRuntimePermissionDenied
import com.quickcleanpro.phonecleaner.use.core.source.local.saveOnboardingScanCompleted
import com.quickcleanpro.phonecleaner.use.core.source.local.saveTemperatureUnit
import com.quickcleanpro.phonecleaner.use.core.repository.SettingsRepository

class SettingsRepositoryImpl(
    context: Context,
) : SettingsRepository {
    private val appContext = context.applicationContext

    override fun readTemperatureUnit(): String = readTemperatureUnit(appContext)

    override fun saveTemperatureUnit(unit: String) {
        saveTemperatureUnit(appContext, unit)
    }

    override fun readLastAutoRatePromptAt(): Long = readLastAutoRatePromptAt(appContext)

    override fun saveLastAutoRatePromptAt(timestampMillis: Long) {
        saveLastAutoRatePromptAt(appContext, timestampMillis)
    }

    override fun hasShownNotificationCleanerExitPrompt(): Boolean = hasShownNotificationCleanerExitPrompt(appContext)

    override fun saveNotificationCleanerExitPromptShown() {
        saveNotificationCleanerExitPromptShown(appContext)
    }

    override fun hasCompletedOnboardingScan(): Boolean = hasCompletedOnboardingScan(appContext)

    override fun saveOnboardingScanCompleted() {
        saveOnboardingScanCompleted(appContext)
    }

    override fun hasDeniedLocationRuntimePermission(): Boolean = hasDeniedLocationRuntimePermission(appContext)

    override fun saveLocationRuntimePermissionDenied() {
        saveLocationRuntimePermissionDenied(appContext)
    }

    override fun hasRequestedLocationRuntimePermissionBefore(): Boolean =
        hasRequestedLocationRuntimePermissionBefore(appContext)

    override fun saveLocationRuntimePermissionRequestedBefore() {
        saveLocationRuntimePermissionRequestedBefore(appContext)
    }

    override fun hasDeniedNotificationRuntimePermission(): Boolean = hasDeniedNotificationRuntimePermission(appContext)

    override fun saveNotificationRuntimePermissionDenied() {
        saveNotificationRuntimePermissionDenied(appContext)
    }

    override fun hasRequestedNotificationRuntimePermissionBefore(): Boolean =
        hasRequestedNotificationRuntimePermissionBefore(appContext)

    override fun saveNotificationRuntimePermissionRequestedBefore() {
        saveNotificationRuntimePermissionRequestedBefore(appContext)
    }

    override fun readLastNotificationPermissionCustomPromptAt(): Long =
        readLastNotificationPermissionCustomPromptAt(appContext)

    override fun saveLastNotificationPermissionCustomPromptAt(timestampMillis: Long) {
        saveLastNotificationPermissionCustomPromptAt(appContext, timestampMillis)
    }
}
