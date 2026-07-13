package com.quickcleanpro.phonecleaner.feature.files.photoprivacy.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*


import com.quickcleanpro.phonecleaner.feature.files.photoprivacy.PhotoPrivacyManagerUiState
import com.quickcleanpro.phonecleaner.feature.files.photoprivacy.PhotoPrivacyManagerViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBottomActionBar
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.DeleteConfirmDialog
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerScaffold
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerPageBrush
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveBackForPermissionRejected
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHome
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHomeWithReturnAd
import com.quickcleanpro.phonecleaner.feature.files.shared.rememberFileManagerPermissionState
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowDialogs
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowEffects
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileOperationPhaseContent
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerPhotoPrivacyView
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerAction

@Composable
internal fun PhotoPrivacyManagerScreen(
    state: PhotoPrivacyManagerUiState,
    permissionGranted: Boolean,
    onAction: (FileManagerAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onAction(FileManagerAction.Back) }
    FileManagerScaffold(
        title = stringResource(R.string.nav_photo_privacy),
        onBack = { onAction(FileManagerAction.Back) },
        bottomBar = {
            if (permissionGranted && state.phase == FileOperationPhase.Browsing) {
                CleanXBottomActionBar(
                    enabled = state.selectedIds.isNotEmpty(),
                    text = stringResource(R.string.file_remove_location_data),
                    onClick = { onAction(FileManagerAction.RequestDelete) },
                    backgroundColor = Color.Transparent,
                    buttonModifier = Modifier.height(52.dp),
                    buttonCornerRadius = 10.dp,
                    buttonFontSize = 20.sp,
                )
            }
        },
    ) {
        Column(modifier = modifier.background(FileManagerPageBrush).padding(horizontal = 16.dp)) {
            FileOperationPhaseContent(
                phase = state.phase,
                scanningText = stringResource(R.string.file_scanning_photo_privacy),
                deletingText = stringResource(R.string.file_removing_location_data_progress),
                resultAmount = state.removedLocationCount.toString(),
                resultUnit = stringResource(R.string.file_photos),
                resultCaption = stringResource(R.string.file_location_data_removed),
                onNavigateTool = onNavigate,
                onContinue = { onAction(FileManagerAction.ContinueManaging) },
            ) {
                FileManagerPhotoPrivacyView(
                    items = state.displayItems,
                    selectedIds = state.selectedIds,
                    allSelected = state.allSelected,
                    onToggleAll = { onAction(FileManagerAction.ToggleVisibleItems) },
                    onSelect = { onAction(FileManagerAction.ToggleSelection(it)) },
                )
            }
        }
    }
    if (permissionGranted && state.phase == FileOperationPhase.ConfirmDelete) {
        DeleteConfirmDialog(
            title = stringResource(R.string.file_remove_location_title),
            message = stringResource(R.string.file_remove_location_message),
            confirmText = stringResource(R.string.remove),
            onCancel = { onAction(FileManagerAction.CancelDelete) },
            onDelete = { onAction(FileManagerAction.DeleteReady) },
        )
    }
}
