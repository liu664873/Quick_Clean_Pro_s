package com.quickcleanpro.phonecleaner.use.skin.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.use.skin.junkclean.screens.JunkCleanScreen
import com.quickcleanpro.phonecleaner.use.feature.junkclean.presentation.JunkCleanViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerCleanRoutes(navigator: AppNavigator, dependencies: AppRouteDependencies) {
    composable(AppDestination.JunkClean.route) {
        val viewModel: JunkCleanViewModel = koinViewModel()
        JunkCleanScreen(
            viewModel = viewModel,
            permissionCoordinator = dependencies.permissions,
            operationTracker = dependencies.operations,
            onNavigateBack = { navigator.back() },
            onNavigateHome = { navigator.home() },
            onNavigateHomeAfterComplete = { navigator.home() },
        )
    }
}
