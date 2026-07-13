package com.quickcleanpro.phonecleaner.feature.junkclean.model

/**
 * 濞撳懐鎮婄紒鎾寸亯妫板棗鐓欏Ο鈥崇€?
 */
data class CleanResult(
    val cleanedFiles: List<JunkFile>,
    val freedSpace: Long,
    val failedFiles: List<JunkFile> = emptyList(),
) {
    val successCount: Int
        get() = cleanedFiles.size

    val failedCount: Int
        get() = failedFiles.size

    val formattedFreedSpace: String
        get() = JunkFile.formatFileSize(freedSpace)

    val isAllSuccess: Boolean
        get() = failedFiles.isEmpty()
}
