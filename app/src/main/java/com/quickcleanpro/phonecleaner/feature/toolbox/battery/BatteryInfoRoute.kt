package com.quickcleanpro.phonecleaner.feature.toolbox.battery

import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.*

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.BatteryInfoViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.ui.BatteryInfoScreen

@Composable
fun BatteryInfoRoute(
    navigator: AppNavigator,
    viewModel: BatteryInfoViewModel,
    featureFlow: FeatureFlowRuntime,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, featureFlow, navigator) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BatteryInfoEffect.Exit ->
                    featureFlow.exit(FeatureKey.BATTERY_INFO, FeatureExitReason.Return) { navigator.back() }
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> viewModel.onAction(BatteryInfoAction.Resumed)
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    -> viewModel.onAction(BatteryInfoAction.Paused)
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAction(BatteryInfoAction.Paused)
        }
    }

    BackHandler { viewModel.onAction(BatteryInfoAction.Back) }

    BatteryInfoScreen(
        state = uiState,
        onAction = viewModel::onAction,
    )
}
