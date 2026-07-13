package com.quickcleanpro.phonecleaner.feature.files.shared

import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter

enum class ManagedFileType { Image, Video, Audio, Document, Other }

@JvmInline
value class FileUri(val value: String) {
    override fun toString(): String = value
}

data class ManagedFileItem(
    val id: Long,
    val uri: FileUri,
    val path: String?,
    val name: String,
    val sizeBytes: Long,
    val modifiedSeconds: Long,
    val mimeType: String?,
    val bucketName: String?,
    val type: ManagedFileType,
) {
    val formattedSize: String get() = FileSizeFormatter.format(sizeBytes)
}
