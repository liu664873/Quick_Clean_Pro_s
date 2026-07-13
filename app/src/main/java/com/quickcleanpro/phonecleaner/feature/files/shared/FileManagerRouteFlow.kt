package com.quickcleanpro.phonecleaner.feature.files.shared

import com.quickcleanpro.phonecleaner.feature.files.shared.ui.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.permission.CleanXPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.files.shared.BaseFileManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.shared.FileUri
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowState
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerDeleteConfirmDialog
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerErrorToastEffect
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerNoResultsDialog
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerOperationEventsEffect
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerPermissionState
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerStartEffect
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerStopOperationDialog
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveBackWithReturnAd
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

@Composable
internal fun FileManagerFlowEffects(
    viewModel: BaseFileManagerViewModel,
    featureFlow: FeatureFlowRuntime,
    permissionCoordinator: CleanXPermissionCoordinator,
    feature: FeatureKey,
    permissionState: FileManagerPermissionState,
    errorMessage: String?,
    onClearError: () -> Unit,
    onStartIfNeeded: () -> Unit,
    onPermissionRejected: () -> Unit,
    flowState: FileManagerFlowState,
) {
    FileManagerOperationEventsEffect(viewModel, featureFlow, viewModel::setCompletionAdInFlight)
    FileManagerErrorToastEffect(errorMessage, onClearError)
    FileManagerStartEffect(
        feature,
        permissionState,
        permissionCoordinator,
        featureFlow,
        onStartIfNeeded,
        onPermissionRejected,
    )
}

@Composable
internal fun FileManagerFlowDialogs(
    flowState: FileManagerFlowState,
    viewModel: BaseFileManagerViewModel,
    permissionState: FileManagerPermissionState,
    router: AppNavigator,
    featureFlow: FeatureFlowRuntime,
    permissionCoordinator: CleanXPermissionCoordinator,
    feature: FeatureKey,
    phase: FileOperationPhase,
    deleteDialogVisible: Boolean,
    selectedUris: List<FileUri>,
    onCancelActiveOperation: () -> Unit,
    onCancelDelete: () -> Unit,
    onDeleteReady: () -> Unit,
    onDeleteRejected: () -> Unit,
) {
    FileManagerStopOperationDialog(
        visible = flowState.showStopDialog,
        permissionGranted = permissionState.granted,
        onQuit = {
            viewModel.dismissStopDialog()
            onCancelActiveOperation()
            permissionState.leaveBackWithReturnAd(router, feature, featureFlow)
        },
        onResume = {
            viewModel.dismissStopDialog()
        },
    )

    FileManagerDeleteConfirmDialog(
        visible = deleteDialogVisible,
        permissionGranted = permissionState.granted,
        selectedUris = selectedUris,
        permissionCoordinator = permissionCoordinator,
        onCancel = onCancelDelete,
        onDeleteReady = onDeleteReady,
        onRejected = onDeleteRejected,
    )

    FileManagerNoResultsDialog(
        visible = phase == FileOperationPhase.NoResults,
        permissionGranted = permissionState.granted,
        onBack = { permissionState.leaveBackWithReturnAd(router, feature, featureFlow) },
    )
}
