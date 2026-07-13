package com.quickcleanpro.phonecleaner.use.skin.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.use.skin.applock.AppLockRoute
import com.quickcleanpro.phonecleaner.use.feature.applock.presentation.AppLockViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerAppLockRoutes(navigator: AppNavigator, dependencies: AppRouteDependencies) {
    composable(AppDestination.AppLock.route) {
        val viewModel: AppLockViewModel = koinViewModel()
        AppLockRoute(navigator = navigator, dependencies = dependencies, viewModel = viewModel)
    }
}
