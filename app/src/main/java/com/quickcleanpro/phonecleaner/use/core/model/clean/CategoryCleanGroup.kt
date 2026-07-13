package com.quickcleanpro.phonecleaner.use.core.model.clean

data class CategoryCleanGroup(
    val category: JunkCategory,
    val items: List<CleanItem>,
) {
    val totalSize: Long
        get() = items.sumOf { it.fileSize }

    val checkedSize: Long
        get() = items.filter { it.isChecked }.sumOf { it.fileSize }

    val checkedCount: Int
        get() = items.count { it.isChecked }

    val totalCount: Int
        get() = items.size

    val formattedTotalSize: String
        get() = JunkFile.formatFileSize(totalSize)

    val formattedCheckedSize: String
        get() = JunkFile.formatFileSize(checkedSize)
}
