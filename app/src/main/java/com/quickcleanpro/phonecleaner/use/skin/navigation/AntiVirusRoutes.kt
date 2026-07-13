package com.quickcleanpro.phonecleaner.use.skin.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.use.skin.antivirus.AntiVirusScreen
import com.quickcleanpro.phonecleaner.use.skin.antivirus.DeepScanVirusScreen
import com.quickcleanpro.phonecleaner.use.skin.antivirus.NoVirusResultScreen
import com.quickcleanpro.phonecleaner.use.skin.antivirus.QuickScanVirusScreen
import com.quickcleanpro.phonecleaner.use.skin.antivirus.ScanVirusResultScreen
import com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation.VirusScanViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerAntiVirusRoutes(
    navController: NavHostController,
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
) {
    composable(AppDestination.AntiVirus.route) {
        val viewModel: VirusScanViewModel = koinViewModel()
        AntiVirusScreen(
            navigator = navigator,
            dependencies = dependencies,
            viewModel = viewModel,
        )
    }

    composable(AppDestination.VirusQuickScan.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.antiVirusViewModelOwnerOr(backStackEntry)
        }
        val viewModel: VirusScanViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        QuickScanVirusScreen(
            navigator = navigator,
            viewModel = viewModel,
        )
    }

    composable(AppDestination.VirusDeepScan.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry, navController) {
            navController.antiVirusViewModelOwnerOr(backStackEntry)
        }
        val viewModel: VirusScanViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        DeepScanVirusScreen(
            navigator = navigator,
            viewModel = viewModel,
        )
    }

    composable(AppDestination.VirusResult.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.antiVirusViewModelOwnerOr(backStackEntry)
        }
        val viewModel: VirusScanViewModel = koinViewModel(viewModelStoreOwner = parentEntry)
        ScanVirusResultScreen(
            navigator = navigator,
            dependencies = dependencies,
            viewModel = viewModel,
        )
    }

    composable(AppDestination.NoVirusResult.route) {
        NoVirusResultScreen(navigator, dependencies.operations)
    }
}

private fun NavHostController.antiVirusViewModelOwnerOr(
    fallback: NavBackStackEntry,
): NavBackStackEntry =
    runCatching { getBackStackEntry(AppDestination.AntiVirus.route) }.getOrDefault(fallback)
