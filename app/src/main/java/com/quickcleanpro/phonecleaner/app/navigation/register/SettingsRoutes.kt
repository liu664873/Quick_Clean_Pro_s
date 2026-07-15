package com.quickcleanpro.phonecleaner.app.navigation.register

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.settings.ManagePermissionsRoute
import com.quickcleanpro.phonecleaner.feature.settings.SettingsRoute
import com.quickcleanpro.phonecleaner.feature.settings.ManagePermissionsViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerSettingsRoutes(
    navigator: AppNavigator,
    externalActivities: ExternalActivityLauncher,
) {
    composable(AppDestination.Settings.route) {
        SettingsRoute(navigator = navigator, externalActivities = externalActivities)
    }
    composable(AppDestination.ManagePermissions.route) {
        ManagePermissionsRoute(
            navigator = navigator,
            permissionCoordinator = LocalPermissionCoordinator.current,
            viewModel = koinViewModel<ManagePermissionsViewModel>(),
        )
    }
}
