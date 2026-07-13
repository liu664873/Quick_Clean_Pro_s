package com.quickcleanpro.phonecleaner.feature.files.shared.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileDetailUiState
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerScaffold
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerTopAction
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerDetailView

internal sealed interface FileDetailAction {
    data object Back : FileDetailAction
    data object RequestDelete : FileDetailAction
    data object CancelDelete : FileDetailAction
    data object DeleteReady : FileDetailAction
    data object DeleteRejected : FileDetailAction
    data class ToggleSelection(val id: Int) : FileDetailAction
}

@Composable
internal fun FileDetailScreen(
    state: FileDetailUiState,
    onAction: (FileDetailAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.totalCount) {
        if (state.totalCount == 0) onAction(FileDetailAction.Back)
    }
    if (state.totalCount == 0) return

    BackHandler { onAction(FileDetailAction.Back) }

    FileManagerScaffold(
        title = fileDetailTitle(state.selectedCount, state.totalCount),
        onBack = { onAction(FileDetailAction.Back) },
        actions = {
            FileManagerTopAction(
                actionText = stringResource(R.string.file_delete_count, state.selectedCount),
                actionEnabled = state.selectedCount > 0,
                onAction = { onAction(FileDetailAction.RequestDelete) },
            )
        },
    ) {
        FileManagerDetailView(
            items = state.items,
            initialIndex = state.initialIndex,
            selectedIds = state.selectedIds,
            selectedSize = state.selectedSizeBytes,
            onToggleSelection = { onAction(FileDetailAction.ToggleSelection(it)) },
        )
    }
}
