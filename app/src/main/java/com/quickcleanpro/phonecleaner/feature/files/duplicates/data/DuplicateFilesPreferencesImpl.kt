package com.quickcleanpro.phonecleaner.feature.files.duplicates.data

import com.quickcleanpro.phonecleaner.common.storage.preferences.AppPreferences

class DuplicateFilesPreferencesImpl(
    appPreferences: AppPreferences,
) : DuplicateFilesPreferences {
    private val store = appPreferences.store

    override fun isWarningAccepted(): Boolean =
        store.getBoolean(KEY_DUPLICATE_FILES_WARNING_ACCEPTED, false)

    override fun acceptWarning() {
        store.edit().putBoolean(KEY_DUPLICATE_FILES_WARNING_ACCEPTED, true).commit()
    }
}

private const val KEY_DUPLICATE_FILES_WARNING_ACCEPTED = "duplicate_files_warning_accepted"
