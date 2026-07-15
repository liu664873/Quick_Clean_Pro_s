package com.quickcleanpro.phonecleaner.app.navigation.register

import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.app.navigation.DETAIL_INITIAL_INDEX_ARG
import com.quickcleanpro.phonecleaner.feature.files.audios.AudiosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.documents.DocumentsManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFilesManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.largefiles.LargeFilesManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.photos.PhotosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.photoprivacy.PhotoPrivacyManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.screenshots.ScreenshotsManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.similarphotos.SimilarPhotosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.videos.VideosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.audios.AudiosManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.shared.AudiosFileDetailRoute
import com.quickcleanpro.phonecleaner.feature.files.shared.DocumentsFileDetailRoute
import com.quickcleanpro.phonecleaner.feature.files.shared.LargeFilesFileDetailRoute
import com.quickcleanpro.phonecleaner.feature.files.shared.PhotosFileDetailRoute
import com.quickcleanpro.phonecleaner.feature.files.shared.ScreenshotsFileDetailRoute
import com.quickcleanpro.phonecleaner.feature.files.shared.SimilarPhotosFileDetailRoute
import com.quickcleanpro.phonecleaner.feature.files.shared.VideosFileDetailRoute
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFilesManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.documents.DocumentsManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.largefiles.LargeFilesManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.photoprivacy.PhotoPrivacyManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.photos.PhotosManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.screenshots.ScreenshotsManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.similarphotos.SimilarPhotosManagerRoute
import com.quickcleanpro.phonecleaner.feature.files.videos.VideosManagerRoute
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerFileManagerRoutes(
    navController: NavHostController,
    navigator: AppNavigator,
    featureFlow: FeatureFlowRuntime,
) {
    composable(AppDestination.PhotosManager.route) {
        PhotosManagerRoute(navigator, koinViewModel<PhotosManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.PhotosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.PhotosManager, backStackEntry)
        }
        val viewModel: PhotosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        PhotosFileDetailRoute(navigator, LocalPermissionCoordinator.current, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.SimilarPhotosManager.route) {
        SimilarPhotosManagerRoute(navigator, koinViewModel<SimilarPhotosManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.SimilarPhotosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.SimilarPhotosManager, backStackEntry)
        }
        val viewModel: SimilarPhotosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        SimilarPhotosFileDetailRoute(navigator, LocalPermissionCoordinator.current, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.PhotoPrivacyManager.route) {
        PhotoPrivacyManagerRoute(navigator, koinViewModel<PhotoPrivacyManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.ScreenshotsManager.route) {
        ScreenshotsManagerRoute(navigator, koinViewModel<ScreenshotsManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.ScreenshotsDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.ScreenshotsManager, backStackEntry)
        }
        val viewModel: ScreenshotsManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        ScreenshotsFileDetailRoute(navigator, LocalPermissionCoordinator.current, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.VideosManager.route) {
        VideosManagerRoute(navigator, koinViewModel<VideosManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.VideosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.VideosManager, backStackEntry)
        }
        val viewModel: VideosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        VideosFileDetailRoute(navigator, LocalPermissionCoordinator.current, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.AudiosManager.route) {
        AudiosManagerRoute(navigator, koinViewModel<AudiosManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.AudiosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.AudiosManager, backStackEntry)
        }
        val viewModel: AudiosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        AudiosFileDetailRoute(navigator, LocalPermissionCoordinator.current, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.LargeFilesManager.route) {
        LargeFilesManagerRoute(navigator, koinViewModel<LargeFilesManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.LargeFilesDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.LargeFilesManager, backStackEntry)
        }
        val viewModel: LargeFilesManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        LargeFilesFileDetailRoute(navigator, LocalPermissionCoordinator.current, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.DuplicateFilesManager.route) {
        DuplicateFilesManagerRoute(navigator, koinViewModel<DuplicateFilesManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.DocumentsManager.route) {
        DocumentsManagerRoute(navigator, koinViewModel<DocumentsManagerViewModel>(), featureFlow)
    }
    composable(AppDestination.DocumentsDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.DocumentsManager, backStackEntry)
        }
        val viewModel: DocumentsManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        DocumentsFileDetailRoute(navigator, LocalPermissionCoordinator.current, viewModel, backStackEntry.initialIndex())
    }
}

private fun NavBackStackEntry.initialIndex(): Int =
    arguments?.getString(DETAIL_INITIAL_INDEX_ARG)?.toIntOrNull()?.coerceAtLeast(0) ?: 0

private fun NavHostController.fileManagerViewModelOwnerOr(
    destination: AppDestination,
    fallback: NavBackStackEntry,
): NavBackStackEntry =
    runCatching { getBackStackEntry(destination.route) }.getOrDefault(fallback)
