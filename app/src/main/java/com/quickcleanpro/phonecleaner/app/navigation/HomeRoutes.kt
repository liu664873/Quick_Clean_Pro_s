package com.quickcleanpro.phonecleaner.app.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.runtime.notification.ToolNotificationIntentFactory
import com.quickcleanpro.phonecleaner.feature.home.HomeRoute
import com.quickcleanpro.phonecleaner.feature.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerHomeRoutes(
    navigator: AppNavigator,
    externalBlockingPromptActive: Boolean,
) {
    composable(AppDestination.Home.route) {
        val viewModel: HomeViewModel = koinViewModel()
        HomeRoute(
            navigator = navigator,
            viewModel = viewModel,
            externalBlockingPromptActive = externalBlockingPromptActive,
        )
    }
    composable(ToolNotificationIntentFactory.ROUTE_HOME_FILE_MANAGER) {
        val viewModel: HomeViewModel = koinViewModel()
        HomeRoute(
            navigator = navigator,
            viewModel = viewModel,
            externalBlockingPromptActive = externalBlockingPromptActive,
            initialTabIndex = 1,
        )
    }
    composable(ToolNotificationIntentFactory.ROUTE_HOME_TOOLBOX) {
        val viewModel: HomeViewModel = koinViewModel()
        HomeRoute(
            navigator = navigator,
            viewModel = viewModel,
            externalBlockingPromptActive = externalBlockingPromptActive,
            initialTabIndex = 2,
        )
    }
}
