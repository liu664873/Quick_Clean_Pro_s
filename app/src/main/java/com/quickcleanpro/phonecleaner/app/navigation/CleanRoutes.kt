package com.quickcleanpro.phonecleaner.app.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanRoute
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerCleanRoutes(
    navigator: AppNavigator,
    featureFlow: FeatureFlowRuntime,
) {
    composable(AppDestination.JunkClean.route) {
        val viewModel: JunkCleanViewModel = koinViewModel()
        JunkCleanRoute(
            viewModel = viewModel,
            permissionCoordinator = LocalPermissionCoordinator.current,
            featureFlow = featureFlow,
            onNavigateBack = { navigator.back() },
            onNavigateHome = { navigator.home() },
            onNavigateHomeAfterComplete = { navigator.home() },
        )
    }
}
