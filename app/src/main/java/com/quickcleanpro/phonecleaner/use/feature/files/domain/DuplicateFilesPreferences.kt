package com.quickcleanpro.phonecleaner.use.feature.files.domain

interface DuplicateFilesPreferences {
    fun isWarningAccepted(): Boolean

    fun acceptWarning()
}
