package com.quickcleanpro.phonecleaner.feature.files.shared

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class FileManagerFlowState(
    val showStopDialog: Boolean = false,
    val completionAdInFlight: Boolean = false,
    val blockedPhase: FileOperationPhase? = null,
)

internal abstract class BaseFileManagerViewModel(
    protected val featureKey: FeatureKey,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    testLoader: (((suspend () -> Unit)) -> Unit)? = null,
) : ViewModel() {
    protected data class FileOperationOutcome(
        val freedBytes: Long = 0L,
        val changedCount: Int = 0,
    )

    private val executor = FileOperationExecutor(
        featureKey = featureKey,
        scope = viewModelScope,
        dispatcher = ioDispatcher,
        testLoader = testLoader,
    )

    protected val operationRunner: FileOperationExecutor get() = executor
    val operationEvents: Flow<FeatureOperationEvent> get() = executor.operationEvents
    private val _flowState = MutableStateFlow(FileManagerFlowState())
    val flowState: StateFlow<FileManagerFlowState> = _flowState.asStateFlow()

    fun showStopDialog(blockedPhase: FileOperationPhase? = null) {
        _flowState.update { it.copy(showStopDialog = true, blockedPhase = blockedPhase) }
    }

    fun dismissStopDialog() {
        _flowState.update { it.copy(showStopDialog = false, blockedPhase = null) }
    }

    fun setCompletionAdInFlight(active: Boolean) {
        _flowState.update { it.copy(completionAdInFlight = active) }
    }

    /**
     * Cancel any running scan or delete operation without changing UI state.
     */
    fun cancelActiveOperation() {
        executor.cancel()
        dismissStopDialog()
    }

    /**
     * Cancel a running delete and reset the phase back to browsing.
     * Safe to call during any phase 闁?only transitions if currently [FileOperationPhase.Deleting].
     */
    fun cancelDeletingAndReturnToBrowsing() {
        executor.cancel()
        onCancelDeletingPhase()
    }

    /**
     * Hook for subclasses to reset their phase from Deleting 闁?Browsing.
     */
    protected abstract fun onCancelDeletingPhase()

    protected fun trackScanStarted() {
        executor.trackScanStarted()
    }

    protected fun trackScanFinished(hasResult: Boolean) {
        executor.trackScanFinished(hasResult)
    }

    fun showResultAfterCompletionAd() {
        executor.showPendingResult()
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
        executor.run(
            selectedFiles = selectedFiles,
            action = action,
            onEmptySelection = onEmptySelection,
            onStart = onStart,
            operationDelayMillis = operationDelayMillis,
            completeDelayMillis = completeDelayMillis,
            operation = {
                operation().let { FileExecutionOutcome(it.freedBytes, it.changedCount) }
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
