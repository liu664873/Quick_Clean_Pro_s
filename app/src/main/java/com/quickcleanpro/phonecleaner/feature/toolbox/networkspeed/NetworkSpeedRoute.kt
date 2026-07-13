package com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed

import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.*


import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.NetworkSpeedPhase
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.NetworkSpeedViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.ui.NetworkSpeedScreen

@Composable
fun NetworkSpeedRoute(
    navigator: AppNavigator,
    viewModel: NetworkSpeedViewModel,
    featureFlow: FeatureFlowRuntime,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var finishAdInFlight by remember { mutableStateOf(false) }

    fun handleBack() {
        if (finishAdInFlight || uiState.phase == NetworkSpeedPhase.Completing) return
        if (uiState.phase == NetworkSpeedPhase.Testing) viewModel.stopSpeedTest()
        featureFlow.exit(FeatureKey.NETWORK_SPEED, FeatureExitReason.Return) { navigator.back() }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshNetworkStateUntilNetworkAvailable()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, featureFlow) {
        viewModel.operationEvents.collect { event ->
            if (event.isNetworkSpeedCompletionSuccess()) {
                if (!finishAdInFlight) {
                    finishAdInFlight = true
                    featureFlow.handleOperation(event) {
                        finishAdInFlight = false
                        viewModel.showResultAfterCompletionAd()
                    }
                }
            } else {
                featureFlow.handleOperation(event)
            }
        }
    }

    BackHandler(onBack = ::handleBack)

    NetworkSpeedScreen(
        uiState = uiState,
        onBack = ::handleBack,
        onRunSpeedTest = viewModel::runSpeedTest,
        onStopSpeedTest = viewModel::stopSpeedTest,
        onNavigateTool = navigator::resetTo,
    )
}

private fun FeatureOperationEvent.isNetworkSpeedCompletionSuccess(): Boolean =
    this is FeatureOperationEvent.OperationFinished &&
        feature == FeatureKey.NETWORK_SPEED &&
        action == OperationAction.TEST &&
        success
