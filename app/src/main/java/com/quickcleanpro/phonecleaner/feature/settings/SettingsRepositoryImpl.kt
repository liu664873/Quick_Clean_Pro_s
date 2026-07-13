package com.quickcleanpro.phonecleaner.feature.settings

import com.quickcleanpro.phonecleaner.common.storage.preferences.AppPreferences

class SettingsRepositoryImpl(
    appPreferences: AppPreferences,
) : SettingsRepository {
    private val store = appPreferences.store

    override fun readTemperatureUnit(): String = store.getString(KEY_TEMPERATURE_UNIT, "C") ?: "C"

    override fun saveTemperatureUnit(unit: String) {
        store.edit().putString(KEY_TEMPERATURE_UNIT, unit).apply()
    }

    override fun readLastAutoRatePromptAt(): Long = store.getLong(KEY_LAST_AUTO_RATE_PROMPT_AT, 0L)

    override fun saveLastAutoRatePromptAt(timestampMillis: Long) {
        store.edit().putLong(KEY_LAST_AUTO_RATE_PROMPT_AT, timestampMillis).apply()
    }

    override fun hasShownNotificationCleanerExitPrompt(): Boolean =
        store.getBoolean(KEY_NOTIFICATION_CLEANER_EXIT_PROMPT_SHOWN, false)

    override fun saveNotificationCleanerExitPromptShown() {
        store.edit().putBoolean(KEY_NOTIFICATION_CLEANER_EXIT_PROMPT_SHOWN, true).apply()
    }

}

private const val KEY_TEMPERATURE_UNIT = "temperature_unit"
private const val KEY_LAST_AUTO_RATE_PROMPT_AT = "last_auto_rate_prompt_at"
private const val KEY_NOTIFICATION_CLEANER_EXIT_PROMPT_SHOWN = "notification_bar_exit_prompt_shown"
