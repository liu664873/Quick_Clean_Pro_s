package com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileItem
import com.quickcleanpro.phonecleaner.feature.files.shared.ManagedFileType
import com.quickcleanpro.phonecleaner.feature.files.shared.data.FileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WhatsAppCleanerPhase {
    Scanning,
    ScanResult,
    Cleaning,
    CompleteAnimation,
    Result,
    Error,
}

enum class WhatsAppCleanerGroup {
    Cache,
    File,
}

enum class WhatsAppCleanerCategory {
    Images,
    Videos,
    Audios,
    Documents,
    Databases,
    Other,
}

data class WhatsAppCleanerSubItem(
    val group: WhatsAppCleanerGroup,
    val category: WhatsAppCleanerCategory,
    val files: List<ManagedFileItem>,
    val selected: Boolean,
) {
    val totalBytes: Long = files.sumOf { it.sizeBytes }
    val hasFiles: Boolean = totalBytes > 0L
}

data class WhatsAppCleanerGroupItem(
    val group: WhatsAppCleanerGroup,
    val children: List<WhatsAppCleanerSubItem>,
    val expanded: Boolean = true,
) {
    val totalBytes: Long = children.sumOf { it.totalBytes }
    val selectedBytes: Long = children.filter { it.selected }.sumOf { it.totalBytes }
    val hasFiles: Boolean = children.any { it.hasFiles }
    val selected: Boolean = hasFiles && children.filter { it.hasFiles }.all { it.selected }
}

data class WhatsAppCleanerUiState(
    val phase: WhatsAppCleanerPhase = WhatsAppCleanerPhase.Scanning,
    val groups: List<WhatsAppCleanerGroupItem> = emptyList(),
    val scannedBytes: Long = 0L,
    val selectedBytes: Long = 0L,
    val selectedCount: Int = 0,
    val deletedBytes: Long = 0L,
    val deletedCount: Int = 0,
    val errorMessage: String? = null,
    val completionAdInFlight: Boolean = false,
)

sealed interface WhatsAppCleanerAction {
    data object Back : WhatsAppCleanerAction
    data object ExitAfterComplete : WhatsAppCleanerAction
    data object StartScan : WhatsAppCleanerAction
    data object CleanSelected : WhatsAppCleanerAction
    data object Retry : WhatsAppCleanerAction
    data object CancelActiveOperation : WhatsAppCleanerAction
    data object CancelCleaningAndReturnToResult : WhatsAppCleanerAction
    data object ClearResult : WhatsAppCleanerAction
    data object ShowResultAfterCompletionAd : WhatsAppCleanerAction
    data object CompletionAdStarted : WhatsAppCleanerAction
    data object CompletionAdFinished : WhatsAppCleanerAction
    data class ToggleGroup(val group: WhatsAppCleanerGroup) : WhatsAppCleanerAction
    data class ToggleCategory(
        val group: WhatsAppCleanerGroup,
        val category: WhatsAppCleanerCategory,
    ) : WhatsAppCleanerAction
    data class ToggleExpanded(val group: WhatsAppCleanerGroup) : WhatsAppCleanerAction
}

