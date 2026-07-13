package com.quickcleanpro.phonecleaner.feature.junkclean.scanner

import com.quickcleanpro.phonecleaner.feature.junkclean.model.CleanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkFile
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanProgress
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.MemoryCleanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanSessionStore
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanupSummary
import com.quickcleanpro.phonecleaner.feature.junkclean.ScanSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared state for the main clean flow.
 *
 * This object stores scan and clean facts that must survive cross-screen flows.
 * UI display state, animation state, and navigation remain outside this module.
 */
class SharedScanState : CleanSessionStore {
    private val _scanProgress = MutableStateFlow(ScanProgress.IDLE)
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    private val _sessionState = MutableStateFlow(ScanSessionState())
    private val _cleanupSummary = MutableStateFlow<CleanupSummary?>(null)
    private val _cleanResult = MutableStateFlow<CleanResult?>(null)
    private val _memoryResult = MutableStateFlow<MemoryCleanResult?>(null)

    override val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    override val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    override val sessionState: StateFlow<ScanSessionState> = _sessionState.asStateFlow()

    override val cleanupSummary: StateFlow<CleanupSummary?> = _cleanupSummary.asStateFlow()

    override val cleanResult: StateFlow<CleanResult?> = _cleanResult.asStateFlow()

    override val memoryResult: StateFlow<MemoryCleanResult?> = _memoryResult.asStateFlow()

    override fun setScanProgress(value: ScanProgress) {
        _scanProgress.value = value
        _sessionState.value =
            _sessionState.value.copy(
                progress = value,
                scanResult = _scanResult.value,
            )
    }

    override fun setScanResult(value: ScanResult) {
        _scanResult.value = value
        _sessionState.value =
            _sessionState.value.copy(
                progress = _scanProgress.value,
                scanResult = value,
            )
    }

    override fun setCleanResult(value: CleanResult) {
        _cleanResult.value = value
    }

    override fun setMemoryResult(value: MemoryCleanResult) {
        _memoryResult.value = value
    }

    override fun setCleanupSummary(value: CleanupSummary) {
        _cleanupSummary.value = value
    }

    override fun removeCleanedFiles(cleanedFiles: List<JunkFile>) {
        val current = _scanResult.value ?: return
        if (cleanedFiles.isEmpty()) return

        val cleanedPaths = cleanedFiles.map { it.filePath }.toSet()
        val remainingFiles = current.junkFiles.filterNot { it.filePath in cleanedPaths }
        val remainingSize = remainingFiles.sumOf { it.fileSize }
        val updatedResult =
            current.copy(
                junkFiles = remainingFiles,
                totalSize = remainingSize,
                totalCount = remainingFiles.size,
                categoryBreakdown = remainingFiles.groupBy { it.category },
            )

        _scanResult.value = updatedResult
        _sessionState.value =
            _sessionState.value.copy(
                progress =
                    if (remainingFiles.isEmpty()) {
                        ScanProgress.IDLE
                    } else {
                        ScanProgress(
                            percent = 100f,
                            foundCount = remainingFiles.size,
                            foundSize = remainingSize,
                        )
                    },
                scanResult = updatedResult,
            )
        _scanProgress.value = _sessionState.value.progress
    }

    override fun clearCleanResults() {
        _cleanResult.value = null
        _memoryResult.value = null
        _cleanupSummary.value = null
    }

    override fun clear() {
        _scanProgress.value = ScanProgress.IDLE
        _scanResult.value = null
        _sessionState.value = ScanSessionState()
        _cleanupSummary.value = null
        _cleanResult.value = null
        _memoryResult.value = null
    }
}
