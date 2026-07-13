package com.quickcleanpro.phonecleaner.use.feature.junkclean.presentation

import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.common.format.FileSizeFormatter
import com.quickcleanpro.phonecleaner.use.core.model.clean.CategoryCleanGroup
import com.quickcleanpro.phonecleaner.use.core.model.clean.CleanItem
import com.quickcleanpro.phonecleaner.use.core.model.clean.JunkCategory
import com.quickcleanpro.phonecleaner.use.feature.junkclean.domain.CleanupSummary

internal val DefaultResultDisplayCategories =
    listOf(
        JunkCategory.CACHE,
        JunkCategory.TEMP_FILE,
        JunkCategory.RESIDUAL,
        JunkCategory.APK,
        JunkCategory.LARGE_FILE,
    )

object JunkCleanReducer {
    fun reduce(state: JunkCleanUiState, action: JunkCleanAction): JunkCleanUiState =
        when (action) {
            JunkCleanAction.ScanStarted ->
                JunkCleanUiState(
                    phase = JunkCleanPhase.Scanning,
                    scanState = JunkCleanScanState.Scanning,
                )

            is JunkCleanAction.ScanProgressUpdated ->
                state.takeIf { it.phase == JunkCleanPhase.Scanning }?.copy(
                    scanState = JunkCleanScanState.Scanning,
                    progress = action.progress,
                    currentCategory = action.currentCategory,
                    foundItemCount = action.foundItemCount,
                    foundTotalSize = action.foundTotalSize,
                    formattedFoundSize = FileSizeFormatter.format(action.foundTotalSize),
                    errorMessageRes = null,
                    errorMessage = null,
                ) ?: state

            is JunkCleanAction.ScanCompleted ->
                state.copy(
                    scanState = JunkCleanScanState.Completed,
                    progress = 100f,
                    currentCategory = null,
                    foundItemCount = action.result.totalCount,
                    foundTotalSize = action.result.totalSize,
                    formattedFoundSize = FileSizeFormatter.format(action.result.totalSize),
                    errorMessageRes = null,
                    errorMessage = null,
                )

            is JunkCleanAction.PreviewLoaded -> publishPreview(state, buildPreviewGroups(action.result))

            JunkCleanAction.PreviewUnavailable ->
                state.copy(
                    phase = JunkCleanPhase.Error,
                    errorMessageRes = R.string.result_no_data,
                    errorMessage = null,
                )

            is JunkCleanAction.ToggleItem -> toggleItem(state, action.itemId)
            is JunkCleanAction.ToggleCategories -> toggleCategories(state, action.categories)
            JunkCleanAction.CleaningRequested -> requestCleaning(state)

            is JunkCleanAction.DeleteAuthorizationRequested ->
                state.copy(
                    phase = JunkCleanPhase.AwaitingAuthorization,
                    awaitingAuthorizationMessage = action.message,
                )

            JunkCleanAction.DeleteAuthorizationHandled ->
                state.copy(
                    phase = JunkCleanPhase.Cleaning,
                    awaitingAuthorizationMessage = null,
                )

            is JunkCleanAction.CleaningCompleted ->
                state.copy(
                    phase = JunkCleanPhase.CompleteAnimation,
                    cleanResult = action.summary.toUiState(),
                    awaitingAuthorizationMessage = null,
                    errorMessageRes = null,
                    errorMessage = null,
                )

            is JunkCleanAction.Failed ->
                state.copy(
                    phase = JunkCleanPhase.Error,
                    scanState = if (action.duringScan) JunkCleanScanState.Error else state.scanState,
                    errorMessageRes = action.messageRes,
                    errorMessage = action.message,
                )

            JunkCleanAction.ResultCleared ->
                state.copy(
                    phase = JunkCleanPhase.Scanning,
                    cleanResult = JunkCleanResultUiState(),
                )

            JunkCleanAction.CompletionAdDismissed ->
                if (state.phase == JunkCleanPhase.CompleteAnimation) {
                    state.copy(phase = JunkCleanPhase.Complete)
                } else {
                    state
                }

            JunkCleanAction.ActiveOperationCancelled -> state.copy(scanState = JunkCleanScanState.Idle)

            JunkCleanAction.CleaningCancelled ->
                if (state.phase == JunkCleanPhase.Cleaning || state.phase == JunkCleanPhase.AwaitingAuthorization) {
                    state.copy(phase = JunkCleanPhase.Preview)
                } else {
                    state
                }
        }

