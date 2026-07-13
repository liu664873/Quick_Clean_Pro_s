package com.quickcleanpro.phonecleaner.use.core.model.clean

/**
 * 主清理链路中的垃圾文件模型 *
 * 该模型表示已经被扫描器识别出的可清理文件 * 会在仓库、用例、共享状态和结果页之间传递 */
data class JunkFile(
    val id: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val category: JunkCategory,
    val lastModified: Long = System.currentTimeMillis(),
) {
    /** 格式化后的文件大小文案*/
    val formattedSize: String
        get() = formatFileSize(fileSize)

    companion object {
        /**
         * 将字节数格式化为面向 UI 的短文本         */
        fun formatFileSize(bytes: Long): String =
            when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
                bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
                else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
            }
    }
}
