package com.quickcleanpro.phonecleaner.use.skin.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.core.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.feature.destinationOrNull
import com.quickcleanpro.phonecleaner.use.feature.home.presentation.HomeViewModel

@Composable
fun HomeRoute(
    navigator: AppNavigator,
    viewModel: HomeViewModel,
    externalBlockingPromptActive: Boolean = false,
    initialTabIndex: Int = 0,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val summaryState by viewModel.summaryState.collectAsStateWithLifecycle()
    val exitPromptSpec = viewModel.exitPromptSpec
    val showAutoRateDialog = viewModel.showAutoRateDialog

    BackHandler(
        enabled = exitPromptSpec == null && !externalBlockingPromptActive && !showAutoRateDialog,
        onBack = viewModel::requestExitPrompt,
    )

    DisposableEffect(lifecycleOwner, viewModel) {
        var skipInitialResume = true
        val observer =
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
                if (skipInitialResume) {
                    skipInitialResume = false
                    return@LifecycleEventObserver
                }
                viewModel.refreshSummary()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(externalBlockingPromptActive) {
        viewModel.setExternalBlockingPromptActive(externalBlockingPromptActive)
    }

    HomeScreen(
        summaryState = summaryState,
        exitPromptSpec = exitPromptSpec,
        showAutoRateDialog = showAutoRateDialog,
        externalBlockingPromptActive = externalBlockingPromptActive,
        initialTabIndex = initialTabIndex,
        onExit = {
            viewModel.dismissExitPrompt()
            context.findActivity()?.finish()
        },
        onViewExitPromptFeature = {
            viewModel.consumeExitPromptForNavigation()?.let { navigator.openRoute(it.route) }
        },
        onSettingsClick = {
            viewModel.onFeatureClicked()
            navigator.open(AppDestination.Settings)
        },
        onFeatureClick = { feature -> openFeature(feature, viewModel, navigator) },
        onTabInteraction = viewModel::onTabInteraction,
        onDismissAutoRateDialog = viewModel::dismissAutoRateDialog,
    )
}

private fun openFeature(
    feature: FeatureKey,
    viewModel: HomeViewModel,
    navigator: AppNavigator,
) {
    val destination = feature.destinationOrNull() ?: return
    viewModel.onFeatureClicked()
    AnalyticsTracker.trackCoreFeatureClicked(feature)
    navigator.open(destination)
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
