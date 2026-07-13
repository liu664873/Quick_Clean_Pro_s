package com.quickcleanpro.phonecleaner.use.feature.files.presentation.duplicates

import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.toggleIds

import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.toggleId

import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.FileUri
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.ManagedFileItem
import com.quickcleanpro.phonecleaner.use.feature.files.domain.FileRepository
import com.quickcleanpro.phonecleaner.use.feature.files.domain.DuplicateFilesPreferences
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.BaseFileManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FILE_DELETE_ANIMATION_MIN_MILLIS
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.appString
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.duplicateScanFailedMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class DuplicateFilesManagerUiState(
    val phase: FileOperationPhase = FileOperationPhase.Scanning,
    val groups: List<DuplicateGroupItem> = emptyList(),
    val selectedGroupId: Int? = null,
    val selectedFileKeys: Set<String> = emptySet(),
    val pendingDeleteFileKeys: Set<String>? = null,
    val deletedBytes: Long = 0L,
    val errorMessage: String? = null,
    val showWarning: Boolean = false,
) {
    val selectedGroup: DuplicateGroupItem?
        get() = groups.firstOrNull { it.id == selectedGroupId }

    val allDeleteFileKeys: Set<String>
        get() = groups.flatMap { group -> group.files.drop(1).map(::duplicateFileKey) }.toSet()

    val activeDeleteFileKeys: Set<String>
        get() = pendingDeleteFileKeys ?: selectedFileKeys

    val filesToDelete: List<ManagedFileItem>
        get() = filesMatchingKeys(activeDeleteFileKeys)

    val globalFilesToDelete: List<ManagedFileItem>
        get() = filesMatchingKeys(selectedFileKeys)

    val selectedGroupFileKeys: Set<String>
        get() {
            val group = selectedGroup ?: return emptySet()
            val groupKeys = group.files.map(::duplicateFileKey).toSet()
            return selectedFileKeys.intersect(groupKeys)
        }

    val selectedGroupFilesToDelete: List<ManagedFileItem>
        get() = filesMatchingKeys(selectedGroupFileKeys)

    val selectedGroupDeleteSize: Long get() = selectedGroupFilesToDelete.sumOf { it.sizeBytes }

    private fun filesMatchingKeys(keys: Set<String>): List<ManagedFileItem> =
        groups.flatMap { group ->
            group.files
                .filter { duplicateFileKey(it) in keys }
                .mapNotNull { it.realFile }
        }

    val selectedDeleteSize: Long get() = filesToDelete.sumOf { it.sizeBytes }

    val allSelected: Boolean
        get() = allDeleteFileKeys.isNotEmpty() && selectedFileKeys.containsAll(allDeleteFileKeys)

    val selectedUris: List<FileUri> get() = filesToDelete.map { it.uri }
}

