package com.quickcleanpro.phonecleaner.feature.files.audios

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.FileUri
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import com.quickcleanpro.phonecleaner.feature.files.shared.buildMediaTabs
import com.quickcleanpro.phonecleaner.feature.files.shared.FileImageDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerTabDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.filterMediaGridItems
import com.quickcleanpro.phonecleaner.feature.files.shared.managedFileToImageDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.splitFileSizeLabel
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter

internal data class AudioItem(
    val id: Int,
    val file: ManagedFileItem,
)

internal data class AudiosManagerUiState(
    val phase: FileOperationPhase = FileOperationPhase.Scanning,
    val items: List<AudioItem> = emptyList(),
    val tabs: List<FileManagerTabDisplayItem> = emptyList(),
    val selectedTabIndex: Int = 0,
    val selectedIds: Set<Int> = emptySet(),
    val detailStartIndex: Int? = null,
    val deletedBytes: Long = 0L,
    val errorMessage: String? = null,
) {
    val displayItems: List<FileImageDisplayItem> get() = items.map { it.toDisplayItem() }
    val displayTabs: List<FileManagerTabDisplayItem> get() = tabs
    val visibleDisplayItems: List<FileImageDisplayItem>
        get() = filterMediaGridItems(tabs.getOrNull(selectedTabIndex)?.title.orEmpty(), displayItems)
    val visibleIds: Set<Int> get() = visibleDisplayItems.map { it.id }.toSet()
    val allSelected: Boolean get() = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)
    val selectedFiles: List<ManagedFileItem> get() = items.filter { it.id in selectedIds }.map { it.file }
    val selectedSizeBytes: Long get() = selectedFiles.sumOf { it.sizeBytes }
    val selectedUris: List<FileUri> get() = selectedFiles.map { it.uri }
    val resultSize: Pair<String, String> get() = FileSizeFormatter.format(deletedBytes).splitFileSizeLabel()
}

internal fun mapAudios(files: List<ManagedFileItem>, limit: Int = 60): List<AudioItem> =
    files.take(limit).mapIndexed { index, file -> AudioItem(id = index + 1, file = file) }

internal fun buildAudioTabs(items: List<AudioItem>): List<FileManagerTabDisplayItem> {
    val displayItems = items.map { it.toDisplayItem() }
    return buildMediaTabs(
        titles = listOf("All", "Music"),
        items = displayItems,
        fileForId = { id -> items.firstOrNull { it.id == id }?.file },
    )
}

internal fun AudioItem.toDisplayItem(): FileImageDisplayItem =
    managedFileToImageDisplayItem(id = id, file = file)
