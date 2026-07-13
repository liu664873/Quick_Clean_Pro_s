package com.quickcleanpro.phonecleaner.use.skin.files.common.detail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileDetailUiState
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionCoordinator
import com.quickcleanpro.phonecleaner.use.skin.files.common.FileManagerDeleteConfirmDialog
import com.quickcleanpro.phonecleaner.use.skin.files.common.FileManagerScaffold
import com.quickcleanpro.phonecleaner.use.skin.files.common.components.FileManagerTopAction
import com.quickcleanpro.phonecleaner.use.skin.files.common.views.detail.FileManagerDetailView

@Composable
internal fun FileDetailScreen(
    uiState: FileDetailUiState,
    permissionGranted: Boolean,
    permissionCoordinator: CleanXPermissionCoordinator,
    onBack: () -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDeleteReady: () -> Unit,
    onDeleteRejected: () -> Unit,
    onToggleSelection: (Int) -> Unit,
) {
    LaunchedEffect(uiState.totalCount) {
        if (uiState.totalCount == 0) onBack()
    }
    if (uiState.totalCount == 0) return

    BackHandler(onBack = onBack)

    FileManagerScaffold(
        title = fileDetailTitle(uiState.selectedCount, uiState.totalCount),
        onBack = onBack,
        actions = {
            FileManagerTopAction(
                actionText = stringResource(R.string.file_delete_count, uiState.selectedCount),
                actionEnabled = uiState.selectedCount > 0,
                onAction = onRequestDelete,
            )
        },
    ) {
        FileManagerDetailView(
            items = uiState.items,
            initialIndex = uiState.initialIndex,
            selectedIds = uiState.selectedIds,
            selectedSize = uiState.selectedSizeBytes,
            onToggleSelection = onToggleSelection,
        )
    }

    FileManagerDeleteConfirmDialog(
        visible = uiState.confirmDeleteVisible,
        permissionGranted = permissionGranted,
        selectedUris = uiState.selectedUris,
        permissionCoordinator = permissionCoordinator,
        onCancel = onCancelDelete,
        onDeleteReady = {
            onDeleteReady()
            onBack()
        },
        onRejected = onDeleteRejected,
    )
}