internal class DuplicateFilesManagerViewModel(
    private val repository: FileRepository,
    private val preferences: DuplicateFilesPreferences,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    testLoader: (((suspend () -> Unit)) -> Unit)? = null,
    private val scanDelayMillis: Long = 900L,
    private val deleteDelayMillis: Long = FILE_DELETE_ANIMATION_MIN_MILLIS,
    private val completeDelayMillis: Long = 700L
) : BaseFileManagerViewModel(FeatureKey.DUPLICATE_FILES, ioDispatcher, testLoader) {

    constructor(
        repository: FileRepository,
        preferences: DuplicateFilesPreferences,
        ioDispatcher: CoroutineDispatcher
    ) : this(
        repository = repository,
        preferences = preferences,
        ioDispatcher = ioDispatcher,
        testLoader = null,
        scanDelayMillis = 900L,
        deleteDelayMillis = FILE_DELETE_ANIMATION_MIN_MILLIS,
        completeDelayMillis = 700L
    )

    private val _uiState =
        MutableStateFlow(
            DuplicateFilesManagerUiState(showWarning = !preferences.isWarningAccepted()),
        )
    val uiState: StateFlow<DuplicateFilesManagerUiState> = _uiState.asStateFlow()

    fun startIfNeeded() {
        if (hasStartedForCurrentState()) return
        refresh()
    }

    fun refresh() {
        trackScanStarted()
        _uiState.value =
            DuplicateFilesManagerUiState(
                phase = FileOperationPhase.Scanning,
                showWarning = !preferences.isWarningAccepted(),
            )
        operationRunner.launch {
            runCatching { mapDuplicateGroups(repository.loadDuplicateFiles()) }
                .onSuccess { groups ->
                    operationRunner.delayIfNeeded(scanDelayMillis)
                    trackScanFinished(hasResult = groups.isNotEmpty())
                    _uiState.value = DuplicateFilesManagerUiState(
                        phase = if (groups.isEmpty()) FileOperationPhase.NoResults else FileOperationPhase.Browsing,
                        groups = groups,
                        selectedFileKeys = emptySet(),
                        showWarning = !preferences.isWarningAccepted(),
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    trackScanFinished(hasResult = false)
                    _uiState.update {
                        it.copy(
                            phase = FileOperationPhase.NoResults,
                            errorMessage = error.message ?: duplicateScanFailedMessage()
                        )
                    }
                }
        }
    }

    fun openGroup(group: DuplicateGroupItem) {
        _uiState.update { it.copy(selectedGroupId = group.id) }
    }

    fun closeGroup() {
        _uiState.update { it.copy(selectedGroupId = null) }
    }

    fun toggleAll() {
        _uiState.update {
            it.copy(selectedFileKeys = toggleIds(it.selectedFileKeys, it.allDeleteFileKeys))
        }
    }

    fun acceptWarning() {
        preferences.acceptWarning()
        _uiState.update { it.copy(showWarning = false) }
    }

    fun toggleFile(file: DuplicateFileEntry) {
        val key = duplicateFileKey(file)
        _uiState.update {
            it.copy(selectedFileKeys = toggleId(it.selectedFileKeys, key))
        }
    }

    fun autoSelectCurrentGroup() {
        val group = _uiState.value.selectedGroup ?: return
        val groupKeys = group.files.map(::duplicateFileKey).toSet()
        val deletableGroupKeys = group.files.drop(1).map(::duplicateFileKey).toSet()
        _uiState.update {
            it.copy(selectedFileKeys = (it.selectedFileKeys - groupKeys) + deletableGroupKeys)
        }
    }

    fun toggleGroupSelection(group: DuplicateGroupItem) {
        val groupKeys = group.files.map(::duplicateFileKey).toSet()
        val deletableGroupKeys = group.files.drop(1).map(::duplicateFileKey).toSet()
        _uiState.update {
            val selectedGroupKeys = it.selectedFileKeys.intersect(groupKeys)
            it.copy(
                selectedFileKeys = if (selectedGroupKeys == deletableGroupKeys) {
                    it.selectedFileKeys - groupKeys
                } else {
                    (it.selectedFileKeys - groupKeys) + deletableGroupKeys
                }
            )
        }
    }

    fun toggleCurrentGroupSelection() {
        val group = _uiState.value.selectedGroup ?: return
        toggleGroupSelection(group)
    }

    fun requestDelete() {
        if (_uiState.value.globalFilesToDelete.isNotEmpty()) {
            trackActionRequested(OperationAction.DELETE)
            _uiState.update { it.copy(phase = FileOperationPhase.ConfirmDelete, pendingDeleteFileKeys = null) }
        }
    }

    fun requestDeleteCurrentGroup() {
        val state = _uiState.value
        val selectedGroupKeys = state.selectedGroupFileKeys
        if (state.selectedGroupFilesToDelete.isNotEmpty()) {
            trackActionRequested(OperationAction.DELETE)
            _uiState.update {
                it.copy(
                    phase = FileOperationPhase.ConfirmDelete,
                    pendingDeleteFileKeys = selectedGroupKeys
                )
            }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(phase = FileOperationPhase.Browsing, pendingDeleteFileKeys = null) }
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
                current.copy(phase = FileOperationPhase.Browsing, pendingDeleteFileKeys = null)
            } else {
                current
            }
        }
    }

    fun deleteSelectedFiles() {
        val selectedFiles = _uiState.value.filesToDelete
        runFileOperation(
            selectedFiles = selectedFiles,
            action = OperationAction.DELETE,
            onEmptySelection = {
                _uiState.update { it.copy(phase = FileOperationPhase.Browsing, pendingDeleteFileKeys = null) }
            },
            onStart = { _uiState.update { it.copy(phase = FileOperationPhase.Deleting, selectedGroupId = null) } },
            operationDelayMillis = deleteDelayMillis,
            completeDelayMillis = completeDelayMillis,
            operation = { FileOperationOutcome(freedBytes = repository.deleteFiles(selectedFiles)) },
            isSuccessful = { it.freedBytes > 0L },
            onCompleteAnimation = { outcome ->
                val groups = mapDuplicateGroups(repository.loadDuplicateFiles())
                _uiState.update {
                    it.copy(
                        phase = FileOperationPhase.CompleteAnimation,
                        groups = groups,
                        selectedGroupId = null,
                        selectedFileKeys = emptySet(),
                        pendingDeleteFileKeys = null,
                        deletedBytes = outcome.freedBytes,
                    )
                }
            },
            onResult = { _uiState.update { it.copy(phase = FileOperationPhase.Result) } },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        phase = FileOperationPhase.Browsing,
                        pendingDeleteFileKeys = null,
                        errorMessage = error.message ?: appString(R.string.deletion_failed)
                    )
                }
            },
        )
    }

    fun continueManaging() {
        _uiState.update {
            it.copy(
                phase = FileOperationPhase.Browsing,
                selectedFileKeys = emptySet(),
                pendingDeleteFileKeys = null,
                selectedGroupId = null
            )
        }
    }

    private fun hasStartedForCurrentState(): Boolean {
        val state = _uiState.value
        if (state.phase != FileOperationPhase.Scanning) return true
        return state.groups.isNotEmpty() || state.errorMessage != null
    }
}
