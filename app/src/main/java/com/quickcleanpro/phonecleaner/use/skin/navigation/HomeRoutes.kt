package com.quickcleanpro.phonecleaner.use.skin.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.use.app.runtime.notification.ToolNotificationIntentFactory
import com.quickcleanpro.phonecleaner.use.skin.home.HomeRoute
import com.quickcleanpro.phonecleaner.use.feature.home.presentation.HomeViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerHomeRoutes(
    navigator: AppNavigator,
    externalBlockingPromptActive: Boolean = false,
    homeNotificationPermissionPrompt: @Composable () -> Unit = {},
) {
    composable(AppDestination.Home.route) {
        val viewModel: HomeViewModel = koinViewModel()
        HomeRoute(
            navigator = navigator,
            viewModel = viewModel,
            externalBlockingPromptActive = externalBlockingPromptActive,
        )
        homeNotificationPermissionPrompt()
    }
    composable(ToolNotificationIntentFactory.ROUTE_HOME_FILE_MANAGER) {
        val viewModel: HomeViewModel = koinViewModel()
        HomeRoute(
            navigator = navigator,
            viewModel = viewModel,
            externalBlockingPromptActive = externalBlockingPromptActive,
            initialTabIndex = 1,
        )
        homeNotificationPermissionPrompt()
    }
    composable(ToolNotificationIntentFactory.ROUTE_HOME_TOOLBOX) {
        val viewModel: HomeViewModel = koinViewModel()
        HomeRoute(
            navigator = navigator,
            viewModel = viewModel,
            externalBlockingPromptActive = externalBlockingPromptActive,
            initialTabIndex = 2,
        )
        homeNotificationPermissionPrompt()
    }
}
