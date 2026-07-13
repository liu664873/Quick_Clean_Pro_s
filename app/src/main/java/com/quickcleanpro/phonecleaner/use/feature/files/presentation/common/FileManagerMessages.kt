package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

import com.quickcleanpro.phonecleaner.R

internal const val FILE_DELETE_ANIMATION_MIN_MILLIS = 2000L

internal fun appString(resId: Int): String = fallbackString(resId)

internal fun fileScanFailedMessage(): String =
    runCatching { appString(R.string.file_scan_failed) }.getOrDefault("File scan failed.")

internal fun duplicateScanFailedMessage(): String =
    runCatching { appString(R.string.duplicate_scan_failed) }.getOrDefault("Duplicate file scan failed.")

internal fun deletionFailedMessage(): String =
    runCatching { appString(R.string.deletion_failed) }.getOrDefault("Deletion failed.")

private fun fallbackString(resId: Int): String =
    when (resId) {
        R.string.file_scan_failed -> "File scan failed."
        R.string.duplicate_scan_failed -> "Duplicate file scan failed."
        R.string.deletion_failed -> "Deletion failed."
        else -> ""
    }
