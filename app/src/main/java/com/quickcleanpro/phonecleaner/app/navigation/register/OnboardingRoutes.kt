package com.quickcleanpro.phonecleaner.app.navigation.register

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.common.ads.AdRuntime
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingRoute
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingScanViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerOnboardingRoute(
    navigator: AppNavigator,
    adRuntime: AdRuntime,
) {
    composable(AppDestination.OnboardingScan.route) {
        val viewModel: OnboardingScanViewModel = koinViewModel()
        OnboardingRoute(
            navigator = navigator,
            adRuntime = adRuntime,
            viewModel = viewModel,
        )
    }
}
