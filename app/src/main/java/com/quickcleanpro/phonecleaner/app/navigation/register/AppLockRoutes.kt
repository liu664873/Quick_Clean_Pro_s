package com.quickcleanpro.phonecleaner.app.navigation.register

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.feature.applock.AppLockRoute
import com.quickcleanpro.phonecleaner.feature.applock.AppLockViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerAppLockRoutes(
    navigator: AppNavigator,
    featureFlow: FeatureFlowRuntime,
) {
    composable(AppDestination.AppLock.route) {
        val viewModel: AppLockViewModel = koinViewModel()
        AppLockRoute(navigator = navigator, viewModel = viewModel, featureFlow = featureFlow)
    }
}
