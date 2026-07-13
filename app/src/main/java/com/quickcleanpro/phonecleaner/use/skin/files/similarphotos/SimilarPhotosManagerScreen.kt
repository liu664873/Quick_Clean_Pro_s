package com.quickcleanpro.phonecleaner.use.skin.files.similarphotos

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileImageDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.similarphotos.SimilarPhotosManagerUiState
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.similarphotos.SimilarPhotosManagerViewModel
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
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXBottomActionBar
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileImageGroupDisplayItem
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
import com.quickcleanpro.phonecleaner.use.skin.files.common.views.list.FileManagerSimilarPhotosView

private val FEATURE = FeatureKey.SIMILAR_PHOTOS

@Composable
fun SimilarPhotosManagerScreen(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: SimilarPhotosManagerViewModel) {
    SimilarPhotosManagerScreenState(navigator = navigator, dependencies = dependencies, viewModel = viewModel)
}

@Composable
private fun SimilarPhotosManagerScreenState(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: SimilarPhotosManagerViewModel) {
    val router = navigator
    val tracker = dependencies.operations
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberFileManagerPermissionState()
    val flowState = rememberFileManagerFlowState()
    val displayState = flowState.blockedPhase?.let { uiState.copy(phase = it) } ?: uiState
    val scrollState = remember { ScrollState(0) }
    val isDetailMode = displayState.detailStartIndex != null
    val displayItems = displayState.displayItems

    fun handleBack() {
        if (flowState.completionAdInFlight) return
        when {
            isDetailMode -> viewModel.closeDetail()
            !permissionState.granted -> permissionState.leaveBackForPermissionRejected(router, FEATURE, tracker)
            displayState.phase == FileOperationPhase.Deleting -> {
                viewModel.cancelDeletingAndReturnToBrowsing(); flowState.showStopDialog = true
            }
            displayState.phase == FileOperationPhase.Scanning -> flowState.blockAndShowStopDialog(displayState.phase)
            displayState.phase == FileOperationPhase.ConfirmDelete -> viewModel.cancelDelete()
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
        title = stringResource(R.string.nav_similar_photos),
        onBack = { handleBack() },
        bottomBar = {
            if (permissionState.granted && displayState.phase == FileOperationPhase.Browsing && !isDetailMode) {
                CleanXBottomActionBar(
                    enabled = displayState.selectedIds.isNotEmpty(),
                    text = stringResource(R.string.file_delete),
                    onClick = viewModel::requestDelete,
                    backgroundColor = Color.Transparent,
                    buttonModifier = Modifier.height(52.dp),
                    buttonCornerRadius = 10.dp,
                    buttonFontSize = 20.sp,
                )
            }
        }
    ) {
        SimilarPhotosManagerContent(
            uiState = displayState,
            scrollState = scrollState,
            onToggleGroup = { group -> viewModel.toggleGroup(group.items.map { it.id }.toSet()) },
            onSelect = viewModel::toggleSelection,
            onOpenDetail = { item ->
                val index = displayItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                viewModel.openDetail(index)
                router.openRoute(AppDestination.SimilarPhotosDetail.withDetailInitialIndex(index))
            },
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
        deleteDialogVisible = displayState.phase == FileOperationPhase.ConfirmDelete && !isDetailMode,
        selectedUris = displayState.selectedUris,
        onCancelActiveOperation = viewModel::cancelActiveOperation,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
    )
}

@Composable
private fun SimilarPhotosManagerContent(
    uiState: SimilarPhotosManagerUiState,
    scrollState: ScrollState,
    onToggleGroup: (FileImageGroupDisplayItem) -> Unit,
    onSelect: (Int) -> Unit,
    onOpenDetail: (com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileImageDisplayItem) -> Unit,
    onNavigateTool: (AppDestination) -> Unit,

    onContinue: () -> Unit,
) {
    val result = uiState.resultSize
    Column(
        modifier = Modifier
            .background(FileManagerPageBrush)
            .padding(horizontal = 16.dp),
    ) {
        FileOperationPhaseContent(
            phase = uiState.phase,
            scanningText = stringResource(R.string.file_scanning_similar_photos),
            deletingText = stringResource(R.string.file_cleanup_completed),
            resultAmount = result.first,
            resultUnit = result.second,
            resultCaption = stringResource(R.string.file_deleted_in_cleanup),
            onNavigateTool = onNavigateTool,
            onContinue = onContinue,
        ) {
            FileManagerSimilarPhotosView(
                groups = uiState.displayGroups,
                selectedIds = uiState.selectedIds,
                scrollState = scrollState,
                onToggleGroup = onToggleGroup,
                onSelect = onSelect,
                onOpenDetail = onOpenDetail,
            )
        }
    }
}