class WhatsAppCleanerViewModel(
    application: Application,
    private val repository: FileRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WhatsAppCleanerUiState())
    private var hasStarted = false
    private var scanJob: Job? = null
    private var cleanJob: Job? = null
    private val operationEventsChannel = Channel<FeatureOperationEvent>(Channel.BUFFERED)

    val uiState: StateFlow<WhatsAppCleanerUiState> = _uiState.asStateFlow()
    val operationEvents: Flow<FeatureOperationEvent> = operationEventsChannel.receiveAsFlow()

    fun onAction(action: WhatsAppCleanerAction) {
        when (action) {
            WhatsAppCleanerAction.Back,
            WhatsAppCleanerAction.ExitAfterComplete -> Unit
            WhatsAppCleanerAction.StartScan -> startScanIfNeeded()
            WhatsAppCleanerAction.CleanSelected -> cleanSelectedFiles()
            WhatsAppCleanerAction.Retry -> retry()
            WhatsAppCleanerAction.CancelActiveOperation -> cancelActiveOperation()
            WhatsAppCleanerAction.CancelCleaningAndReturnToResult -> cancelCleaningAndReturnToResult()
            WhatsAppCleanerAction.ClearResult -> clearResult()
            WhatsAppCleanerAction.ShowResultAfterCompletionAd -> showResultAfterCompletionAd()
            WhatsAppCleanerAction.CompletionAdStarted ->
                _uiState.update { it.copy(completionAdInFlight = true) }
            WhatsAppCleanerAction.CompletionAdFinished ->
                _uiState.update { it.copy(completionAdInFlight = false) }
            is WhatsAppCleanerAction.ToggleGroup -> toggleGroup(action.group)
            is WhatsAppCleanerAction.ToggleCategory -> toggleCategory(action.group, action.category)
            is WhatsAppCleanerAction.ToggleExpanded -> toggleExpanded(action.group)
        }
    }

    fun startScanIfNeeded() {
        if (hasStarted) return
        hasStarted = true
        scanJob?.cancel()
        _uiState.value = WhatsAppCleanerUiState(phase = WhatsAppCleanerPhase.Scanning)
        trackOperationEvent(FeatureOperationEvent.ScanStarted(FeatureKey.WHATSAPP_CLEANER))
        scanJob =
            viewModelScope.launch(ioDispatcher) {
                try {
                    val files = repository.loadWhatsAppFiles()
                    delay(SCAN_DELAY_MILLIS)
                    val nextState = buildScanResultState(files)
                    _uiState.value = nextState
                    trackOperationEvent(
                        FeatureOperationEvent.ScanFinished(
                            FeatureKey.WHATSAPP_CLEANER,
                            nextState.scannedBytes > 0L,
                        ),
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(
                            phase = WhatsAppCleanerPhase.Error,
                            errorMessage = error.message ?: appString(R.string.whatsapp_clean_unavailable),
                        )
                    }
                }
            }
    }

    fun toggleGroup(group: WhatsAppCleanerGroup) {
        _uiState.update { state ->
            if (state.phase != WhatsAppCleanerPhase.ScanResult) return@update state
            val targetGroup = state.groups.firstOrNull { it.group == group } ?: return@update state
            val shouldSelect = !targetGroup.selected
            state.withGroups(
                state.groups.map { groupItem ->
                    if (groupItem.group != group) {
                        groupItem
                    } else {
                        groupItem.copy(
                            children = groupItem.children.map { child ->
                                if (child.hasFiles) child.copy(selected = shouldSelect) else child
                            },
                        )
                    }
                },
            )
        }
    }

    fun toggleCategory(group: WhatsAppCleanerGroup, category: WhatsAppCleanerCategory) {
        _uiState.update { state ->
            if (state.phase != WhatsAppCleanerPhase.ScanResult) return@update state
            state.withGroups(
                state.groups.map { groupItem ->
                    if (groupItem.group != group) {
                        groupItem
                    } else {
                        groupItem.copy(
                            children = groupItem.children.map { child ->
                                if (child.category == category && child.hasFiles) {
                                    child.copy(selected = !child.selected)
                                } else {
                                    child
                                }
                            },
                        )
                    }
                },
            )
        }
    }

    fun toggleExpanded(group: WhatsAppCleanerGroup) {
        _uiState.update { state ->
            if (state.phase != WhatsAppCleanerPhase.ScanResult) return@update state
            state.copy(
                groups = state.groups.map { groupItem ->
                    if (groupItem.group == group) {
                        groupItem.copy(expanded = !groupItem.expanded)
                    } else {
                        groupItem
                    }
                },
            )
        }
    }

    fun cleanSelectedFiles() {
        val state = _uiState.value
        if (state.phase != WhatsAppCleanerPhase.ScanResult || state.selectedBytes <= 0L) return

        val selectedFiles =
            state.groups
                .flatMap { it.children }
                .filter { it.selected }
                .flatMap { it.files }
                .distinctBy { it.path ?: it.uri.toString() }
        val expectedBytes = selectedFiles.sumOf { it.sizeBytes }

        trackOperationEvent(FeatureOperationEvent.OperationStarted(FeatureKey.WHATSAPP_CLEANER, OperationAction.CLEAN))
        _uiState.update {
            it.copy(
                phase = WhatsAppCleanerPhase.Cleaning,
                selectedBytes = expectedBytes,
                selectedCount = selectedFiles.size,
                errorMessage = null,
            )
        }
        cleanJob?.cancel()
        cleanJob =
            viewModelScope.launch(ioDispatcher) {
                try {
                    val freedBytes = repository.deleteFiles(selectedFiles).takeIf { it > 0L } ?: expectedBytes
                    delay(RESULT_DELAY_MILLIS)
                    _uiState.value =
                        WhatsAppCleanerUiState(
                            phase = WhatsAppCleanerPhase.CompleteAnimation,
                            deletedBytes = freedBytes,
                            deletedCount = selectedFiles.size,
                        )
                    delay(COMPLETE_ANIMATION_MILLIS)
                    trackOperationEvent(
                        FeatureOperationEvent.OperationFinished(
                            FeatureKey.WHATSAPP_CLEANER,
                            OperationAction.CLEAN,
                            success = true,
                        ),
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    trackOperationEvent(
                        FeatureOperationEvent.OperationFinished(
                            FeatureKey.WHATSAPP_CLEANER,
                            OperationAction.CLEAN,
                            success = false,
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            phase = WhatsAppCleanerPhase.Error,
                            errorMessage = error.message ?: appString(R.string.whatsapp_clean_unavailable),
                        )
                    }
                }
            }
    }

    fun retry() {
        cancelActiveOperation()
        hasStarted = false
        startScanIfNeeded()
    }

    fun cancelActiveOperation() {
        scanJob?.cancel()
        scanJob = null
        cleanJob?.cancel()
        cleanJob = null
        hasStarted = false
    }

    fun clearResult() {
        cancelActiveOperation()
        _uiState.value = WhatsAppCleanerUiState()
    }

    fun showResultAfterCompletionAd() {
        _uiState.update { current ->
            if (current.phase == WhatsAppCleanerPhase.CompleteAnimation) {
                current.copy(phase = WhatsAppCleanerPhase.Result)
            } else {
                current
            }
        }
    }

    fun cancelCleaningAndReturnToResult() {
        cleanJob?.cancel()
        cleanJob = null
        _uiState.update { current ->
            if (current.phase == WhatsAppCleanerPhase.Cleaning || current.phase == WhatsAppCleanerPhase.CompleteAnimation) {
                current.copy(phase = WhatsAppCleanerPhase.ScanResult)
            } else {
                current
            }
        }
    }

    override fun onCleared() {
        scanJob?.cancel()
        cleanJob?.cancel()
        super.onCleared()
    }

    private fun buildScanResultState(files: List<ManagedFileItem>): WhatsAppCleanerUiState {
        val cleanableFiles = files.filter { it.sizeBytes > 0L }
        val groups =
            WhatsAppCleanerGroup.values().map { group ->
                val groupFiles = cleanableFiles.filter { item -> item.toCleanerGroup() == group }
                WhatsAppCleanerGroupItem(
                    group = group,
                    children =
                        WhatsAppCleanerCategory.values().map { category ->
                            val categoryFiles = groupFiles.filter { item -> item.toCleanerCategory() == category }
                            WhatsAppCleanerSubItem(
                                group = group,
                                category = category,
                                files = categoryFiles,
                                selected = categoryFiles.isNotEmpty() && category.shouldSelectByDefault(group),
                            )
                        },
                )
            }
        return WhatsAppCleanerUiState(
            phase = WhatsAppCleanerPhase.ScanResult,
            groups = groups,
            scannedBytes = groups.sumOf { it.totalBytes },
            selectedBytes = groups.sumOf { it.selectedBytes },
            selectedCount = groups.flatMap { it.children }.filter { it.selected }.sumOf { it.files.size },
        )
    }

    private fun trackOperationEvent(event: FeatureOperationEvent) {
        operationEventsChannel.trySend(event)
    }

    private fun WhatsAppCleanerUiState.withGroups(newGroups: List<WhatsAppCleanerGroupItem>): WhatsAppCleanerUiState =
        copy(
            groups = newGroups,
            selectedBytes = newGroups.sumOf { it.selectedBytes },
            selectedCount = newGroups.flatMap { it.children }.filter { it.selected }.sumOf { it.files.size },
        )

    private fun ManagedFileItem.toCleanerGroup(): WhatsAppCleanerGroup {
        val source = listOfNotNull(path, bucketName, name).joinToString("/").lowercase()
        return if (
            "cache" in source ||
            "tmp" in source ||
            "temp" in source ||
            ".trash" in source ||
            ".statuses" in source ||
            "thumb" in source
        ) {
            WhatsAppCleanerGroup.Cache
        } else {
            WhatsAppCleanerGroup.File
        }
    }

    private fun ManagedFileItem.toCleanerCategory(): WhatsAppCleanerCategory {
        val source = listOfNotNull(path, bucketName, name).joinToString("/").lowercase()
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when {
            "database" in source || extension in DATABASE_EXTENSIONS -> WhatsAppCleanerCategory.Databases
            "document" in source || (type == ManagedFileType.Document && extension in DOCUMENT_EXTENSIONS) -> WhatsAppCleanerCategory.Documents
            type == ManagedFileType.Image -> WhatsAppCleanerCategory.Images
            type == ManagedFileType.Video -> WhatsAppCleanerCategory.Videos
            type == ManagedFileType.Audio -> WhatsAppCleanerCategory.Audios
            else -> WhatsAppCleanerCategory.Other
        }
    }

    private fun WhatsAppCleanerCategory.shouldSelectByDefault(group: WhatsAppCleanerGroup): Boolean =
        group == WhatsAppCleanerGroup.Cache || this == WhatsAppCleanerCategory.Other

    private companion object {
        private const val SCAN_DELAY_MILLIS = 650L
        private const val RESULT_DELAY_MILLIS = 1200L
        private const val COMPLETE_ANIMATION_MILLIS = 800L
        val DATABASE_EXTENSIONS = setOf("db", "crypt", "crypt5", "crypt7", "crypt8", "crypt12", "crypt14", "crypt15")
        val DOCUMENT_EXTENSIONS = setOf(
            "pdf",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "ppt",
            "pptx",
            "txt",
            "csv",
            "rtf",
            "zip",
            "rar",
            "7z",
            "apk",
            "json",
            "xml",
        )
    }

    private fun appString(resId: Int): String = getApplication<Application>().getString(resId)
}
