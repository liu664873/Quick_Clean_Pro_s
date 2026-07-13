package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.FileUri

internal data class FileDetailUiState(
    val items: List<FileDetailDisplayItem> = emptyList(),
    val initialIndex: Int = 0,
    val selectedIds: Set<Int> = emptySet(),
    val selectedSizeBytes: Long = 0L,
    val selectedUris: List<FileUri> = emptyList(),
    val confirmDeleteVisible: Boolean = false,
) {
    val selectedCount: Int get() = selectedIds.size
    val totalCount: Int get() = items.size
}
