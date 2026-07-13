package com.quickcleanpro.phonecleaner.feature.junkclean.model

data class ScanResult(
    val junkFiles: List<JunkFile>,
    val totalSize: Long,
    val totalCount: Int,
    val categoryBreakdown: Map<JunkCategory, List<JunkFile>>,
) {

    val formattedTotalSize: String
        get() = JunkFile.formatFileSize(totalSize)

    companion object {

        val EMPTY =
            ScanResult(
                junkFiles = emptyList(),
                totalSize = 0,
                totalCount = 0,
                categoryBreakdown = emptyMap(),
            )
    }
}
