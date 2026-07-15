package com.quickcleanpro.phonecleaner.feature.files.shared

import com.quickcleanpro.phonecleaner.feature.files.shared.ui.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.feature.files.audios.AudiosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileDetailUiState
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.toFileDetailDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.documents.DocumentsManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.largefiles.LargeFilesManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.photos.PhotosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.screenshots.ScreenshotsManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.similarphotos.SimilarPhotosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.videos.VideosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.shared.rememberFileManagerPermissionState
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.common.permission.AppPermissionCoordinator

@Composable
internal fun PhotosFileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: PhotosManagerViewModel,
    initialIndex: Int,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileDetailRoute(
        navigator = navigator,
        permissionCoordinator = permissionCoordinator,
        uiState =
            FileDetailUiState(
                items = uiState.currentDisplayItems.map { it.toFileDetailDisplayItem() },
                initialIndex = uiState.detailStartIndex ?: initialIndex,
                selectedIds = uiState.selectedIds,
                selectedSizeBytes = uiState.selectedSizeBytes,
                selectedUris = uiState.selectedUris,
                confirmDeleteVisible = uiState.phase == FileOperationPhase.ConfirmDelete,
            ),
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
        onToggleSelection = viewModel::toggleSelection,
        onCloseDetail = viewModel::closeDetail,
    )
}

@Composable
internal fun SimilarPhotosFileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: SimilarPhotosManagerViewModel,
    initialIndex: Int,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileDetailRoute(
        navigator = navigator,
        permissionCoordinator = permissionCoordinator,
        uiState =
            FileDetailUiState(
                items = uiState.displayItems.map { it.toFileDetailDisplayItem() },
                initialIndex = uiState.detailStartIndex ?: initialIndex,
                selectedIds = uiState.selectedIds,
                selectedSizeBytes = uiState.selectedSizeBytes,
                selectedUris = uiState.selectedUris,
                confirmDeleteVisible = uiState.phase == FileOperationPhase.ConfirmDelete,
            ),
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
        onToggleSelection = viewModel::toggleSelection,
        onCloseDetail = viewModel::closeDetail,
    )
}

@Composable
internal fun ScreenshotsFileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: ScreenshotsManagerViewModel,
    initialIndex: Int,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileDetailRoute(
        navigator = navigator,
        permissionCoordinator = permissionCoordinator,
        uiState =
            FileDetailUiState(
                items = uiState.displayItems.map { it.toFileDetailDisplayItem() },
                initialIndex = uiState.detailStartIndex ?: initialIndex,
                selectedIds = uiState.selectedIds,
                selectedSizeBytes = uiState.selectedSizeBytes,
                selectedUris = uiState.selectedUris,
                confirmDeleteVisible = uiState.phase == FileOperationPhase.ConfirmDelete,
            ),
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
        onToggleSelection = viewModel::toggleSelection,
        onCloseDetail = viewModel::closeDetail,
    )
}

@Composable
internal fun VideosFileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: VideosManagerViewModel,
    initialIndex: Int,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileDetailRoute(
        navigator = navigator,
        permissionCoordinator = permissionCoordinator,
        uiState =
            FileDetailUiState(
                items = uiState.visibleDisplayItems.map { it.toFileDetailDisplayItem() },
                initialIndex = uiState.detailStartIndex ?: initialIndex,
                selectedIds = uiState.selectedIds,
                selectedSizeBytes = uiState.selectedSizeBytes,
                selectedUris = uiState.selectedUris,
                confirmDeleteVisible = uiState.phase == FileOperationPhase.ConfirmDelete,
            ),
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
        onToggleSelection = viewModel::toggleSelection,
        onCloseDetail = viewModel::closeDetail,
    )
}

@Composable
internal fun AudiosFileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: AudiosManagerViewModel,
    initialIndex: Int,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileDetailRoute(
        navigator = navigator,
        permissionCoordinator = permissionCoordinator,
        uiState =
            FileDetailUiState(
                items = uiState.visibleDisplayItems.map { it.toFileDetailDisplayItem() },
                initialIndex = uiState.detailStartIndex ?: initialIndex,
                selectedIds = uiState.selectedIds,
                selectedSizeBytes = uiState.selectedSizeBytes,
                selectedUris = uiState.selectedUris,
                confirmDeleteVisible = uiState.phase == FileOperationPhase.ConfirmDelete,
            ),
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
        onToggleSelection = viewModel::toggleSelection,
        onCloseDetail = viewModel::closeDetail,
    )
}

@Composable
internal fun LargeFilesFileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: LargeFilesManagerViewModel,
    initialIndex: Int,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileDetailRoute(
        navigator = navigator,
        permissionCoordinator = permissionCoordinator,
        uiState =
            FileDetailUiState(
                items = uiState.visibleDisplayItems.map { it.toFileDetailDisplayItem() },
                initialIndex = uiState.detailStartIndex ?: initialIndex,
                selectedIds = uiState.selectedIds,
                selectedSizeBytes = uiState.selectedSizeBytes,
                selectedUris = uiState.selectedUris,
                confirmDeleteVisible = uiState.phase == FileOperationPhase.ConfirmDelete,
            ),
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
        onToggleSelection = viewModel::toggleSelection,
        onCloseDetail = viewModel::closeDetail,
    )
}

@Composable
internal fun DocumentsFileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: DocumentsManagerViewModel,
    initialIndex: Int,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileDetailRoute(
        navigator = navigator,
        permissionCoordinator = permissionCoordinator,
        uiState =
            FileDetailUiState(
                items = uiState.visibleDisplayItems.map { it.toFileDetailDisplayItem() },
                initialIndex = uiState.detailStartIndex ?: initialIndex,
                selectedIds = uiState.selectedIds,
                selectedSizeBytes = uiState.selectedSizeBytes,
                selectedUris = uiState.selectedUris,
                confirmDeleteVisible = uiState.phase == FileOperationPhase.ConfirmDelete,
            ),
        onRequestDelete = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onDeleteReady = viewModel::deleteSelectedFiles,
        onDeleteRejected = viewModel::rejectSystemDelete,
        onToggleSelection = viewModel::toggleSelection,
        onCloseDetail = viewModel::closeDetail,
    )
}

@Composable
private fun FileDetailRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    uiState: FileDetailUiState,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDeleteReady: () -> Unit,
    onDeleteRejected: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onCloseDetail: () -> Unit,
) {
    val router = navigator
    val permissionState = rememberFileManagerPermissionState()

    fun closeAndBack() {
        onCloseDetail()
        router.back()
    }

    FileDetailScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                FileDetailAction.Back -> closeAndBack()
                FileDetailAction.RequestDelete -> onRequestDelete()
                FileDetailAction.CancelDelete -> onCancelDelete()
                FileDetailAction.DeleteReady -> {
                    onDeleteReady()
                    closeAndBack()
                }
                FileDetailAction.DeleteRejected -> onDeleteRejected()
                is FileDetailAction.ToggleSelection -> onToggleSelection(action.id)
            }
        },
        onNavigate = navigator::open,
    )

    FileManagerDeleteConfirmDialog(
        visible = uiState.confirmDeleteVisible,
        permissionGranted = permissionState.granted,
        selectedUris = uiState.selectedUris,
        permissionCoordinator = permissionCoordinator,
        onCancel = onCancelDelete,
        onDeleteReady = {
            onDeleteReady()
            closeAndBack()
        },
        onRejected = onDeleteRejected,
    )
}
