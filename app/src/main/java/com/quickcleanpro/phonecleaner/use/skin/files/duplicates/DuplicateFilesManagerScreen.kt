package com.quickcleanpro.phonecleaner.use.skin.files.duplicates

import com.quickcleanpro.phonecleaner.use.feature.files.presentation.duplicates.DuplicateFilesManagerViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.height
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
import com.quickcleanpro.phonecleaner.use.skin.files.common.FileManagerScaffold
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.skin.files.common.leaveBackForPermissionRejected
import com.quickcleanpro.phonecleaner.use.skin.files.common.leaveBackWithReturnAd
import com.quickcleanpro.phonecleaner.use.skin.files.common.leaveHome
import com.quickcleanpro.phonecleaner.use.skin.files.common.leaveHomeWithReturnAd
import com.quickcleanpro.phonecleaner.use.skin.files.common.rememberFileManagerPermissionState
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.FileManagerFlowDialogs
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.FileManagerFlowEffects
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.blockAndShowStopDialog
import com.quickcleanpro.phonecleaner.use.skin.files.common.shell.rememberFileManagerFlowState
import com.quickcleanpro.phonecleaner.use.skin.files.duplicates.views.DuplicateFilesManagerContentView
import com.quickcleanpro.phonecleaner.use.core.common.format.FileSizeFormatter

private val FEATURE = FeatureKey.DUPLICATE_FILES

@Composable
internal fun DuplicateFilesManagerScreen(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: DuplicateFilesManagerViewModel) {
    DuplicateFilesManagerScreenState(navigator = navigator, dependencies = dependencies, viewModel = viewModel)
}

@Composable
private fun DuplicateFilesManagerScreenState(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: DuplicateFilesManagerViewModel) {
    val router = navigator
    val tracker = dependencies.operations
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberFileManagerPermissionState()
    val flowState = rememberFileManagerFlowState()
    val displayState = flowState.blockedPhase?.let { uiState.copy(phase = it) } ?: uiState
    val groupListScrollState = remember { ScrollState(0) }
    val groupDetailScrollStates = remember { mutableMapOf<Int, ScrollState>() }
    fun scrollStateForGroup(groupId: Int): ScrollState = groupDetailScrollStates.getOrPut(groupId) { ScrollState(0) }
    val selectedGroup = displayState.selectedGroup

    fun handleBack() {
        if (flowState.completionAdInFlight) return
        when {
            selectedGroup != null -> viewModel.closeGroup()
            !permissionState.granted -> permissionState.leaveBackForPermissionRejected(router, FEATURE, tracker)
            displayState.phase == FileOperationPhase.Deleting -> {
                viewModel.cancelDeletingAndReturnToBrowsing(); flowState.showStopDialog = true
            }
            displayState.phase == FileOperationPhase.Scanning -> flowState.blockAndShowStopDialog(displayState.phase)
            displayState.phase == FileOperationPhase.Result -> permissionState.leaveHomeWithReturnAd(router, FEATURE, tracker)
            else -> permissionState.leaveBackWithReturnAd(router, FEATURE, tracker)
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
        title = stringResource(R.string.nav_duplicate_files),
        onBack = { handleBack() },
        bottomBar = {
            if (permissionState.granted &&
                (displayState.phase == FileOperationPhase.Browsing || displayState.phase == FileOperationPhase.ConfirmDelete)
            ) {
                val cleanupEnabled = if (selectedGroup == null) {
                    displayState.globalFilesToDelete.isNotEmpty()
                } else {
                    displayState.selectedGroupFilesToDelete.isNotEmpty()
                }
                val cleanupSize = if (selectedGroup == null) {
                    displayState.selectedDeleteSize
                } else {
                    displayState.selectedGroupDeleteSize
                }
                val onCleanupClick = if (selectedGroup == null) {
                    viewModel::requestDelete
                } else {
                    viewModel::requestDeleteCurrentGroup
                }
                CleanXBottomActionBar(
                    enabled = cleanupEnabled,
                    text = stringResource(R.string.file_clean_up_size, formatDuplicateCleanupSize(cleanupSize)),
                    onClick = onCleanupClick,
                    backgroundColor = Color.Transparent,
                    buttonModifier = Modifier.height(52.dp),
                    buttonCornerRadius = 10.dp,
                    buttonFontSize = 20.sp,
                )
            }
        }
    ) {
        DuplicateFilesManagerContentView(
            uiState = displayState,
            groupListScrollState = groupListScrollState,
            scrollStateForGroup = ::scrollStateForGroup,
            onToggleAll = viewModel::toggleAll,
            onOpenGroup = viewModel::openGroup,
            onToggleFile = viewModel::toggleFile,
            onAutoSelect = viewModel::autoSelectCurrentGroup,
            onToggleGroupSelection = viewModel::toggleGroupSelection,
            onAcceptWarning = viewModel::acceptWarning,
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
        deleteDialogVisible = displayState.phase == FileOperationPhase.ConfirmDelete,
        selectedUris = displayState.selectedUris,
        onCancelActiveOperation = viewModel::cancelActiveOperation,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
    )
}

private fun formatDuplicateCleanupSize(bytes: Long): String =
    if (bytes == 0L) "0KB" else FileSizeFormatter.format(bytes)
