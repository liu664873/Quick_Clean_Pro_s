package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkspeed

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
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.common.operation.exitHandler
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedPhase
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedViewModel
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies

@Composable
fun NetworkSpeedRoute(
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
    viewModel: NetworkSpeedViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tracker = dependencies.operations
    val featureExit = tracker.exitHandler()
    var finishAdInFlight by remember { mutableStateOf(false) }

    fun handleBack() {
        if (finishAdInFlight || uiState.phase == NetworkSpeedPhase.Completing) return
        if (uiState.phase == NetworkSpeedPhase.Testing) viewModel.stopSpeedTest()
        featureExit.exitBack(FeatureKey.NETWORK_SPEED) { navigator.back() }
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

    LaunchedEffect(viewModel, tracker) {
        viewModel.operationEvents.collect { event ->
            if (event.isNetworkSpeedCompletionSuccess()) {
                if (!finishAdInFlight) {
                    finishAdInFlight = true
                    tracker.trackWithAd(event) {
                        finishAdInFlight = false
                        viewModel.showResultAfterCompletionAd()
                    }
                }
            } else {
                tracker.trackWithAd(event) {}
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
