package com.quickcleanpro.phonecleaner.use.feature.files.domain

import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.ManagedFileItem

interface FileRepository {
    suspend fun loadImages(): List<ManagedFileItem>

    suspend fun loadVideos(): List<ManagedFileItem>

    suspend fun loadAudios(): List<ManagedFileItem>

    suspend fun loadScreenshots(): List<ManagedFileItem>

    suspend fun loadPrivacyImages(): List<ManagedFileItem>

    suspend fun loadDocuments(): List<ManagedFileItem>

    suspend fun loadLargeFiles(minBytes: Long = 10L * 1024 * 1024): List<ManagedFileItem>

    suspend fun loadDuplicateFiles(): List<List<ManagedFileItem>>

    suspend fun loadWhatsAppFiles(): List<ManagedFileItem>

    suspend fun deleteFiles(items: List<ManagedFileItem>): Long

    suspend fun removeLocationData(items: List<ManagedFileItem>): Int

    fun hasAllFilesAccess(): Boolean
}
