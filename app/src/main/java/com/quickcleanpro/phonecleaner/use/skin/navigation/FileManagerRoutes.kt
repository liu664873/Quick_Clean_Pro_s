package com.quickcleanpro.phonecleaner.use.skin.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.navigation.DETAIL_INITIAL_INDEX_ARG
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.audios.AudiosManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.documents.DocumentsManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.duplicates.DuplicateFilesManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.largefiles.LargeFilesManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.photos.PhotosManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.photoprivacy.PhotoPrivacyManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.screenshots.ScreenshotsManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.similarphotos.SimilarPhotosManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.videos.VideosManagerViewModel
import com.quickcleanpro.phonecleaner.use.skin.files.audios.AudiosManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.common.detail.AudiosFileDetailScreen
import com.quickcleanpro.phonecleaner.use.skin.files.common.detail.DocumentsFileDetailScreen
import com.quickcleanpro.phonecleaner.use.skin.files.common.detail.LargeFilesFileDetailScreen
import com.quickcleanpro.phonecleaner.use.skin.files.common.detail.PhotosFileDetailScreen
import com.quickcleanpro.phonecleaner.use.skin.files.common.detail.ScreenshotsFileDetailScreen
import com.quickcleanpro.phonecleaner.use.skin.files.common.detail.SimilarPhotosFileDetailScreen
import com.quickcleanpro.phonecleaner.use.skin.files.common.detail.VideosFileDetailScreen
import com.quickcleanpro.phonecleaner.use.skin.files.duplicates.DuplicateFilesManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.documents.DocumentsManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.largefiles.LargeFilesManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.photoprivacy.PhotoPrivacyManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.photos.PhotosManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.screenshots.ScreenshotsManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.similarphotos.SimilarPhotosManagerScreen
import com.quickcleanpro.phonecleaner.use.skin.files.videos.VideosManagerScreen
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerFileManagerRoutes(
    navController: NavHostController,
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
) {
    composable(AppDestination.PhotosManager.route) {
        PhotosManagerScreen(navigator, dependencies, koinViewModel<PhotosManagerViewModel>())
    }
    composable(AppDestination.PhotosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.PhotosManager, backStackEntry)
        }
        val viewModel: PhotosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        PhotosFileDetailScreen(navigator, dependencies.permissions, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.SimilarPhotosManager.route) {
        SimilarPhotosManagerScreen(navigator, dependencies, koinViewModel<SimilarPhotosManagerViewModel>())
    }
    composable(AppDestination.SimilarPhotosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.SimilarPhotosManager, backStackEntry)
        }
        val viewModel: SimilarPhotosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        SimilarPhotosFileDetailScreen(navigator, dependencies.permissions, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.PhotoPrivacyManager.route) {
        PhotoPrivacyManagerScreen(navigator, dependencies, koinViewModel<PhotoPrivacyManagerViewModel>())
    }
    composable(AppDestination.ScreenshotsManager.route) {
        ScreenshotsManagerScreen(navigator, dependencies, koinViewModel<ScreenshotsManagerViewModel>())
    }
    composable(AppDestination.ScreenshotsDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.ScreenshotsManager, backStackEntry)
        }
        val viewModel: ScreenshotsManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        ScreenshotsFileDetailScreen(navigator, dependencies.permissions, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.VideosManager.route) {
        VideosManagerScreen(navigator, dependencies, koinViewModel<VideosManagerViewModel>())
    }
    composable(AppDestination.VideosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.VideosManager, backStackEntry)
        }
        val viewModel: VideosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        VideosFileDetailScreen(navigator, dependencies.permissions, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.AudiosManager.route) {
        AudiosManagerScreen(navigator, dependencies, koinViewModel<AudiosManagerViewModel>())
    }
    composable(AppDestination.AudiosDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.AudiosManager, backStackEntry)
        }
        val viewModel: AudiosManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        AudiosFileDetailScreen(navigator, dependencies.permissions, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.LargeFilesManager.route) {
        LargeFilesManagerScreen(navigator, dependencies, koinViewModel<LargeFilesManagerViewModel>())
    }
    composable(AppDestination.LargeFilesDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.LargeFilesManager, backStackEntry)
        }
        val viewModel: LargeFilesManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        LargeFilesFileDetailScreen(navigator, dependencies.permissions, viewModel, backStackEntry.initialIndex())
    }
    composable(AppDestination.DuplicateFilesManager.route) {
        DuplicateFilesManagerScreen(navigator, dependencies, koinViewModel<DuplicateFilesManagerViewModel>())
    }
    composable(AppDestination.DocumentsManager.route) {
        DocumentsManagerScreen(navigator, dependencies, koinViewModel<DocumentsManagerViewModel>())
    }
    composable(AppDestination.DocumentsDetail.detailPattern()) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.fileManagerViewModelOwnerOr(AppDestination.DocumentsManager, backStackEntry)
        }
        val viewModel: DocumentsManagerViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        DocumentsFileDetailScreen(navigator, dependencies.permissions, viewModel, backStackEntry.initialIndex())
    }
}

private fun NavBackStackEntry.initialIndex(): Int =
    arguments?.getString(DETAIL_INITIAL_INDEX_ARG)?.toIntOrNull()?.coerceAtLeast(0) ?: 0

private fun NavHostController.fileManagerViewModelOwnerOr(
    destination: AppDestination,
    fallback: NavBackStackEntry,
): NavBackStackEntry =
    runCatching { getBackStackEntry(destination.route) }.getOrDefault(fallback)
