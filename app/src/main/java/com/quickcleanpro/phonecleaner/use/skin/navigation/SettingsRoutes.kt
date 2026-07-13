package com.quickcleanpro.phonecleaner.use.skin.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.use.skin.settings.ManagePermissionsScreen
import com.quickcleanpro.phonecleaner.use.skin.settings.SettingsRoute
import com.quickcleanpro.phonecleaner.use.feature.settings.presentation.ManagePermissionsViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerSettingsRoutes(navigator: AppNavigator, dependencies: AppRouteDependencies) {
    composable(AppDestination.Settings.route) {
        SettingsRoute(
            navigator = navigator,
            externalActivityLaunchHandler = dependencies.externalActivities,
        )
    }
    composable(AppDestination.ManagePermissions.route) {
        ManagePermissionsScreen(
            permissionCoordinator = dependencies.permissions,
            viewModel = koinViewModel<ManagePermissionsViewModel>(),
        )
    }
}
