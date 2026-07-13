package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.ManagedFileItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

internal class FileManagerController(
    private val featureKey: FeatureKey,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    testLoader: (((suspend () -> Unit)) -> Unit)? = null,
) {
    private val mutableState = MutableStateFlow(FileManagerState())
    private val effectsChannel = Channel<FileManagerEffect>(Channel.BUFFERED)
    private val operationEventsChannel = Channel<FeatureOperationEvent>(Channel.BUFFERED)
    private var pendingCompletionResult: (() -> Unit)? = null

    val state: StateFlow<FileManagerState> = mutableState.asStateFlow()
    val effects: Flow<FileManagerEffect> = effectsChannel.receiveAsFlow()
    val operationEvents: Flow<FeatureOperationEvent> = operationEventsChannel.receiveAsFlow()
    val operationRunner = FileOperationRunner(scope, dispatcher, testLoader)

    @Synchronized
    fun dispatch(action: FileManagerAction) {
        val transition = FileManagerReducer.reduce(mutableState.value, action)
        mutableState.value = transition.state
        transition.effects.forEach(effectsChannel::trySend)
        operationEvent(action)?.let(operationEventsChannel::trySend)
    }

    fun cancelActiveOperation(updatePhase: Boolean) {
        pendingCompletionResult = null
        operationRunner.cancelActiveOperation()
        if (updatePhase) dispatch(FileManagerAction.ActiveOperationCancelled)
    }

    fun showResultAfterCompletionAd() {
        val resultAction = pendingCompletionResult ?: return
        pendingCompletionResult = null
        resultAction()
        dispatch(FileManagerAction.CompletionAdDismissed)
    }

    fun runFileOperation(
        selectedFiles: List<ManagedFileItem>,
        action: OperationAction,
        onEmptySelection: () -> Unit,
        onStart: () -> Unit,
        operationDelayMillis: Long,
        completeDelayMillis: Long,
        operation: suspend () -> FileManagerOperationOutcome,
        isSuccessful: (FileManagerOperationOutcome) -> Boolean,
        onCompleteAnimation: suspend (FileManagerOperationOutcome) -> Unit,
        onResult: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        if (selectedFiles.isEmpty()) {
            dispatch(FileManagerAction.CancelOperation)
            onEmptySelection()
            return
        }

        pendingCompletionResult = null
        onStart()
        dispatch(FileManagerAction.OperationStarted(action))
        operationRunner.launch {
            runCatching {
                val outcome = operation()
                if (!isSuccessful(outcome)) error(deletionFailedMessage())
                operationRunner.delayIfNeeded(operationDelayMillis)
                onCompleteAnimation(outcome)
                dispatch(FileManagerAction.OperationAnimationFinished(outcome.toResult()))
                operationRunner.delayIfNeeded(completeDelayMillis)
                pendingCompletionResult = onResult
                dispatch(FileManagerAction.OperationFinished(action, success = true))
            }.onFailure { error ->
                if (error is CancellationException) throw error
                pendingCompletionResult = null
                dispatch(FileManagerAction.OperationFailed(error.message ?: deletionFailedMessage()))
                dispatch(FileManagerAction.OperationFinished(action, success = false))
                onFailure(error)
            }
        }
    }

    private fun operationEvent(action: FileManagerAction): FeatureOperationEvent? = when (action) {
        FileManagerAction.ScanStarted -> FeatureOperationEvent.ScanStarted(featureKey)
        is FileManagerAction.ScanFinished -> FeatureOperationEvent.ScanFinished(featureKey, action.hasResults)
        is FileManagerAction.ScanFailed -> FeatureOperationEvent.ScanFinished(featureKey, hasResult = false)
        is FileManagerAction.RequestOperation -> FeatureOperationEvent.ActionRequested(featureKey, action.action)
        is FileManagerAction.OperationStarted -> FeatureOperationEvent.OperationStarted(featureKey, action.action)
        is FileManagerAction.OperationFinished -> FeatureOperationEvent.OperationFinished(
            featureKey,
            action.action,
            action.success,
        )
        FileManagerAction.CompletionAdDismissed -> FeatureOperationEvent.ResultShown(featureKey)
        else -> null
    }
}
