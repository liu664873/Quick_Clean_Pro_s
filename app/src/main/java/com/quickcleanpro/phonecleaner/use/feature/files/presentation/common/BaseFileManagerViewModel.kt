package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.ManagedFileItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseFileManagerViewModel(
    protected val featureKey: FeatureKey,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    testLoader: (((suspend () -> Unit)) -> Unit)? = null,
) : ViewModel() {
    protected data class FileOperationOutcome(
        val freedBytes: Long = 0L,
        val changedCount: Int = 0,
    )

    private val controller = FileManagerController(
        featureKey = featureKey,
        scope = viewModelScope,
        dispatcher = ioDispatcher,
        testLoader = testLoader,
    )

    protected val operationRunner: FileOperationRunner get() = controller.operationRunner
    val fileManagerState: StateFlow<FileManagerState> get() = controller.state
    val fileManagerEffects: Flow<FileManagerEffect> get() = controller.effects
    val operationEvents: Flow<FeatureOperationEvent> get() = controller.operationEvents

    /**
     * Cancel any running scan or delete operation without changing UI state.
     */
    fun cancelActiveOperation() {
        controller.cancelActiveOperation(updatePhase = false)
    }

    /**
     * Cancel a running delete and reset the phase back to browsing.
     * Safe to call during any phase 锟?only transitions if currently [FileOperationPhase.Deleting].
     */
    fun cancelDeletingAndReturnToBrowsing() {
        controller.cancelActiveOperation(updatePhase = true)
        onCancelDeletingPhase()
    }

    /**
     * Hook for subclasses to reset their phase from Deleting 锟?Browsing.
     */
    protected abstract fun onCancelDeletingPhase()

    protected fun trackScanStarted() {
        controller.dispatch(FileManagerAction.ScanStarted)
    }

    protected fun trackScanFinished(hasResult: Boolean) {
        controller.dispatch(FileManagerAction.ScanFinished(hasResult))
    }

    protected fun trackActionRequested(action: OperationAction) {
        controller.dispatch(FileManagerAction.RequestOperation(action))
    }

    fun showResultAfterCompletionAd() {
        controller.showResultAfterCompletionAd()
    }

    protected fun runFileOperation(
        selectedFiles: List<ManagedFileItem>,
        action: OperationAction,
        onEmptySelection: () -> Unit,
        onStart: () -> Unit,
        operationDelayMillis: Long,
        completeDelayMillis: Long,
        operation: suspend () -> FileOperationOutcome,
        isSuccessful: (FileOperationOutcome) -> Boolean = { true },
        onCompleteAnimation: suspend (FileOperationOutcome) -> Unit,
        onResult: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        controller.runFileOperation(
            selectedFiles = selectedFiles,
            action = action,
            onEmptySelection = onEmptySelection,
            onStart = onStart,
            operationDelayMillis = operationDelayMillis,
            completeDelayMillis = completeDelayMillis,
            operation = {
                operation().let { FileManagerOperationOutcome(it.freedBytes, it.changedCount) }
            },
            isSuccessful = { outcome ->
                isSuccessful(FileOperationOutcome(outcome.freedBytes, outcome.changedCount))
            },
            onCompleteAnimation = { outcome ->
                onCompleteAnimation(FileOperationOutcome(outcome.freedBytes, outcome.changedCount))
            },
            onResult = onResult,
            onFailure = onFailure,
        )
    }
}
