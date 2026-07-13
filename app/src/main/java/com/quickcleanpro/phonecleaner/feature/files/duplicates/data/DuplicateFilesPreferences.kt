package com.quickcleanpro.phonecleaner.feature.files.duplicates.data

interface DuplicateFilesPreferences {
    fun isWarningAccepted(): Boolean

    fun acceptWarning()
}
