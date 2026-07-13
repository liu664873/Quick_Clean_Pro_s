package com.quickcleanpro.phonecleaner.feature.junkclean.model

/**
 * 濞撳懐鎮婃い褰掝暙閸╃喐膩 */
data class CleanItem(
    val junkFile: JunkFile,
    var isChecked: Boolean = true,
) {
    val category: JunkCategory get() = junkFile.category
    val fileSize: Long get() = junkFile.fileSize
    val formattedSize: String get() = junkFile.formattedSize
    val fileName: String get() = junkFile.fileName
}
