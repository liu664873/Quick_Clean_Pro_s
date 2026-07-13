package com.quickcleanpro.phonecleaner.use.feature.files.presentation.photoprivacy

import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.ManagedFileItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileImageDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.managedFileToImageDisplayItem

internal data class PrivacyPhotoItem(
    val id: Int,
    val file: ManagedFileItem,
)

internal data class PhotoPrivacyManagerUiState(
    val phase: FileOperationPhase = FileOperationPhase.Scanning,
    val items: List<PrivacyPhotoItem> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val removedLocationCount: Int = 0,
    val errorMessage: String? = null,
) {
    val displayItems: List<FileImageDisplayItem> get() = items.map { it.toDisplayItem() }
    val visibleIds: Set<Int> get() = items.map { it.id }.toSet()
    val allSelected: Boolean get() = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)
    val selectedFiles: List<ManagedFileItem> get() = items.filter { it.id in selectedIds }.map { it.file }
}

internal fun mapPrivacyPhotos(files: List<ManagedFileItem>, limit: Int = 60): List<PrivacyPhotoItem> =
    files.take(limit).mapIndexed { index, file -> PrivacyPhotoItem(id = index + 1, file = file) }

internal fun PrivacyPhotoItem.toDisplayItem(): FileImageDisplayItem =
    managedFileToImageDisplayItem(id = id, file = file)
