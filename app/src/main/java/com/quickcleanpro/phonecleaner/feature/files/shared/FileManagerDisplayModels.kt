package com.quickcleanpro.phonecleaner.feature.files.shared

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.FileUri
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import com.quickcleanpro.phonecleaner.feature.files.shared.filterFileManagerListItems
import com.quickcleanpro.phonecleaner.feature.files.shared.filterMediaGridItems
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileManagerTabDisplayItem(
    val title: String,
    val sizeLabel: String = ""
)

internal data class FileImageDisplayItem(
    val id: Int,
    val name: String,
    val meta: String,
    val sizeLabel: String,
    val paletteIndex: Int,
    val uri: FileUri?,
    val bucketName: String?,
    val path: String?,
    val bestPhoto: Boolean = false,
)

internal data class FileImageGroupDisplayItem(
    val count: Int,
    val items: List<FileImageDisplayItem>
)

internal enum class FileListDisplayStyle {
    Default,
    Documents
}

enum class FileListIconKind {
    LargeVideo,
    Document
}

data class FileListDisplayItem(
    val id: Int,
    val name: String,
    val meta: String,
    val sizeLabel: String,
    val iconKind: FileListIconKind,
    val bucketName: String?,
    val path: String?,
)

internal sealed interface FileDetailDisplayPreview {
    data class MediaPreview(
        val item: FileImageDisplayItem,
    ) : FileDetailDisplayPreview

    data class FileIconPreview(
        val kind: FileListIconKind,
    ) : FileDetailDisplayPreview
}

internal data class FileDetailDisplayItem(
    val id: Int,
    val name: String,
    val meta: String,
    val sizeLabel: String,
    val preview: FileDetailDisplayPreview,
)

internal fun FileImageDisplayItem.toFileDetailDisplayItem(): FileDetailDisplayItem =
    FileDetailDisplayItem(
        id = id,
        name = name,
        meta = meta,
        sizeLabel = sizeLabel,
        preview = FileDetailDisplayPreview.MediaPreview(this),
    )

internal fun FileListDisplayItem.toFileDetailDisplayItem(): FileDetailDisplayItem =
    FileDetailDisplayItem(
        id = id,
        name = name,
        meta = meta,
        sizeLabel = sizeLabel,
        preview = FileDetailDisplayPreview.FileIconPreview(iconKind),
    )

internal fun managedFileToImageDisplayItem(
    id: Int,
    file: ManagedFileItem,
    bestPhoto: Boolean = false,
): FileImageDisplayItem =
    FileImageDisplayItem(
        id = id,
        name = file.name,
        meta = managedFileMeta(file),
        sizeLabel = file.formattedSize,
        paletteIndex = id - 1,
        uri = file.uri,
        bucketName = file.bucketName,
        path = file.path,
        bestPhoto = bestPhoto,
    )

internal fun managedFileToListDisplayItem(
    id: Int,
    file: ManagedFileItem,
    iconKind: FileListIconKind,
): FileListDisplayItem =
    FileListDisplayItem(
        id = id,
        name = file.name,
        meta = managedFileMeta(file),
        sizeLabel = file.formattedSize,
        iconKind = iconKind,
        bucketName = file.bucketName,
        path = file.path,
    )

internal fun buildMediaTabs(
    titles: List<String>,
    items: List<FileImageDisplayItem>,
    fileForId: (Int) -> ManagedFileItem?,
): List<FileManagerTabDisplayItem> =
    titles.map { title ->
        val files = filterMediaGridItems(title, items).mapNotNull { fileForId(it.id) }
        FileManagerTabDisplayItem(title = title, sizeLabel = totalSizeLabel(files))
    }

internal fun buildListTabs(
    titles: List<String>,
    items: List<FileListDisplayItem>,
    fileForId: (Int) -> ManagedFileItem?,
): List<FileManagerTabDisplayItem> =
    titles.map { title ->
        val files = filterFileManagerListItems(title, items).mapNotNull { fileForId(it.id) }
        FileManagerTabDisplayItem(title = title, sizeLabel = totalSizeLabel(files))
    }

internal fun formatManagedFileDate(modifiedSeconds: Long): String {
    val millis = if (modifiedSeconds > 9_999_999_999L) modifiedSeconds else modifiedSeconds * 1000L
    return SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).format(Date(millis))
}

internal fun managedFileMeta(file: ManagedFileItem): String =
    "${formatManagedFileDate(file.modifiedSeconds)} ${file.formattedSize}"

internal fun totalSizeLabel(files: List<ManagedFileItem>): String =
    FileSizeFormatter.format(files.sumOf { it.sizeBytes })

internal fun String.splitFileSizeLabel(): Pair<String, String> =
    substringBefore(" ") to substringAfter(" ", "B")
