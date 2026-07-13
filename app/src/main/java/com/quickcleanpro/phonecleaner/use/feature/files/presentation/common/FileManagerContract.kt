package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction

enum class FileManagerPhase {
    Scanning,
    Browsing,
    ConfirmDelete,
    Deleting,
    CompleteAnimation,
    Result,
    NoResults,
}

data class FileManagerState(
    val phase: FileManagerPhase = FileManagerPhase.Scanning,
    val selectedKeys: Set<String> = emptySet(),
    val detailIndex: Int? = null,
    val result: FileManagerResult? = null,
    val errorMessage: String? = null,
)

data class FileManagerResult(
    val freedBytes: Long = 0L,
    val changedCount: Int = 0,
)

internal sealed interface FileManagerAction {
    data object ScanStarted : FileManagerAction
    data class ScanFinished(val hasResults: Boolean) : FileManagerAction
    data class ScanFailed(val message: String) : FileManagerAction
    data class SetSelection(val keys: Set<String>) : FileManagerAction
    data class ToggleSelection(val key: String) : FileManagerAction
    data class OpenDetail(val index: Int) : FileManagerAction
    data object CloseDetail : FileManagerAction
    data class RequestOperation(val action: OperationAction) : FileManagerAction
    data object CancelOperation : FileManagerAction
    data class OperationStarted(val action: OperationAction) : FileManagerAction
    data class OperationAnimationFinished(val result: FileManagerResult) : FileManagerAction
    data class OperationFinished(val action: OperationAction, val success: Boolean) : FileManagerAction
    data class OperationFailed(val message: String) : FileManagerAction
    data object CompletionAdDismissed : FileManagerAction
    data object ContinueManaging : FileManagerAction
    data object ClearError : FileManagerAction
    data object ActiveOperationCancelled : FileManagerAction
}

sealed interface FileManagerEffect {
    data class ConfirmOperation(val action: OperationAction) : FileManagerEffect
    data class OperationCompleted(val action: OperationAction) : FileManagerEffect
    data object ResultShown : FileManagerEffect
}

internal data class FileManagerTransition(
    val state: FileManagerState,
    val effects: List<FileManagerEffect> = emptyList(),
)

internal data class FileManagerOperationOutcome(
    val freedBytes: Long = 0L,
    val changedCount: Int = 0,
) {
    fun toResult(): FileManagerResult = FileManagerResult(
        freedBytes = freedBytes,
        changedCount = changedCount,
    )
}
