package com.quickcleanpro.phonecleaner.feature.toolbox.shared.device

import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.StorageInfo
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter
import java.io.File

object StorageDataSource {
    fun formatFileSize(bytes: Long): String = FileSizeFormatter.format(bytes)

    fun getInternalStorageInfo(): StorageInfo {
        val path = Environment.getDataDirectory()
        return getStorageInfo(path)
    }

    fun getExternalStorageInfo(): StorageInfo {
        val dirs = getExternalStorageDirectories()
        if (dirs.isEmpty()) return StorageInfo(0, 0, 0)
        var totalBytes = 0L
        var availableBytes = 0L
        for (dir in dirs) {
            val info = getStorageInfo(dir)
            totalBytes += info.totalBytes
            availableBytes += info.availableBytes
        }
        return StorageInfo(totalBytes, availableBytes, totalBytes - availableBytes)
    }

    fun getTotalUsedStorage(): Long = getInternalStorageInfo().usedBytes + getExternalStorageInfo().usedBytes

    fun getTotalAvailableStorage(): Long = getInternalStorageInfo().availableBytes + getExternalStorageInfo().availableBytes

    fun getExternalStorageDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        val primaryExternal = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && primaryExternal != null) {
            dirs.add(primaryExternal)
        }

        runCatching { File("/storage").listFiles() }
            .getOrNull()
            .orEmpty()
            .forEach { candidate ->
                val primaryPath = primaryExternal?.absolutePath
                if (candidate.isDirectory &&
                    candidate.canRead() &&
                    candidate.absolutePath != primaryPath
                ) {
                    dirs.add(candidate)
                }
            }
        return dirs.distinctBy { it.absolutePath }
    }

    private fun getStorageInfo(path: File): StorageInfo =
        try {
            val stat = StatFs(path.absolutePath)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalBytes = blockSize * totalBlocks
            val availableBytes = blockSize * availableBlocks
            StorageInfo(totalBytes, availableBytes, totalBytes - availableBytes)
        } catch (e: Exception) {
            StorageInfo(0, 0, 0)
        }
}
