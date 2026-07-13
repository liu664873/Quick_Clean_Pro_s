package com.quickcleanpro.phonecleaner.use.core.model.clean

/**
 * 清理项领域模 */
data class CleanItem(
    val junkFile: JunkFile,
    var isChecked: Boolean = true,
) {
    val category: JunkCategory get() = junkFile.category
    val fileSize: Long get() = junkFile.fileSize
    val formattedSize: String get() = junkFile.formattedSize
    val fileName: String get() = junkFile.fileName
}
