package com.quickcleanpro.phonecleaner.feature.junkclean

import android.app.PendingIntent
import androidx.annotation.StringRes
import com.quickcleanpro.phonecleaner.feature.junkclean.model.CategoryCleanGroup
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkCategory
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanupSummary

enum class JunkCleanPhase {
    Scanning,
    Preview,
    Cleaning,
    AwaitingAuthorization,
    CompleteAnimation,
    Complete,
    Error,
}

enum class JunkCleanScanState {
    Idle,
    Scanning,
    Completed,
    Error,
}

data class JunkCleanResultUiState(
    val freedSpace: Long = 0L,
    val cleanedCount: Int = 0,
    val failedCount: Int = 0,
    val memoryFreedBytes: Long = 0L,
    val memoryProcessesKilled: Int = 0,
    val totalFreedBytes: Long = 0L,
    val formattedFreedSpace: String = "",
    val hasVisibleResult: Boolean = false,
)

data class SelectionSummary(
    val checkedCount: Int = 0,
    val checkedSize: Long = 0L,
    val checkedEmptyCategoryCount: Int = 0,
)

data class JunkCleanUiState(
    val phase: JunkCleanPhase = JunkCleanPhase.Scanning,
    val scanState: JunkCleanScanState = JunkCleanScanState.Idle,
    val progress: Float = 0f,
    val currentCategory: JunkCategory? = null,
    val foundItemCount: Int = 0,
    val foundTotalSize: Long = 0L,
    val formattedFoundSize: String = "0 B",
    val groups: List<CategoryCleanGroup> = emptyList(),
    val checkedEmptyCategories: Set<JunkCategory> = emptySet(),
    val selectedSummary: SelectionSummary = SelectionSummary(),
    val cleanResult: JunkCleanResultUiState = JunkCleanResultUiState(),
    val awaitingAuthorizationMessage: String? = null,
    @StringRes val errorMessageRes: Int? = null,
    val errorMessage: String? = null,
    val showStopDialog: Boolean = false,
    val stopDialogPhase: JunkCleanPhase? = null,
)

sealed interface JunkCleanAction {
    data object ScanStarted : JunkCleanAction

    data class ScanProgressUpdated(
        val progress: Float,
        val currentCategory: JunkCategory?,
        val foundItemCount: Int,
        val foundTotalSize: Long,
    ) : JunkCleanAction

    data class ScanCompleted(val result: ScanResult) : JunkCleanAction
    data class PreviewLoaded(val result: ScanResult) : JunkCleanAction
    data object PreviewUnavailable : JunkCleanAction
    data class ToggleItem(val itemId: String) : JunkCleanAction
    data class ToggleCategories(val categories: List<JunkCategory>) : JunkCleanAction
    data object CleaningRequested : JunkCleanAction
    data class DeleteAuthorizationRequested(val message: String) : JunkCleanAction
    data object DeleteAuthorizationHandled : JunkCleanAction
    data class CleaningCompleted(val summary: CleanupSummary) : JunkCleanAction
    data class Failed(@StringRes val messageRes: Int, val message: String?, val duringScan: Boolean = false) : JunkCleanAction
    data object ResultCleared : JunkCleanAction
    data object CompletionAdDismissed : JunkCleanAction
    data object ActiveOperationCancelled : JunkCleanAction
    data object CleaningCancelled : JunkCleanAction
    data class StopDialogShown(val phase: JunkCleanPhase) : JunkCleanAction
    data object StopDialogDismissed : JunkCleanAction
}

sealed interface JunkCleanUiAction {
    data object Back : JunkCleanUiAction
    data object CleanSelected : JunkCleanUiAction
    data class ToggleItem(val itemId: String) : JunkCleanUiAction
    data class ToggleCategories(val categories: List<JunkCategory>) : JunkCleanUiAction
    data object ContinueFromResult : JunkCleanUiAction
    data object QuitStoppedOperation : JunkCleanUiAction
    data object ResumeStoppedOperation : JunkCleanUiAction
}

sealed interface JunkCleanEvent {
    data class RequestDeleteAuthorization(val deleteRequest: PendingIntent) : JunkCleanEvent
}
