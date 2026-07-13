package com.quickcleanpro.phonecleaner.use.skin.files.documents

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileListDisplayItem
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.documents.DocumentsListStyle
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.documents.DocumentsManagerUiState
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.documents.DocumentsManagerViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXBottomActionBar
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import com.quickcleanpro.phonecleaner.use.skin.files.common.FileManagerScaffold
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.skin.files.common.components.FileManagerListView
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
import com.quickcleanpro.phonecleaner.use.core.common.format.FileSizeFormatter

private val FEATURE = FeatureKey.DOCUMENTS

@Composable
fun DocumentsManagerScreen(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: DocumentsManagerViewModel) {
    DocumentsManagerScreenState(navigator = navigator, dependencies = dependencies, viewModel = viewModel)
}

@Composable
private fun DocumentsManagerScreenState(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: DocumentsManagerViewModel) {
    val router = navigator
    val tracker = dependencies.operations
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberFileManagerPermissionState()
    val flowState = rememberFileManagerFlowState()
    val displayState = flowState.blockedPhase?.let { uiState.copy(phase = it) } ?: uiState
    val latestPermissionGranted by rememberUpdatedState(permissionState.granted)
    val latestDisplayState by rememberUpdatedState(displayState)
    val latestShowStopDialog by rememberUpdatedState(flowState.showStopDialog)
    val latestLeavingPage by rememberUpdatedState(permissionState.leavingPage)
    val scrollStates = remember { mutableMapOf<Int, ScrollState>() }
    fun scrollStateForTab(index: Int): ScrollState = scrollStates.getOrPut(index) { ScrollState(0) }
    val isDetailMode = displayState.detailStartIndex != null
    val visibleItems = displayState.visibleDisplayItems

    DisposableEffect(lifecycleOwner, viewModel) {
        var skipInitialResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            if (skipInitialResume) { skipInitialResume = false; return@LifecycleEventObserver }
            val canRefresh = latestPermissionGranted && !latestLeavingPage &&
                !latestShowStopDialog && latestDisplayState.phase == FileOperationPhase.Browsing
            if (canRefresh) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        title = stringResource(R.string.nav_documents),
        onBack = { handleBack() },
        bottomBar = {
            if (permissionState.granted && displayState.phase == FileOperationPhase.Browsing && !isDetailMode) {
                CleanXBottomActionBar(
                    enabled = displayState.selectedIds.isNotEmpty(),
                    text = if (displayState.selectedSizeBytes > 0L) {
                        stringResource(R.string.file_delete_size, FileSizeFormatter.format(displayState.selectedSizeBytes).replace(" ", ""))
                    } else stringResource(R.string.file_delete),
                    onClick = viewModel::requestDelete,
                    backgroundColor = Color.Transparent,
                    buttonModifier = Modifier.height(52.dp),
                    buttonCornerRadius = 10.dp,
                    buttonFontSize = 20.sp,
                )
            }
        }
    ) {
        DocumentsManagerContent(
            uiState = displayState,
            scrollState = scrollStateForTab(displayState.selectedTabIndex),
            onTabSelected = viewModel::selectTab,
            onToggleAll = viewModel::toggleVisibleItems,
            onSelect = viewModel::toggleSelection,
            onOpenDetail = { item ->
                val index = visibleItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                viewModel.openDetail(index)
                router.openRoute(AppDestination.DocumentsDetail.withDetailInitialIndex(index))
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
private fun DocumentsManagerContent(
    uiState: DocumentsManagerUiState,
    scrollState: ScrollState,
    onTabSelected: (Int) -> Unit,
    onToggleAll: () -> Unit,
    onSelect: (Int) -> Unit,
    onOpenDetail: (com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileListDisplayItem) -> Unit,
    onNavigateTool: (AppDestination) -> Unit,

    onContinue: () -> Unit,
) {
    val visibleItems = uiState.visibleDisplayItems
    val result = uiState.resultSize
    Column(
        modifier = Modifier
            .background(FileManagerPageBrush)
            .padding(horizontal = 16.dp),
    ) {
        FileOperationPhaseContent(
            phase = uiState.phase,
            scanningText = stringResource(R.string.file_scanning_documents),
            deletingText = stringResource(R.string.file_deleting_files),
            resultAmount = result.first,
            resultUnit = result.second,
            resultCaption = stringResource(R.string.file_deleted_in_cleanup),
            onNavigateTool = onNavigateTool,
            onContinue = onContinue,
        ) {
            FileManagerListView(
                tabs = uiState.displayTabs,
                items = visibleItems,
                selectedTabIndex = uiState.selectedTabIndex,
                selectedIds = uiState.selectedIds,
                allSelected = uiState.allSelected,
                scrollState = scrollState,
                style = DocumentsListStyle,
                onTabSelected = onTabSelected,
                onToggleAll = onToggleAll,
                onSelect = onSelect,
                onOpenDetail = onOpenDetail,
            )
        }
    }
}

