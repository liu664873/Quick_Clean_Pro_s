package com.quickcleanpro.phonecleaner.feature.files.largefiles

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.FileUri
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import com.quickcleanpro.phonecleaner.feature.files.shared.buildListTabs
import com.quickcleanpro.phonecleaner.feature.files.shared.FileListDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileListDisplayStyle
import com.quickcleanpro.phonecleaner.feature.files.shared.FileListIconKind
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerTabDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.filterFileManagerListItems
import com.quickcleanpro.phonecleaner.feature.files.shared.managedFileToListDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.splitFileSizeLabel
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter

internal data class LargeFileItem(
    val id: Int,
    val file: ManagedFileItem,
)

internal data class LargeFilesManagerUiState(
    val phase: FileOperationPhase = FileOperationPhase.Scanning,
    val items: List<LargeFileItem> = emptyList(),
    val tabs: List<FileManagerTabDisplayItem> = emptyList(),
    val selectedTabIndex: Int = 0,
    val selectedIds: Set<Int> = emptySet(),
    val detailStartIndex: Int? = null,
    val deletedBytes: Long = 0L,
    val errorMessage: String? = null,
) {
    val displayItems: List<FileListDisplayItem> get() = items.map { it.toDisplayItem() }

    val displayTabs: List<FileManagerTabDisplayItem> get() = tabs

    val visibleDisplayItems: List<FileListDisplayItem>
        get() = filterFileManagerListItems(tabs.getOrNull(selectedTabIndex)?.title.orEmpty(), displayItems)

    val visibleIds: Set<Int> get() = visibleDisplayItems.map { it.id }.toSet()

    val allSelected: Boolean get() = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)

    val selectedFiles: List<ManagedFileItem>
        get() = items.filter { it.id in selectedIds }.map { it.file }

    val selectedSizeBytes: Long get() = selectedFiles.sumOf { it.sizeBytes }

    val selectedUris: List<FileUri> get() = selectedFiles.map { it.uri }

    val resultSize: Pair<String, String> get() = FileSizeFormatter.format(deletedBytes).splitFileSizeLabel()
}

internal fun mapLargeFiles(files: List<ManagedFileItem>): List<LargeFileItem> =
    files.sortedByDescending { it.sizeBytes }
        .mapIndexed { index, file -> LargeFileItem(id = index + 1, file = file) }

internal fun buildLargeFileTabs(items: List<LargeFileItem>): List<FileManagerTabDisplayItem> {
    val displayItems = items.map { it.toDisplayItem() }
    return buildListTabs(
        titles = listOf("All", "Download", "Other"),
        items = displayItems,
        fileForId = { id -> items.firstOrNull { it.id == id }?.file },
    )
}

internal fun LargeFileItem.toDisplayItem(): FileListDisplayItem =
    managedFileToListDisplayItem(
        id = id,
        file = file,
        iconKind = FileListIconKind.LargeVideo,
    )

internal val LargeFilesListStyle: FileListDisplayStyle = FileListDisplayStyle.Default
