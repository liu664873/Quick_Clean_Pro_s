package com.quickcleanpro.phonecleaner.feature.junkclean

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.junkclean.model.CleanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkCategory
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkFile
import com.quickcleanpro.phonecleaner.feature.junkclean.model.ScanResult
import com.quickcleanpro.phonecleaner.feature.junkclean.model.MemoryCleanResult
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanSessionStore
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanupSummary
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanRepository
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkAuthorizedDeleteResult
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkDeleteOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SCAN_PREVIEW_MIN_MILLIS = 1_000L
private const val CLEANING_ANIMATION_MIN_MILLIS = 1_600L

class JunkCleanViewModel(
    private val cleanRepository: CleanRepository,
    private val sharedState: CleanSessionStore,
    private val ioDispatcher: CoroutineDispatcher,
    private val scanPreviewMinMillis: Long = SCAN_PREVIEW_MIN_MILLIS,
    private val cleaningAnimationMinMillis: Long = CLEANING_ANIMATION_MIN_MILLIS,
) : ViewModel() {
    private data class CleaningExecutionState(
        val pendingAuthorizationOutcomes: List<JunkDeleteOutcome> = emptyList(),
        val directCleanedFiles: List<JunkFile> = emptyList(),
        val directFailedFiles: List<JunkFile> = emptyList(),
        val directFreedSpace: Long = 0L,
        val memoryResult: MemoryCleanResult? = null,
    )

    private val FINISHING_ANIMATION_MILLIS = 800L

    private val _uiState = MutableStateFlow(JunkCleanUiState())
    val uiState: StateFlow<JunkCleanUiState> = _uiState.asStateFlow()

    private val eventsChannel = Channel<JunkCleanEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private val operationEventsChannel = Channel<FeatureOperationEvent>(Channel.BUFFERED)
    val operationEvents = operationEventsChannel.receiveAsFlow()

    private fun trackOperationEvent(event: FeatureOperationEvent) {
        operationEventsChannel.trySend(event)
    }

    private var progressJob: Job? = null
    private var scanJob: Job? = null
    private var cleaningJob: Job? = null
    private var hasStarted = false
    private var cleaningExecutionState = CleaningExecutionState()

    private fun dispatch(action: JunkCleanAction) {
        _uiState.value = JunkCleanReducer.reduce(_uiState.value, action)
    }

    fun startScanIfNeeded() {
        if (hasStarted) return
        startScanInternal(resetSession = true)
    }

    fun retryScan() {
        hasStarted = false
        startScanInternal(resetSession = true)
    }

    fun toggleItemSelection(itemId: String) {
        val previous = _uiState.value
        dispatch(JunkCleanAction.ToggleItem(itemId))
        if (_uiState.value !== previous) trackPreviewPublished()
    }

    fun toggleCategorySelection(categories: List<JunkCategory>) {
        val previous = _uiState.value
        dispatch(JunkCleanAction.ToggleCategories(categories))
        if (_uiState.value !== previous) trackPreviewPublished()
    }

    fun startCleaning(context: Context) {
        val appContext = context.applicationContext ?: context
        val requestContext = context
        val state = _uiState.value.takeIf { it.phase == JunkCleanPhase.Preview } ?: return
        val selectedItems = state.groups.flatMap { it.items }.filter { it.isChecked }
        dispatch(JunkCleanAction.CleaningRequested)
        if (_uiState.value.phase != JunkCleanPhase.Cleaning) return

        trackOperationEvent(FeatureOperationEvent.OperationStarted(FeatureKey.JUNK_CLEAN, OperationAction.CLEAN))
        val cleaningStartedAt = System.currentTimeMillis()
        cleaningJob?.cancel()
        cleaningJob =
            viewModelScope.launch {
                try {
                    val outcomes =
                        withContext(ioDispatcher) {
                            cleanRepository.deleteFileItems(selectedItems)
                        }
                    val deleted = outcomes.filter { it.deleted }
                    val pending = outcomes.filter { !it.deleted && it.authorizationUri != null }
                    val failed = outcomes.filter { !it.deleted && it.authorizationUri == null }
                    val memoryResult = withContext(ioDispatcher) { cleanRepository.cleanMemory() }

                    cleaningExecutionState =
                        CleaningExecutionState(
                            pendingAuthorizationOutcomes = pending,
                            directCleanedFiles = deleted.map { it.junkFile },
                            directFailedFiles = failed.map { it.junkFile },
                            directFreedSpace = deleted.sumOf { it.freedBytes },
                            memoryResult = memoryResult,
                        )

                    val uris = pending.mapNotNull { it.authorizationUri }.map(Uri::parse).distinct()
                    if (uris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val request = MediaStore.createDeleteRequest(requestContext.contentResolver, uris)
                        val message = appContext.getString(R.string.result_confirm_system_deletion, uris.size)
                        dispatch(JunkCleanAction.DeleteAuthorizationRequested(message))
                        eventsChannel.send(JunkCleanEvent.RequestDeleteAuthorization(request))
                    } else {
                        delayRemainingCleaningAnimation(cleaningStartedAt)
                        finishCleaning(
                            extraCleanedFiles = emptyList(),
                            extraFailedFiles = pending.map { it.junkFile },
                            extraFreedSpace = 0L,
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    trackOperationEvent(FeatureOperationEvent.OperationFinished(FeatureKey.JUNK_CLEAN, OperationAction.CLEAN, success = false))
                    dispatch(JunkCleanAction.Failed(R.string.result_clean_error, error.message))
                }
            }
    }

    fun handleAuthorizationResult(approved: Boolean) {
        val pending = cleaningExecutionState.pendingAuthorizationOutcomes
        if (pending.isEmpty()) return

        dispatch(JunkCleanAction.DeleteAuthorizationHandled)
        val cleaningStartedAt = System.currentTimeMillis()
        cleaningJob?.cancel()
        cleaningJob =
            viewModelScope.launch {
                try {
                    val authorizedResult =
                        withContext(ioDispatcher) {
                            if (approved) {
                                cleanRepository.finalizeAuthorizedDeletes(pending)
                            } else {
                                JunkAuthorizedDeleteResult(
                                    cleanedFiles = emptyList(),
                                    failedFiles = pending.map { it.junkFile },
                                    freedBytes = 0L,
                                )
                            }
                        }
                    delayRemainingCleaningAnimation(cleaningStartedAt)
                    finishCleaning(
                        extraCleanedFiles = authorizedResult.cleanedFiles,
                        extraFailedFiles = authorizedResult.failedFiles,
                        extraFreedSpace = authorizedResult.freedBytes,
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    trackOperationEvent(FeatureOperationEvent.OperationFinished(FeatureKey.JUNK_CLEAN, OperationAction.CLEAN, success = false))
                    dispatch(JunkCleanAction.Failed(R.string.result_clean_error_after_authorization, error.message))
                }
            }
    }

    fun clearResult() {
        sharedState.clearCleanResults()
        dispatch(JunkCleanAction.ResultCleared)
    }

    fun showResultAfterCompletionAd() {
        dispatch(JunkCleanAction.CompletionAdDismissed)
    }

    private fun startScanInternal(resetSession: Boolean) {
        if (_uiState.value.scanState == JunkCleanScanState.Scanning) return
        hasStarted = true
        scanJob?.cancel()
        if (resetSession) {
            sharedState.clear()
        }
        val scanStartedAt = System.currentTimeMillis()
        observeProgress()
        trackOperationEvent(FeatureOperationEvent.ScanStarted(FeatureKey.JUNK_CLEAN))
        dispatch(JunkCleanAction.ScanStarted)

        scanJob =
            viewModelScope.launch(ioDispatcher) {
                try {
                    val result = cleanRepository.performFullScan()
                    progressJob?.cancel()
                    dispatch(JunkCleanAction.ScanCompleted(result))
                    delayRemainingScanAnimation(scanStartedAt)
                    loadPreview(result)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    progressJob?.cancel()
                    dispatch(JunkCleanAction.Failed(R.string.scan_failed, error.message, duringScan = true))
                }
            }
        }

    fun cancelActiveOperation() {
        scanJob?.cancel()
        scanJob = null
        progressJob?.cancel()
        progressJob = null
        cleaningJob?.cancel()
        cleaningJob = null
        hasStarted = false
        dispatch(JunkCleanAction.ActiveOperationCancelled)
        cleaningExecutionState = CleaningExecutionState()
    }

    fun cancelCleaningAndReturnToPreview() {
        cleaningJob?.cancel()
        cleaningJob = null
        cleaningExecutionState = CleaningExecutionState()
        dispatch(JunkCleanAction.CleaningCancelled)
    }

    fun requestStopDialog() {
        val phase = _uiState.value.phase
        when (phase) {
            JunkCleanPhase.Scanning -> cancelActiveOperation()
            JunkCleanPhase.Cleaning,
            JunkCleanPhase.AwaitingAuthorization,
            -> cancelCleaningAndReturnToPreview()
            else -> return
        }
        dispatch(JunkCleanAction.StopDialogShown(phase))
    }

    fun dismissStopDialog(resume: Boolean) {
        val stoppedPhase = _uiState.value.stopDialogPhase
        dispatch(JunkCleanAction.StopDialogDismissed)
        if (resume && stoppedPhase == JunkCleanPhase.Scanning) {
            startScanIfNeeded()
        }
    }

    private fun observeProgress() {
        progressJob?.cancel()
        progressJob =
            viewModelScope.launch {
                sharedState.scanProgress.collect { progress ->
                    if (_uiState.value.phase != JunkCleanPhase.Scanning) return@collect
                    val currentResult = sharedState.scanResult.value
                    val foundSize = currentResult?.totalSize ?: progress.foundSize
                    dispatch(
                        JunkCleanAction.ScanProgressUpdated(
                            progress = progress.percent,
                            currentCategory = progress.currentCategory,
                            foundItemCount = currentResult?.totalCount ?: progress.foundCount,
                            foundTotalSize = foundSize,
                        ),
                    )
                }
            }
    }

    private fun loadPreview(scanResult: ScanResult?) {
        if (scanResult == null) {
            dispatch(JunkCleanAction.PreviewUnavailable)
            return
        }

        dispatch(JunkCleanAction.PreviewLoaded(scanResult))
        trackPreviewPublished()
    }

    private fun trackPreviewPublished() {
        val hasGroups = _uiState.value.groups.any { it.items.isNotEmpty() }
        trackOperationEvent(FeatureOperationEvent.ScanFinished(FeatureKey.JUNK_CLEAN, hasGroups))
    }

    private suspend fun finishCleaning(
        extraCleanedFiles: List<JunkFile>,
        extraFailedFiles: List<JunkFile>,
        extraFreedSpace: Long,
    ) {
        val cleanedFiles = cleaningExecutionState.directCleanedFiles + extraCleanedFiles
        val failedFiles = cleaningExecutionState.directFailedFiles + extraFailedFiles
        val freedSpace = cleaningExecutionState.directFreedSpace + extraFreedSpace
        val result =
            CleanResult(
                cleanedFiles = cleanedFiles,
                freedSpace = freedSpace,
                failedFiles = failedFiles,
            )
        val memoryResult = cleaningExecutionState.memoryResult
        val summary =
            CleanupSummary(
                freedSpace = result.freedSpace,
                cleanedCount = result.successCount,
                failedCount = result.failedCount,
                memoryFreedBytes = memoryResult?.freedBytes ?: 0L,
                memoryProcessesKilled = memoryResult?.killedCount ?: 0,
            )

        sharedState.removeCleanedFiles(cleanedFiles)
        sharedState.setCleanResult(result)
        if (memoryResult != null) {
            sharedState.setMemoryResult(memoryResult)
        }
        sharedState.setCleanupSummary(summary)

        cleaningExecutionState = CleaningExecutionState()
        dispatch(JunkCleanAction.CleaningCompleted(summary))
        delay(FINISHING_ANIMATION_MILLIS)
        if (_uiState.value.phase == JunkCleanPhase.CompleteAnimation) {
            trackOperationEvent(FeatureOperationEvent.OperationFinished(FeatureKey.JUNK_CLEAN, OperationAction.CLEAN, success = true))
        }
    }

    private suspend fun delayRemainingScanAnimation(startedAtMillis: Long) {
        val remainingMillis = scanPreviewMinMillis - (System.currentTimeMillis() - startedAtMillis)
        delay(remainingMillis.coerceAtLeast(150L))
    }

    private suspend fun delayRemainingCleaningAnimation(startedAtMillis: Long) {
        val remainingMillis = cleaningAnimationMinMillis - (System.currentTimeMillis() - startedAtMillis)
        if (remainingMillis > 0L) delay(remainingMillis)
    }

    override fun onCleared() {
        scanJob?.cancel()
        progressJob?.cancel()
        cleaningJob?.cancel()
        super.onCleared()
    }
}
