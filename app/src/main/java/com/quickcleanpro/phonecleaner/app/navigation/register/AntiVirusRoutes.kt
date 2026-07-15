package com.quickcleanpro.phonecleaner.app.navigation.register

import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.feature.antivirus.AntiVirusRoute
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.DeepScanVirusScreen
import com.quickcleanpro.phonecleaner.feature.antivirus.NoVirusResultRoute
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.QuickScanVirusScreen
import com.quickcleanpro.phonecleaner.feature.antivirus.ScanVirusResultRoute
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerAntiVirusRoutes(
    navController: NavHostController,
    navigator: AppNavigator,
    featureFlow: FeatureFlowRuntime,
    externalActivities: ExternalActivityLauncher,
) {
    composable(AppDestination.AntiVirus.route) {
        val viewModel: VirusScanViewModel = koinViewModel()
        AntiVirusRoute(
            navigator = navigator,
            viewModel = viewModel,
            featureFlow = featureFlow,
            externalActivities = externalActivities,
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
        ScanVirusResultRoute(
            navigator = navigator,
            viewModel = viewModel,
            externalActivities = externalActivities,
        )
    }

    composable(AppDestination.NoVirusResult.route) {
        NoVirusResultRoute(navigator)
    }
}

private fun NavHostController.antiVirusViewModelOwnerOr(
    fallback: NavBackStackEntry,
): NavBackStackEntry =
    runCatching { getBackStackEntry(AppDestination.AntiVirus.route) }.getOrDefault(fallback)
