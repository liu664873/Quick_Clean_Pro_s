package com.quickcleanpro.phonecleaner.feature.files.logic.duplicates


import com.quickcleanpro.phonecleaner.feature.files.logic.DuplicateFilesPreferences
import com.quickcleanpro.phonecleaner.feature.files.logic.FileRepository
import com.quickcleanpro.phonecleaner.feature.files.logic.ManagedFileItem
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateFilesWarningStateTest {
    @Test
    fun `accepting warning updates state and feature preferences`() {
        val preferences = FakeDuplicateFilesPreferences()
        val viewModel =
            DuplicateFilesManagerViewModel(
                repository = UnusedFileRepository,
                preferences = preferences,
                ioDispatcher = Dispatchers.Unconfined,
            )

        assertTrue(viewModel.uiState.value.showWarning)

        viewModel.acceptWarning()

        assertFalse(viewModel.uiState.value.showWarning)
        assertTrue(preferences.isWarningAccepted())
    }

    private class FakeDuplicateFilesPreferences : DuplicateFilesPreferences {
        private var accepted = false

        override fun isWarningAccepted(): Boolean = accepted

        override fun acceptWarning() {
            accepted = true
        }
    }

    private object UnusedFileRepository : FileRepository {
        override suspend fun loadImages(): List<ManagedFileItem> = emptyList()
        override suspend fun loadVideos(): List<ManagedFileItem> = emptyList()
        override suspend fun loadAudios(): List<ManagedFileItem> = emptyList()
        override suspend fun loadScreenshots(): List<ManagedFileItem> = emptyList()
        override suspend fun loadPrivacyImages(): List<ManagedFileItem> = emptyList()
        override suspend fun loadDocuments(): List<ManagedFileItem> = emptyList()
        override suspend fun loadLargeFiles(minBytes: Long): List<ManagedFileItem> = emptyList()
        override suspend fun loadDuplicateFiles(): List<List<ManagedFileItem>> = emptyList()
        override suspend fun loadWhatsAppFiles(): List<ManagedFileItem> = emptyList()
        override suspend fun deleteFiles(items: List<ManagedFileItem>): Long = 0L
        override suspend fun removeLocationData(items: List<ManagedFileItem>): Int = 0
        override fun hasAllFilesAccess(): Boolean = false
    }
}
