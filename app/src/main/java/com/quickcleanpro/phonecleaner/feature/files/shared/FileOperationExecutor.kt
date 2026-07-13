package com.quickcleanpro.phonecleaner.feature.files.shared

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal data class FileExecutionOutcome(
    val freedBytes: Long = 0L,
    val changedCount: Int = 0,
)

internal class FileOperationExecutor(
    private val featureKey: FeatureKey,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val testLoader: (((suspend () -> Unit)) -> Unit)? = null,
) {
    private val events = Channel<FeatureOperationEvent>(Channel.BUFFERED)
    private var activeJob: Job? = null
    private var pendingResult: (() -> Unit)? = null

    val operationEvents: Flow<FeatureOperationEvent> = events.receiveAsFlow()

    fun trackScanStarted() = emit(FeatureOperationEvent.ScanStarted(featureKey))

    fun trackScanFinished(hasResult: Boolean) =
        emit(FeatureOperationEvent.ScanFinished(featureKey, hasResult))

    fun launch(block: suspend () -> Unit) {
        cancel()
        val loader = testLoader
        if (loader != null) {
            loader(block)
            return
        }
        activeJob = scope.launch(dispatcher) {
            try {
                block()
            } finally {
                if (activeJob == coroutineContext[Job]) activeJob = null
            }
        }
    }

    suspend fun delayIfNeeded(millis: Long) {
        if (millis > 0L) delay(millis)
    }

    fun cancel() {
        pendingResult = null
        activeJob?.cancel()
        activeJob = null
    }

    fun showPendingResult() {
        val action = pendingResult ?: return
        pendingResult = null
        action()
    }

    fun run(
        selectedFiles: List<ManagedFileItem>,
        action: OperationAction,
        onEmptySelection: () -> Unit,
        onStart: () -> Unit,
        operationDelayMillis: Long,
        completeDelayMillis: Long,
        operation: suspend () -> FileExecutionOutcome,
        isSuccessful: (FileExecutionOutcome) -> Boolean,
        onCompleteAnimation: suspend (FileExecutionOutcome) -> Unit,
        onResult: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        if (selectedFiles.isEmpty()) {
            onEmptySelection()
            return
        }
        pendingResult = null
        onStart()
        emit(FeatureOperationEvent.OperationStarted(featureKey, action))
        launch {
            runCatching {
                val outcome = operation()
                if (!isSuccessful(outcome)) error(deletionFailedMessage())
                delayIfNeeded(operationDelayMillis)
                onCompleteAnimation(outcome)
                delayIfNeeded(completeDelayMillis)
                pendingResult = onResult
                emit(FeatureOperationEvent.OperationFinished(featureKey, action, success = true))
            }.onFailure { error ->
                if (error is CancellationException) throw error
                pendingResult = null
                emit(FeatureOperationEvent.OperationFinished(featureKey, action, success = false))
                onFailure(error)
            }
        }
    }

    private fun emit(event: FeatureOperationEvent) {
        events.trySend(event)
    }
}
