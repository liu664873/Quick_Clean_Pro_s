package com.quickcleanpro.phonecleaner.feature.files.shared.data.source

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileType
import java.io.File
import java.util.Locale

internal object ManagedFileClassifier {
    fun isDocumentFile(name: String, mimeType: String?): Boolean {
        if (isMediaFile(name, mimeType)) return false
        val extension = name.lowercase(Locale.US).substringAfterLast('.', missingDelimiterValue = "")
        val mime = mimeType.orEmpty().lowercase(Locale.US)
        return extension in DOCUMENT_EXTENSIONS ||
            mime.startsWith("text/") ||
            DOCUMENT_MIME_KEYWORDS.any(mime::contains)
    }

    fun isMediaFile(name: String, mimeType: String?): Boolean {
        val extension = name.lowercase(Locale.US).substringAfterLast('.', missingDelimiterValue = "")
        val mime = mimeType.orEmpty().lowercase(Locale.US)
        return extension in MEDIA_EXTENSIONS ||
            mime.startsWith("image/") ||
            mime.startsWith("video/") ||
            mime.startsWith("audio/")
    }

    fun typeForName(name: String): ManagedFileType =
        when (name.lowercase(Locale.US).substringAfterLast('.', missingDelimiterValue = "")) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> ManagedFileType.Image
            "mp4", "mkv", "mov", "avi", "webm", "3gp" -> ManagedFileType.Video
            "mp3", "wav", "m4a", "aac", "ogg", "flac" -> ManagedFileType.Audio
            else -> ManagedFileType.Document
        }

    fun shouldSkipPath(path: String?, name: String): Boolean {
        val lowerName = name.lowercase(Locale.US)
        val lowerPath = path?.lowercase(Locale.US).orEmpty()
        if (lowerName.startsWith(".") || lowerName == "lost+found") return true
        if (lowerPath.isBlank()) return false
        return SYSTEM_DIRECTORIES.any { lowerPath == it || lowerPath.startsWith("$it/") }
    }

    fun identity(path: String?, uri: String): String =
        path
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { File(it).canonicalPath }.getOrDefault(it) }
            ?: uri

    private val DOCUMENT_EXTENSIONS =
        setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "xml", "json",
            "md", "markdown", "epub", "mobi", "azw", "azw3", "fb2", "zip", "rar", "7z", "tar", "gz",
            "tgz", "bz2", "xz", "odt", "ods", "odp", "ott", "ots", "otp", "wps", "et", "dps", "pages",
            "numbers", "key", "xps", "log", "ini", "conf", "cfg", "yaml", "yml", "toml", "properties", "sql",
            "db", "sqlite", "sqlite3", "kt", "kts", "java", "js", "ts", "html", "htm", "css", "py", "sh",
            "bat", "gradle", "c", "cpp", "h", "hpp", "cs", "go", "rs", "php", "rb", "swift",
        )

    private val DOCUMENT_MIME_KEYWORDS =
        listOf(
            "pdf", "document", "spreadsheet", "presentation", "msword", "officedocument", "opendocument", "word",
            "excel", "powerpoint", "wps", "csv", "rtf", "epub", "ebook", "markdown", "javascript", "xhtml",
            "html", "yaml", "json", "xml", "sqlite", "database", "archive", "compressed", "gzip", "tar", "zip",
            "rar", "7z",
        )

    private val MEDIA_EXTENSIONS =
        setOf(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "tif", "tiff", "svg", "mp4", "mkv",
            "mov", "avi", "webm", "3gp", "m4v", "wmv", "flv", "mpeg", "mpg", "mp3", "wav", "m4a", "aac",
            "ogg", "oga", "flac", "amr", "wma", "opus",
        )

    private val SYSTEM_DIRECTORIES =
        listOf(
            "/system", "/lost+found", "/preload", "/vendor", "/mnt", "/proc", "/sys", "/acct", "/dev",
            "/config", "/oem", "/firmware", "/cache",
        )
}
