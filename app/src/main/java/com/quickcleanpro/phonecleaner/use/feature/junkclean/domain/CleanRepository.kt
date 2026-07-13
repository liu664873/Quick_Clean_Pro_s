package com.quickcleanpro.phonecleaner.use.feature.junkclean.domain

import com.quickcleanpro.phonecleaner.use.core.model.clean.CleanItem
import com.quickcleanpro.phonecleaner.use.core.model.clean.CleanResult
import com.quickcleanpro.phonecleaner.use.core.model.clean.ScanProgress
import com.quickcleanpro.phonecleaner.use.core.model.clean.ScanResult
import com.quickcleanpro.phonecleaner.use.core.model.clean.MemoryCleanResult
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
