package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileManagerReducerTest {
    @Test
    fun `scan result chooses browsing or no results`() {
        val browsing = FileManagerReducer.reduce(
            FileManagerState(),
            FileManagerAction.ScanFinished(hasResults = true),
        ).state
        val empty = FileManagerReducer.reduce(
            FileManagerState(),
            FileManagerAction.ScanFinished(hasResults = false),
        ).state

        assertEquals(FileManagerPhase.Browsing, browsing.phase)
        assertEquals(FileManagerPhase.NoResults, empty.phase)
    }

    @Test
    fun `operation follows confirmation animation and result phases`() {
        val selected = FileManagerState(
            phase = FileManagerPhase.Browsing,
            selectedKeys = setOf("file-1"),
        )
        val confirmation = FileManagerReducer.reduce(
            selected,
            FileManagerAction.RequestOperation(OperationAction.DELETE),
        )
        val deleting = FileManagerReducer.reduce(
            confirmation.state,
            FileManagerAction.OperationStarted(OperationAction.DELETE),
        ).state
        val animation = FileManagerReducer.reduce(
            deleting,
            FileManagerAction.OperationAnimationFinished(FileManagerResult(freedBytes = 42L)),
        ).state
        val result = FileManagerReducer.reduce(animation, FileManagerAction.CompletionAdDismissed)

        assertEquals(
            listOf(FileManagerEffect.ConfirmOperation(OperationAction.DELETE)),
            confirmation.effects,
        )
        assertEquals(FileManagerPhase.Deleting, deleting.phase)
        assertEquals(FileManagerPhase.CompleteAnimation, animation.phase)
        assertEquals(emptySet<String>(), animation.selectedKeys)
        assertEquals(42L, animation.result?.freedBytes)
        assertEquals(FileManagerPhase.Result, result.state.phase)
        assertEquals(listOf(FileManagerEffect.ResultShown), result.effects)
    }

    @Test
    fun `cancellation only exits deleting phase`() {
        val browsing = FileManagerState(phase = FileManagerPhase.Browsing)
        val unchanged = FileManagerReducer.reduce(
            browsing,
            FileManagerAction.ActiveOperationCancelled,
        ).state
        val cancelled = FileManagerReducer.reduce(
            FileManagerState(phase = FileManagerPhase.Deleting),
            FileManagerAction.ActiveOperationCancelled,
        ).state

        assertEquals(browsing, unchanged)
        assertEquals(FileManagerPhase.Browsing, cancelled.phase)
    }

    @Test
    fun `failure is visible and clearable`() {
        val failed = FileManagerReducer.reduce(
            FileManagerState(phase = FileManagerPhase.Deleting),
            FileManagerAction.OperationFailed("delete failed"),
        ).state
        val cleared = FileManagerReducer.reduce(failed, FileManagerAction.ClearError).state

        assertEquals(FileManagerPhase.Browsing, failed.phase)
        assertEquals("delete failed", failed.errorMessage)
        assertNull(cleared.errorMessage)
    }
}
