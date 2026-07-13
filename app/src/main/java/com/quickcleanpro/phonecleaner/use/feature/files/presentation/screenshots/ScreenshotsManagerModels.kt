package com.quickcleanpro.phonecleaner.use.feature.files.presentation.screenshots

import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.FileUri
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.ManagedFileItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileImageDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.managedFileToImageDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.splitFileSizeLabel
import com.quickcleanpro.phonecleaner.use.core.common.format.FileSizeFormatter

internal data class ScreenshotItem(
    val id: Int,
    val file: ManagedFileItem,
)

internal data class ScreenshotsManagerUiState(
    val phase: FileOperationPhase = FileOperationPhase.Scanning,
    val items: List<ScreenshotItem> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val detailStartIndex: Int? = null,
    val deletedBytes: Long = 0L,
    val errorMessage: String? = null,
) {
    val displayItems: List<FileImageDisplayItem> get() = items.map { it.toDisplayItem() }
    val visibleIds: Set<Int> get() = items.map { it.id }.toSet()
    val allSelected: Boolean get() = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)
    val selectedFiles: List<ManagedFileItem> get() = items.filter { it.id in selectedIds }.map { it.file }
    val selectedSizeBytes: Long get() = selectedFiles.sumOf { it.sizeBytes }
    val selectedUris: List<FileUri> get() = selectedFiles.map { it.uri }
    val resultSize: Pair<String, String> get() = FileSizeFormatter.format(deletedBytes).splitFileSizeLabel()
}

internal fun mapScreenshots(files: List<ManagedFileItem>, limit: Int = 60): List<ScreenshotItem> =
    files.take(limit).mapIndexed { index, file -> ScreenshotItem(id = index + 1, file = file) }

internal fun ScreenshotItem.toDisplayItem(): FileImageDisplayItem =
    managedFileToImageDisplayItem(id = id, file = file)
