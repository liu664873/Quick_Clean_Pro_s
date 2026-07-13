package com.quickcleanpro.phonecleaner.use.feature.files.presentation.documents

import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.FileUri
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.ManagedFileItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.buildListTabs
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileListDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileListDisplayStyle
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileListIconKind
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileManagerTabDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.filterFileManagerListItems
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.managedFileToListDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.splitFileSizeLabel
import com.quickcleanpro.phonecleaner.use.core.common.format.FileSizeFormatter

data class DocumentItem(
    val id: Int,
    val file: ManagedFileItem,
)

data class DocumentsManagerUiState(
    val phase: FileOperationPhase = FileOperationPhase.Scanning,
    val items: List<DocumentItem> = emptyList(),
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

internal fun mapDocuments(files: List<ManagedFileItem>): List<DocumentItem> =
    files.sortedByDescending { it.sizeBytes }
        .mapIndexed { index, file -> DocumentItem(id = index + 1, file = file) }

internal fun buildDocumentTabs(items: List<DocumentItem>): List<FileManagerTabDisplayItem> {
    val displayItems = items.map { it.toDisplayItem() }
    return buildListTabs(
        titles = listOf("All", "Download", "Other"),
        items = displayItems,
        fileForId = { id -> items.firstOrNull { it.id == id }?.file },
    )
}

internal fun DocumentItem.toDisplayItem(): FileListDisplayItem =
    managedFileToListDisplayItem(
        id = id,
        file = file,
        iconKind = FileListIconKind.Document,
    )

internal val DocumentsListStyle: FileListDisplayStyle = FileListDisplayStyle.Documents
