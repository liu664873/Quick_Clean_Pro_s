package com.quickcleanpro.phonecleaner.feature.junkclean

import com.quickcleanpro.phonecleaner.feature.junkclean.model.CleanItem
import com.quickcleanpro.phonecleaner.feature.junkclean.model.CleanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanProgress
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.MemoryCleanResult
import kotlinx.coroutines.flow.Flow

/**
 * Core clean capability contract.
 *
 * Domain callers depend on this abstraction rather than concrete scanner or
 * delete implementations.
 */
interface CleanRepository {
    val scanProgress: Flow<ScanProgress>

    suspend fun performFullScan(): ScanResult

    suspend fun cleanFiles(selectedItems: List<CleanItem>): CleanResult

    suspend fun cleanMemory(): MemoryCleanResult

    suspend fun deleteFileItems(selectedItems: List<CleanItem>): List<JunkDeleteOutcome>

    suspend fun finalizeAuthorizedDeletes(pendingOutcomes: List<JunkDeleteOutcome>): JunkAuthorizedDeleteResult
}