    private fun toggleItem(state: JunkCleanUiState, itemId: String): JunkCleanUiState {
        if (state.phase != JunkCleanPhase.Preview) return state
        val categoryIndex = state.groups.indexOfFirst { group -> group.items.any { it.junkFile.id == itemId } }
        val group = state.groups.getOrNull(categoryIndex) ?: return state
        val groups = state.groups.toMutableList()
        groups[categoryIndex] =
            group.copy(
                items = group.items.map { item ->
                    if (item.junkFile.id == itemId) item.copy(isChecked = !item.isChecked) else item
                },
            )
        return publishPreview(state, groups, state.checkedEmptyCategories - group.category)
    }

    private fun toggleCategories(state: JunkCleanUiState, categories: List<JunkCategory>): JunkCleanUiState {
        if (state.phase != JunkCleanPhase.Preview) return state
        val targetCategories = categories.distinct()
        val targetIndices =
            state.groups.mapIndexedNotNull { index, group ->
                index.takeIf { group.category in targetCategories && group.items.isNotEmpty() }
            }
        if (targetIndices.isEmpty()) {
            val category = targetCategories.firstOrNull() ?: return state
            val checkedEmptyCategories =
                if (category in state.checkedEmptyCategories) {
                    state.checkedEmptyCategories - category
                } else {
                    state.checkedEmptyCategories + category
                }
            return publishPreview(state, state.groups, checkedEmptyCategories)
        }

        val allChecked = targetIndices.flatMap { state.groups[it].items }.all { it.isChecked }
        val groups = state.groups.toMutableList()
        targetIndices.forEach { index ->
            val group = groups[index]
            groups[index] = group.copy(items = group.items.map { it.copy(isChecked = !allChecked) })
        }
        return publishPreview(state, groups, state.checkedEmptyCategories - targetCategories.toSet())
    }

    private fun requestCleaning(state: JunkCleanUiState): JunkCleanUiState {
        if (state.phase != JunkCleanPhase.Preview) return state
        if (state.groups.flatMap { it.items }.any { it.isChecked }) {
            return state.copy(
                phase = JunkCleanPhase.Cleaning,
                awaitingAuthorizationMessage = null,
                errorMessageRes = null,
                errorMessage = null,
            )
        }
        return state.copy(
            phase = JunkCleanPhase.Error,
            errorMessageRes =
                if (state.checkedEmptyCategories.isNotEmpty()) {
                    R.string.result_zero_byte_selection_hint
                } else {
                    R.string.result_select_at_least_one
                },
            errorMessage = null,
        )
    }

    private fun publishPreview(
        state: JunkCleanUiState,
        groups: List<CategoryCleanGroup>,
        selectedEmptyCategories: Set<JunkCategory> = defaultEmptyCheckedCategories(groups),
    ): JunkCleanUiState {
        val checkedEmptyCategories =
            selectedEmptyCategories.filter { category ->
                groups.firstOrNull { it.category == category }?.items?.isEmpty() ?: true
            }.toSet()
        return state.copy(
            phase = JunkCleanPhase.Preview,
            scanState = JunkCleanScanState.Completed,
            groups = groups,
            checkedEmptyCategories = checkedEmptyCategories,
            selectedSummary =
                SelectionSummary(
                    checkedCount = groups.sumOf { it.checkedCount },
                    checkedSize = groups.sumOf { it.checkedSize },
                    checkedEmptyCategoryCount = checkedEmptyCategories.size,
                ),
            awaitingAuthorizationMessage = null,
            errorMessageRes = null,
            errorMessage = null,
        )
    }

    private fun buildPreviewGroups(result: com.quickcleanpro.phonecleaner.use.core.model.clean.ScanResult) =
        result.categoryBreakdown.map { (category, files) ->
            CategoryCleanGroup(category, files.map { CleanItem(junkFile = it, isChecked = true) })
        }

    private fun defaultEmptyCheckedCategories(groups: List<CategoryCleanGroup>): Set<JunkCategory> {
        val groupsByCategory = groups.associateBy { it.category }
        val residualHasFiles =
            groupsByCategory[JunkCategory.RESIDUAL]?.items?.isNotEmpty() == true ||
                groupsByCategory[JunkCategory.DUPLICATE]?.items?.isNotEmpty() == true
        return DefaultResultDisplayCategories.filter { category ->
            when (category) {
                JunkCategory.RESIDUAL -> !residualHasFiles
                else -> groupsByCategory[category]?.items?.isEmpty() ?: true
            }
        }.toSet()
    }

    private fun CleanupSummary.toUiState() =
        JunkCleanResultUiState(
            freedSpace = freedSpace,
            cleanedCount = cleanedCount,
            failedCount = failedCount,
            memoryFreedBytes = memoryFreedBytes,
            memoryProcessesKilled = memoryProcessesKilled,
            totalFreedBytes = totalFreedBytes,
            formattedFreedSpace = totalFreedBytes.takeIf { it > 0L }?.let(FileSizeFormatter::format).orEmpty(),
            hasVisibleResult = hasVisibleResult,
        )
}
