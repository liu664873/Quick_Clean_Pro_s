package com.quickcleanpro.phonecleaner.use.feature.files.data

import com.quickcleanpro.phonecleaner.use.core.source.local.SharedPreferencesUtils
import com.quickcleanpro.phonecleaner.use.feature.files.domain.DuplicateFilesPreferences

class DuplicateFilesPreferencesImpl : DuplicateFilesPreferences {
    override fun isWarningAccepted(): Boolean =
        SharedPreferencesUtils.getBoolean(SharedPreferencesUtils.KEY_DUPLICATE_FILES_WARNING_ACCEPTED)

    override fun acceptWarning() {
        SharedPreferencesUtils.putBoolean(
            SharedPreferencesUtils.KEY_DUPLICATE_FILES_WARNING_ACCEPTED,
            true,
            commit = true,
        )
    }
}
