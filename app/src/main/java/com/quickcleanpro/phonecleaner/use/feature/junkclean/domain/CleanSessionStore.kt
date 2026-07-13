package com.quickcleanpro.phonecleaner.use.feature.junkclean.domain

import com.quickcleanpro.phonecleaner.use.core.model.clean.CleanResult
import com.quickcleanpro.phonecleaner.use.core.model.clean.JunkFile
import com.quickcleanpro.phonecleaner.use.core.model.clean.MemoryCleanResult
import com.quickcleanpro.phonecleaner.use.core.model.clean.ScanProgress
import com.quickcleanpro.phonecleaner.use.core.model.clean.ScanResult
import kotlinx.coroutines.flow.StateFlow

interface CleanSessionStore {
    val scanProgress: StateFlow<ScanProgress>

    val scanResult: StateFlow<ScanResult?>

    val sessionState: StateFlow<ScanSessionState>

    val cleanupSummary: StateFlow<CleanupSummary?>

    val cleanResult: StateFlow<CleanResult?>

    val memoryResult: StateFlow<MemoryCleanResult?>

    fun setScanProgress(value: ScanProgress)

    fun setScanResult(value: ScanResult)

    fun setCleanResult(value: CleanResult)

    fun setMemoryResult(value: MemoryCleanResult)

    fun setCleanupSummary(value: CleanupSummary)

    fun removeCleanedFiles(cleanedFiles: List<JunkFile>)

    fun clearCleanResults()

    fun clear()
}

data class ScanSessionState(
    val progress: ScanProgress = ScanProgress.IDLE,
    val scanResult: ScanResult? = null,
)

data class CleanupSummary(
    val freedSpace: Long,
    val cleanedCount: Int,
    val failedCount: Int,
    val memoryFreedBytes: Long,
    val memoryProcessesKilled: Int,
) {
    val totalFreedBytes: Long
        get() = freedSpace + memoryFreedBytes

    val hasVisibleResult: Boolean
        get() = totalFreedBytes > 0L || cleanedCount > 0 || failedCount > 0 || memoryProcessesKilled > 0
}
