package com.quickcleanpro.phonecleaner.feature.files.shared.data.source

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import android.net.Uri
import android.os.Environment
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import java.io.File

internal object DirectoryFileScanner {
    fun scanPublicFiles(checkCancellation: () -> Unit = {}): List<ManagedFileItem> {
        val root = externalStorageDirectoryOrNull() ?: return emptyList()
        if (!root.exists() || !root.isDirectory) return emptyList()

        val results = mutableListOf<ManagedFileItem>()
        collectFiles(root, results, maxDepth = 1, limit = MAX_FILE_SYSTEM_RESULTS, checkCancellation = checkCancellation)
        readableDirectories(root, PUBLIC_SCAN_DIRECTORIES, checkCancellation).forEach { directory ->
            collectFiles(directory, results, maxDepth = 12, limit = MAX_FILE_SYSTEM_RESULTS, checkCancellation = checkCancellation)
        }
        return results.distinctBy(::identity)
    }

    fun scanDocumentFiles(checkCancellation: () -> Unit = {}): List<ManagedFileItem> {
        val root = externalStorageDirectoryOrNull() ?: return emptyList()
        if (!root.exists() || !root.isDirectory) return emptyList()

        val results = mutableListOf<ManagedFileItem>()
        collectFiles(
            directory = root,
            results = results,
            maxDepth = 2,
            limit = MAX_DOCUMENT_FILE_SYSTEM_RESULTS,
            accept = { ManagedFileClassifier.isDocumentFile(it.name, null) },
            checkCancellation = checkCancellation,
        )
        readableDirectories(root, DOCUMENT_SCAN_DIRECTORIES, checkCancellation).forEach { directory ->
            collectFiles(
                directory = directory,
                results = results,
                maxDepth = 16,
                limit = MAX_DOCUMENT_FILE_SYSTEM_RESULTS,
                accept = { ManagedFileClassifier.isDocumentFile(it.name, null) },
                checkCancellation = checkCancellation,
            )
        }
        return results.distinctBy(::identity)
    }

    fun scanWhatsAppFiles(checkCancellation: () -> Unit = {}): List<ManagedFileItem> {
        val root = externalStorageDirectoryOrNull() ?: return emptyList()
        if (!root.exists() || !root.isDirectory) return emptyList()

        val results = mutableListOf<ManagedFileItem>()
        readableDirectories(root, WHATSAPP_SCAN_DIRECTORIES, checkCancellation).forEach { directory ->
            collectFiles(
                directory = directory,
                results = results,
                maxDepth = 18,
                limit = MAX_WHATSAPP_FILE_SYSTEM_RESULTS,
                checkCancellation = checkCancellation,
            )
        }
        return results.distinctBy(::identity)
    }

    private fun readableDirectories(root: File, paths: List<String>, checkCancellation: () -> Unit): List<File> =
        paths
            .map { path ->
                checkCancellation()
                File(root, path)
            }
            .filter { it.exists() && it.isDirectory && it.canRead() }
            .distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }

    private fun collectFiles(
        directory: File,
        results: MutableList<ManagedFileItem>,
        maxDepth: Int,
        limit: Int,
        accept: (File) -> Boolean = { true },
        checkCancellation: () -> Unit,
    ) {
        checkCancellation()
        if (maxDepth < 0 || results.size >= limit) return
        val children = runCatching { directory.listFiles() }.getOrNull().orEmpty()
        for (file in children) {
            checkCancellation()
            if (results.size >= limit) return
            if (ManagedFileClassifier.shouldSkipPath(file.absolutePath, file.name)) continue
            if (file.isDirectory) {
                collectFiles(file, results, maxDepth - 1, limit, accept, checkCancellation)
            } else if (file.isFile && file.length() > 0L && accept(file)) {
                results += file.toManagedFileItem()
            }
        }
    }

    private fun File.toManagedFileItem(): ManagedFileItem =
        ManagedFileItem(
            id = absolutePath.hashCode().toLong(),
            uri = Uri.fromFile(this).toFileUri(),
            path = absolutePath,
            name = name,
            sizeBytes = length(),
            modifiedSeconds = lastModified(),
            mimeType = null,
            bucketName = parentFile?.name,
            type = ManagedFileClassifier.typeForName(name),
        )

    private fun identity(item: ManagedFileItem): String = ManagedFileClassifier.identity(item.path, item.uri.value)

    private fun externalStorageDirectoryOrNull(): File? =
        runCatching { Environment.getExternalStorageDirectory() }.getOrNull()

    private val PUBLIC_SCAN_DIRECTORIES =
        listOf("Download", "Downloads", "Documents", "DCIM", "Pictures", "Movies", "Music", "WhatsApp", "Telegram", "Android/media")

    private val DOCUMENT_SCAN_DIRECTORIES =
        listOf(
            "Download", "Downloads", "Documents", "Desktop", "Android/media", "Android/data", "WhatsApp",
            "WhatsApp/Media/WhatsApp Documents", "Telegram", "Telegram/Telegram Documents", "Tencent",
            "Tencent/QQfile_recv", "Tencent/MicroMsg", "WeChat", "DingTalk", "Feishu", "Lark", "BaiduNetdisk",
            "UCDownloads", "Browser", "Chrome", "QQBrowser", "WPS", "Kingsoft", "Office", "Documents/WeChat Files",
        )

    private val WHATSAPP_SCAN_DIRECTORIES =
        listOf(
            "WhatsApp", "WhatsApp Business", "Android/media/com.whatsapp/WhatsApp",
            "Android/media/com.whatsapp.w4b/WhatsApp Business", "Android/media/com.whatsapp.w4b/WhatsApp",
            "Android/media/com.gbwhatsapp/GBWhatsApp", "Android/media/com.yowhatsapp/YoWhatsApp",
            "Android/media/com.fmwhatsapp/FMWhatsApp",
        )

    private const val MAX_FILE_SYSTEM_RESULTS = 10_000
    private const val MAX_DOCUMENT_FILE_SYSTEM_RESULTS = 30_000
    private const val MAX_WHATSAPP_FILE_SYSTEM_RESULTS = 30_000
}
