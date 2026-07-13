package com.quickcleanpro.phonecleaner.feature.files.photos

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.FileUri
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileImageDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerTabDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.managedFileToImageDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.splitFileSizeLabel
import com.quickcleanpro.phonecleaner.feature.files.shared.totalSizeLabel
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter

internal data class PhotosManagerItem(
    val id: Int,
    val file: ManagedFileItem,
)

internal data class PhotosManagerTab(
    val title: String,
    val sizeLabel: String?,
    val itemIds: Set<Int>,
)

internal data class PhotosManagerUiState(
    val phase: FileOperationPhase = FileOperationPhase.Scanning,
    val items: List<PhotosManagerItem> = emptyList(),
    val tabs: List<PhotosManagerTab> = emptyList(),
    val selectedTabIndex: Int = 0,
    val selectedIds: Set<Int> = emptySet(),
    val detailStartIndex: Int? = null,
    val deletedBytes: Long = 0L,
    val errorMessage: String? = null,
) {
    val displayItems: List<FileImageDisplayItem> get() = items.map { it.toDisplayItem() }

    val displayTabs: List<FileManagerTabDisplayItem>
        get() = tabs.map { FileManagerTabDisplayItem(it.title, it.sizeLabel.orEmpty()) }

    val currentDisplayItems: List<FileImageDisplayItem>
        get() {
            val ids = tabs.getOrNull(selectedTabIndex)?.itemIds ?: items.map { it.id }.toSet()
            return displayItems.filter { it.id in ids }
        }

    val visibleIds: Set<Int> get() = currentDisplayItems.map { it.id }.toSet()

    val allSelected: Boolean get() = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)

    val selectedFiles: List<ManagedFileItem>
        get() = items.filter { it.id in selectedIds }.map { it.file }

    val selectedSizeBytes: Long get() = selectedFiles.sumOf { it.sizeBytes }

    val selectedUris: List<FileUri> get() = selectedFiles.map { it.uri }

    val resultSize: Pair<String, String> get() = FileSizeFormatter.format(deletedBytes).splitFileSizeLabel()
}

internal fun mapPhotos(files: List<ManagedFileItem>, limit: Int = 60): List<PhotosManagerItem> =
    files.take(limit).mapIndexed { index, file -> PhotosManagerItem(id = index + 1, file = file) }

internal fun buildPhotoTabs(items: List<PhotosManagerItem>): List<PhotosManagerTab> {
    fun PhotosManagerItem.matchesFolder(folder: String): Boolean =
        file.bucketName?.contains(folder, ignoreCase = true) == true ||
            file.path?.contains("/$folder/", ignoreCase = true) == true

    val pictures = items.filter { it.matchesFolder("Pictures") }
    val dcim = items.filter { it.matchesFolder("DCIM") || it.matchesFolder("Camera") }
    val used = (pictures + dcim).map { it.id }.toSet()
    val other = items.filter { it.id !in used }

    fun tab(title: String, tabItems: List<PhotosManagerItem>) =
        PhotosManagerTab(
            title = title,
            sizeLabel = totalSizeLabel(tabItems.map { it.file }).takeIf { it.isNotBlank() },
            itemIds = tabItems.map { it.id }.toSet(),
        )

    return listOf(
        tab("Photo", items),
        tab("Pictures", pictures),
        tab("DCIM", dcim),
        tab("Other", other),
    )
}

internal fun PhotosManagerItem.toDisplayItem(): FileImageDisplayItem =
    managedFileToImageDisplayItem(id = id, file = file)
