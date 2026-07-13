package com.quickcleanpro.phonecleaner.feature.files.audios

import com.quickcleanpro.phonecleaner.feature.files.shared.*


import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.feature.files.shared.FileImageDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.audios.AudiosManagerUiState
import com.quickcleanpro.phonecleaner.feature.files.audios.AudiosManagerViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBottomActionBar
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerScaffold
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerAudioListView
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerPageBrush
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveBackForPermissionRejected
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHome
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHomeWithReturnAd
import com.quickcleanpro.phonecleaner.feature.files.shared.rememberFileManagerPermissionState
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowDialogs
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowEffects
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileOperationPhaseContent
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerAction


import com.quickcleanpro.phonecleaner.feature.files.audios.ui.AudiosManagerScreen

private val FEATURE = FeatureKey.AUDIOS

@Composable
internal fun AudiosManagerRoute(
    navigator: AppNavigator,
    viewModel: AudiosManagerViewModel,
    featureFlow: FeatureFlowRuntime,
) {
    AudiosManagerRouteContent(navigator = navigator, viewModel = viewModel, featureFlow = featureFlow)
}

@Composable
private fun AudiosManagerRouteContent(
    navigator: AppNavigator,
    viewModel: AudiosManagerViewModel,
    featureFlow: FeatureFlowRuntime,
) {
    val router = navigator
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberFileManagerPermissionState()
    val flowState by viewModel.flowState.collectAsStateWithLifecycle()
    val displayState = flowState.blockedPhase?.let { uiState.copy(phase = it) } ?: uiState
    val isDetailMode = displayState.detailStartIndex != null
    val visibleItems = displayState.visibleDisplayItems

    fun handleBack() {
        if (flowState.completionAdInFlight) return
        when {
            isDetailMode -> viewModel.closeDetail()
            !permissionState.granted -> permissionState.leaveBackForPermissionRejected(router, FEATURE, featureFlow)
            displayState.phase == FileOperationPhase.Deleting -> {
                viewModel.cancelDeletingAndReturnToBrowsing()
                viewModel.showStopDialog()
            }
            displayState.phase == FileOperationPhase.Scanning -> viewModel.showStopDialog(displayState.phase)
            displayState.phase == FileOperationPhase.ConfirmDelete -> viewModel.cancelDelete()
            else -> permissionState.leaveHomeWithReturnAd(router, FEATURE, featureFlow)
        }
    }

    FileManagerFlowEffects(
        viewModel = viewModel,
        featureFlow = featureFlow,
        permissionCoordinator = LocalPermissionCoordinator.current,
        feature = FEATURE,
        permissionState = permissionState,
        errorMessage = uiState.errorMessage,
        onClearError = viewModel::clearError,
        onStartIfNeeded = viewModel::startIfNeeded,
        onPermissionRejected = { permissionState.leaveHome(router) },
        flowState = flowState,
    )

    AudiosManagerScreen(
        state = displayState,
        permissionGranted = permissionState.granted,
        onAction = { action ->
            when (action) {
                FileManagerAction.Back -> handleBack()
                FileManagerAction.RequestDelete -> viewModel.requestDelete()
                FileManagerAction.ContinueManaging -> viewModel.continueManaging()
                is FileManagerAction.SelectTab -> viewModel.selectTab(action.index)
                is FileManagerAction.ToggleSelection -> viewModel.toggleSelection(action.id)
                is FileManagerAction.ToggleVisibleIds -> viewModel.toggleVisibleItems(action.ids)
                is FileManagerAction.OpenDetail -> {
                    viewModel.openDetail(action.index)
                    router.openRoute(AppDestination.AudiosDetail.withDetailInitialIndex(action.index))
                }
                else -> Unit
            }
        },
        onNavigate = router::resetTo,
    )

    FileManagerFlowDialogs(
        flowState = flowState,
        viewModel = viewModel,
        permissionState = permissionState,
        router = router,
        featureFlow = featureFlow,
        permissionCoordinator = LocalPermissionCoordinator.current,
        feature = FEATURE,
        phase = displayState.phase,
        deleteDialogVisible = displayState.phase == FileOperationPhase.ConfirmDelete && !isDetailMode,
        selectedUris = displayState.selectedUris,
        onCancelActiveOperation = viewModel::cancelActiveOperation,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
    )
}

