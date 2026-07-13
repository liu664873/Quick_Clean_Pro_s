package com.quickcleanpro.phonecleaner.feature.files.photos

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.toggleAllVisible

import com.quickcleanpro.phonecleaner.feature.files.shared.toggleId

import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.files.shared.data.FileRepository
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.feature.files.shared.BaseFileManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.shared.FILE_DELETE_ANIMATION_MIN_MILLIS
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.appString
import com.quickcleanpro.phonecleaner.feature.files.shared.fileScanFailedMessage
import com.quickcleanpro.phonecleaner.feature.files.shared.openDetailIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class PhotosManagerViewModel(
    private val repository: FileRepository,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    testLoader: (((suspend () -> Unit)) -> Unit)? = null,
    private val scanDelayMillis: Long = 900L,
    private val deleteDelayMillis: Long = FILE_DELETE_ANIMATION_MIN_MILLIS,
    private val completeDelayMillis: Long = 700L
) : BaseFileManagerViewModel(FeatureKey.PHOTOS, ioDispatcher, testLoader) {

    private val _uiState = MutableStateFlow(PhotosManagerUiState())
    val uiState: StateFlow<PhotosManagerUiState> = _uiState.asStateFlow()

    fun startIfNeeded() {
        if (_uiState.value.phase != FileOperationPhase.Scanning || _uiState.value.items.isNotEmpty()) return
        refresh()
    }

    fun refresh() {
        trackScanStarted()
        _uiState.value = PhotosManagerUiState(phase = FileOperationPhase.Scanning)
        operationRunner.launch {
            runCatching { mapPhotos(repository.loadImages()) }
                .onSuccess { photos ->
                    operationRunner.delayIfNeeded(scanDelayMillis)
                    trackScanFinished(hasResult = photos.isNotEmpty())
                    _uiState.value = PhotosManagerUiState(
                        phase = if (photos.isEmpty()) FileOperationPhase.NoResults else FileOperationPhase.Browsing,
                        items = photos,
                        tabs = buildPhotoTabs(photos),
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    trackScanFinished(hasResult = false)
                    _uiState.update {
                        it.copy(
                            phase = FileOperationPhase.NoResults,
                            errorMessage = error.message ?: fileScanFailedMessage()
                        )
                    }
                }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update {
            it.copy(
                selectedTabIndex = index.coerceAtLeast(0),
                selectedIds = emptySet(),
                detailStartIndex = null,
            )
        }
    }

    fun toggleSelection(id: Int) {
        _uiState.update {
            it.copy(selectedIds = toggleId(it.selectedIds, id))
        }
    }

    fun toggleVisibleItems() {
        _uiState.update { state ->
            state.copy(selectedIds = toggleAllVisible(state.selectedIds, state.visibleIds))
        }
    }

    fun openDetail(index: Int?) {
        _uiState.update { it.copy(detailStartIndex = openDetailIndex(index)) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(detailStartIndex = null) }
    }

    fun requestDelete() {
        _uiState.update {
            if (it.selectedIds.isNotEmpty()) {
                it.copy(phase = FileOperationPhase.ConfirmDelete)
            } else {
                it
            }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(phase = FileOperationPhase.Browsing) }
    }

    fun rejectSystemDelete() {
        cancelDelete()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCancelDeletingPhase() {
        _uiState.update { current ->
            if (current.phase == FileOperationPhase.Deleting) {
                current.copy(phase = FileOperationPhase.Browsing)
            } else {
                current
            }
        }
    }

    fun deleteSelectedFiles() {
        val selectedFiles = _uiState.value.selectedFiles
        runFileOperation(
            selectedFiles = selectedFiles,
            action = OperationAction.DELETE,
            onEmptySelection = { _uiState.update { it.copy(phase = FileOperationPhase.Browsing) } },
            onStart = { _uiState.update { it.copy(phase = FileOperationPhase.Deleting, detailStartIndex = null) } },
            operationDelayMillis = deleteDelayMillis,
            completeDelayMillis = completeDelayMillis,
            operation = { FileOperationOutcome(freedBytes = repository.deleteFiles(selectedFiles)) },
            isSuccessful = { it.freedBytes > 0L },
            onCompleteAnimation = { outcome ->
                val rebuilt = mapPhotos(repository.loadImages())
                _uiState.update {
                    it.copy(
                        phase = FileOperationPhase.CompleteAnimation,
                        items = rebuilt,
                        tabs = buildPhotoTabs(rebuilt),
                        selectedIds = emptySet(),
                        deletedBytes = outcome.freedBytes,
                    )
                }
            },
            onResult = { _uiState.update { it.copy(phase = FileOperationPhase.Result) } },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        phase = FileOperationPhase.Browsing,
                        errorMessage = error.message ?: appString(R.string.deletion_failed)
                    )
                }
            },
        )
    }

    fun continueManaging() {
        _uiState.update { it.copy(phase = FileOperationPhase.Browsing, selectedIds = emptySet(), detailStartIndex = null) }
    }
}
