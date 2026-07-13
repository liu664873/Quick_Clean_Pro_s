package com.quickcleanpro.phonecleaner.use.skin.files.photoprivacy

import com.quickcleanpro.phonecleaner.use.feature.files.presentation.photoprivacy.PhotoPrivacyManagerUiState
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.photoprivacy.PhotoPrivacyManagerViewModel
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXBottomActionBar
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.DeleteConfirmDialog
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import com.quickcleanpro.phonecleaner.use.skin.files.common.FileManagerScaffold
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.skin.files.common.components.FileManagerPageBrush
import com.quickcleanpro.phonecleaner.use.skin.files.common.leaveBackForPermissionRejected
import com.quickcleanpro.phonecleaner.use.skin.files.common.leaveHome
import com.quickcleanpro.phonecleaner.use.skin.files.common.leaveHomeWithReturnAd
import com.quickcleanpro.phonecleaner.use.skin.files.common.rememberFileManagerPermissionState
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.FileManagerFlowDialogs
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.FileManagerFlowEffects
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.blockAndShowStopDialog
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.rememberFileManagerFlowState
import com.quickcleanpro.phonecleaner.use.skin.files.common.views.FileOperationPhaseContent
import com.quickcleanpro.phonecleaner.use.skin.files.common.views.list.FileManagerPhotoPrivacyView

private val FEATURE = FeatureKey.PHOTO_PRIVACY

@Composable
internal fun PhotoPrivacyManagerScreen(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: PhotoPrivacyManagerViewModel) {
    PhotoPrivacyManagerScreenState(navigator = navigator, dependencies = dependencies, viewModel = viewModel)
}

@Composable
private fun PhotoPrivacyManagerScreenState(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: PhotoPrivacyManagerViewModel) {
    val router = navigator
    val tracker = dependencies.operations
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberFileManagerPermissionState()
    val flowState = rememberFileManagerFlowState()
    val displayState = flowState.blockedPhase?.let { uiState.copy(phase = it) } ?: uiState

    fun handleBack() {
        if (flowState.completionAdInFlight) return
        when {
            !permissionState.granted -> permissionState.leaveBackForPermissionRejected(router, FEATURE, tracker)
            displayState.phase == FileOperationPhase.Deleting -> {
                viewModel.cancelDeletingAndReturnToBrowsing(); flowState.showStopDialog = true
            }
            displayState.phase == FileOperationPhase.Scanning -> flowState.blockAndShowStopDialog(displayState.phase)
            displayState.phase == FileOperationPhase.ConfirmDelete -> viewModel.cancelRemoveLocation()
            else -> permissionState.leaveHomeWithReturnAd(router, FEATURE, tracker)
        }
    }

    FileManagerFlowEffects(
        viewModel = viewModel,
        tracker = tracker,
        permissionCoordinator = dependencies.permissions,
        feature = FEATURE,
        permissionState = permissionState,
        errorMessage = uiState.errorMessage,
        onClearError = viewModel::clearError,
        onStartIfNeeded = viewModel::startIfNeeded,
        onPermissionRejected = { permissionState.leaveHome(router) },
        flowState = flowState,
    )

    BackHandler { handleBack() }

    FileManagerScaffold(
        title = stringResource(R.string.nav_photo_privacy),
        onBack = { handleBack() },
        bottomBar = {
            if (permissionState.granted && displayState.phase == FileOperationPhase.Browsing) {
                CleanXBottomActionBar(
                    enabled = displayState.selectedIds.isNotEmpty(),
                    text = stringResource(R.string.file_remove_location_data),
                    onClick = viewModel::requestRemoveLocation,
                    backgroundColor = Color.Transparent,
                    buttonModifier = Modifier.height(52.dp),
                    buttonCornerRadius = 10.dp,
                    buttonFontSize = 20.sp,
                )
            }
        }
    ) {
        PhotoPrivacyManagerContent(
            uiState = displayState,
            onToggleAll = viewModel::toggleVisibleItems,
            onSelect = viewModel::toggleSelection,
            onNavigateTool = router::resetTo,
            onContinue = viewModel::continueManaging,
        )
    }

    FileManagerFlowDialogs(
        flowState = flowState,
        permissionState = permissionState,
        router = router,
        tracker = tracker,
        permissionCoordinator = dependencies.permissions,
        feature = FEATURE,
        phase = displayState.phase,
        deleteDialogVisible = false,
        selectedUris = emptyList(),
        onCancelActiveOperation = viewModel::cancelActiveOperation,
        onCancelDelete = viewModel::cancelRemoveLocation,
        onDeleteReady = viewModel::removeLocationData,
        onDeleteRejected = viewModel::cancelRemoveLocation,
    )

    if (permissionState.granted && displayState.phase == FileOperationPhase.ConfirmDelete) {
        DeleteConfirmDialog(
            title = stringResource(R.string.file_remove_location_title),
            message = stringResource(R.string.file_remove_location_message),
            confirmText = stringResource(R.string.remove),
            onCancel = viewModel::cancelRemoveLocation,
            onDelete = viewModel::removeLocationData,
        )
    }

}

@Composable
private fun PhotoPrivacyManagerContent(
    uiState: PhotoPrivacyManagerUiState,
    onToggleAll: () -> Unit,
    onSelect: (Int) -> Unit,
    onNavigateTool: (AppDestination) -> Unit,

    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(FileManagerPageBrush)
            .padding(horizontal = 16.dp),
    ) {
        FileOperationPhaseContent(
            phase = uiState.phase,
            scanningText = stringResource(R.string.file_scanning_photo_privacy),
            deletingText = stringResource(R.string.file_removing_location_data_progress),
            resultAmount = uiState.removedLocationCount.toString(),
            resultUnit = stringResource(R.string.file_photos),
            resultCaption = stringResource(R.string.file_location_data_removed),
            onNavigateTool = onNavigateTool,
            onContinue = onContinue,
        ) {
            FileManagerPhotoPrivacyView(
                items = uiState.displayItems,
                selectedIds = uiState.selectedIds,
                allSelected = uiState.allSelected,
                onToggleAll = onToggleAll,
                onSelect = onSelect,
            )
        }
    }
}
