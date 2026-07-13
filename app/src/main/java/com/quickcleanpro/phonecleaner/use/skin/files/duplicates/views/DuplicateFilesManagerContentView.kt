package com.quickcleanpro.phonecleaner.use.skin.files.duplicates.views

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.duplicates.DuplicateFileEntry
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.duplicates.DuplicateGroupItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.skin.files.common.components.FileManagerPageBrush
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.splitFileSizeLabel
import com.quickcleanpro.phonecleaner.use.skin.files.common.views.FileOperationPhaseContent
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.duplicates.DuplicateFilesManagerUiState
import com.quickcleanpro.phonecleaner.use.core.common.format.FileSizeFormatter

@Composable
internal fun DuplicateFilesManagerContentView(
    uiState: DuplicateFilesManagerUiState,
    groupListScrollState: ScrollState,
    scrollStateForGroup: (Int) -> ScrollState,
    onToggleAll: () -> Unit,
    onOpenGroup: (DuplicateGroupItem) -> Unit,
    onToggleFile: (DuplicateFileEntry) -> Unit,
    onAutoSelect: () -> Unit,
    onToggleGroupSelection: (DuplicateGroupItem) -> Unit,
    onAcceptWarning: () -> Unit,
    onNavigateTool: (AppDestination) -> Unit,

    onContinue: () -> Unit,
) {
    val result = FileSizeFormatter.format(uiState.deletedBytes).splitFileSizeLabel()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FileManagerPageBrush)
            .padding(horizontal = 16.dp),
    ) {
        FileOperationPhaseContent(
            phase = uiState.phase,
            scanningText = stringResource(R.string.file_scanning_duplicate_files),
            deletingText = stringResource(R.string.file_deleting_duplicate_files),
            resultAmount = result.first,
            resultUnit = result.second,
            resultCaption = stringResource(R.string.file_deleted_in_cleanup),
            onNavigateTool = onNavigateTool,
            onContinue = onContinue,
        ) {
            if (uiState.phase == FileOperationPhase.Browsing || uiState.phase == FileOperationPhase.ConfirmDelete) {
                val group = uiState.selectedGroup
                if (group == null) {
                    DuplicateFilesGroupListView(
                        groups = uiState.groups,
                        selectedFileKeys = uiState.selectedFileKeys,
                        allSelected = uiState.allSelected,
                        showWarning = uiState.showWarning,
                        scrollState = groupListScrollState,
                        onToggleAll = onToggleAll,
                        onOpenGroup = onOpenGroup,
                        onToggleGroupSelection = onToggleGroupSelection,
                        onAcceptWarning = onAcceptWarning,
                    )
                } else {
                    DuplicateFilesGroupDetailView(
                        group = group,
                        selectedFileKeys = uiState.selectedFileKeys,
                        scrollState = scrollStateForGroup(group.id),
                        onToggleFile = onToggleFile,
                        onAutoSelect = onAutoSelect,
                        onToggleGroupSelection = { onToggleGroupSelection(group) },
                    )
                }
            }
        }
    }
}
