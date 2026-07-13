package com.quickcleanpro.phonecleaner.feature.junkclean.ui

import com.quickcleanpro.phonecleaner.feature.junkclean.logic.*

import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.junkclean.logic.model.JunkCategory
import com.quickcleanpro.phonecleaner.feature.junkclean.logic.model.JunkFile
import com.quickcleanpro.phonecleaner.feature.junkclean.logic.model.ScanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.logic.CleanupSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JunkCleanReducerTest {
    @Test
    fun `preview selects scanned files and tracks empty display categories`() {
        val result = scanResult(file("cache", 120L, JunkCategory.CACHE))

        val state = JunkCleanReducer.reduce(JunkCleanUiState(), JunkCleanAction.PreviewLoaded(result))

        assertEquals(JunkCleanPhase.Preview, state.phase)
        assertEquals(1, state.selectedSummary.checkedCount)
        assertEquals(120L, state.selectedSummary.checkedSize)
        assertTrue(state.groups.single().items.single().isChecked)
        assertFalse(JunkCategory.CACHE in state.checkedEmptyCategories)
        assertTrue(JunkCategory.TEMP_FILE in state.checkedEmptyCategories)
    }

    @Test
    fun `item and category toggles update one selection summary`() {
        val preview =
            JunkCleanReducer.reduce(
                JunkCleanUiState(),
                JunkCleanAction.PreviewLoaded(
                    scanResult(
                        file("cache-1", 100L, JunkCategory.CACHE),
                        file("cache-2", 200L, JunkCategory.CACHE),
                    ),
                ),
            )

        val oneUnchecked = JunkCleanReducer.reduce(preview, JunkCleanAction.ToggleItem("cache-1"))
        val allChecked =
            JunkCleanReducer.reduce(
                oneUnchecked,
                JunkCleanAction.ToggleCategories(listOf(JunkCategory.CACHE)),
            )
        val allUnchecked =
            JunkCleanReducer.reduce(
                allChecked,
                JunkCleanAction.ToggleCategories(listOf(JunkCategory.CACHE)),
            )

        assertEquals(1, oneUnchecked.selectedSummary.checkedCount)
        assertEquals(200L, oneUnchecked.selectedSummary.checkedSize)
        assertEquals(2, allChecked.selectedSummary.checkedCount)
        assertEquals(0, allUnchecked.selectedSummary.checkedCount)
        assertTrue(allUnchecked.groups.single().items.none { it.isChecked })
    }

    @Test
    fun `empty category toggles independently and changes cleaning validation`() {
        val preview =
            JunkCleanReducer.reduce(
                JunkCleanUiState(),
                JunkCleanAction.PreviewLoaded(ScanResult.EMPTY),
            )
        val emptyCategoriesCleared =
            DefaultResultDisplayCategories.fold(preview) { state, category ->
                JunkCleanReducer.reduce(state, JunkCleanAction.ToggleCategories(listOf(category)))
            }

        val zeroByteFailure = JunkCleanReducer.reduce(preview, JunkCleanAction.CleaningRequested)
        val noSelectionFailure = JunkCleanReducer.reduce(emptyCategoriesCleared, JunkCleanAction.CleaningRequested)

        assertEquals(R.string.result_zero_byte_selection_hint, zeroByteFailure.errorMessageRes)
        assertEquals(R.string.result_select_at_least_one, noSelectionFailure.errorMessageRes)
    }

    @Test
    fun `cleaning follows authorization animation and complete phases`() {
        val preview =
            JunkCleanReducer.reduce(
                JunkCleanUiState(),
                JunkCleanAction.PreviewLoaded(scanResult(file("cache", 100L, JunkCategory.CACHE))),
            )
        val cleaning = JunkCleanReducer.reduce(preview, JunkCleanAction.CleaningRequested)
        val authorization =
            JunkCleanReducer.reduce(cleaning, JunkCleanAction.DeleteAuthorizationRequested("confirm"))
        val resumed = JunkCleanReducer.reduce(authorization, JunkCleanAction.DeleteAuthorizationHandled)
        val animation =
            JunkCleanReducer.reduce(
                resumed,
                JunkCleanAction.CleaningCompleted(
                    CleanupSummary(
                        freedSpace = 100L,
                        cleanedCount = 1,
                        failedCount = 0,
                        memoryFreedBytes = 20L,
                        memoryProcessesKilled = 1,
                    ),
                ),
            )
        val complete = JunkCleanReducer.reduce(animation, JunkCleanAction.CompletionAdDismissed)

        assertEquals(JunkCleanPhase.Cleaning, cleaning.phase)
        assertEquals(JunkCleanPhase.AwaitingAuthorization, authorization.phase)
        assertEquals("confirm", authorization.awaitingAuthorizationMessage)
        assertEquals(JunkCleanPhase.Cleaning, resumed.phase)
        assertNull(resumed.awaitingAuthorizationMessage)
        assertEquals(JunkCleanPhase.CompleteAnimation, animation.phase)
        assertEquals(120L, animation.cleanResult.totalFreedBytes)
        assertTrue(animation.cleanResult.hasVisibleResult)
        assertEquals(JunkCleanPhase.Complete, complete.phase)
    }

    @Test
    fun `phase guarded actions do not mutate unrelated states`() {
        val scanning = JunkCleanUiState(phase = JunkCleanPhase.Scanning)

        assertSame(scanning, JunkCleanReducer.reduce(scanning, JunkCleanAction.ToggleItem("missing")))
        assertSame(scanning, JunkCleanReducer.reduce(scanning, JunkCleanAction.CleaningRequested))
        assertSame(scanning, JunkCleanReducer.reduce(scanning, JunkCleanAction.CleaningCancelled))
    }

    @Test
    fun `scan failure records scan state while cleaning failure preserves it`() {
        val scanFailure =
            JunkCleanReducer.reduce(
                JunkCleanUiState(scanState = JunkCleanScanState.Scanning),
                JunkCleanAction.Failed(R.string.scan_failed, "scan", duringScan = true),
            )
        val cleanFailure =
            JunkCleanReducer.reduce(
                JunkCleanUiState(scanState = JunkCleanScanState.Completed),
                JunkCleanAction.Failed(R.string.result_clean_error, "clean"),
            )

        assertEquals(JunkCleanScanState.Error, scanFailure.scanState)
        assertEquals("scan", scanFailure.errorMessage)
        assertEquals(JunkCleanScanState.Completed, cleanFailure.scanState)
        assertEquals("clean", cleanFailure.errorMessage)
    }

    private fun scanResult(vararg files: JunkFile): ScanResult {
        val items = files.toList()
        return ScanResult(
            junkFiles = items,
            totalSize = items.sumOf { it.fileSize },
            totalCount = items.size,
            categoryBreakdown = items.groupBy { it.category },
        )
    }

    private fun file(id: String, size: Long, category: JunkCategory) =
        JunkFile(
            id = id,
            filePath = "/$id",
            fileName = id,
            fileSize = size,
            category = category,
            lastModified = 0L,
        )
}
