package com.quickcleanpro.phonecleaner.feature.notificationcleaner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.common.permission.ProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationCleanerAction
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationCleanerViewModel
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.ui.NotificationCleanerScreen

@Composable
fun NotificationCleanerRoute(
    navigator: AppNavigator,
    viewModel: NotificationCleanerViewModel,
    featureFlow: FeatureFlowRuntime,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionCoordinator = LocalPermissionCoordinator.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(NotificationCleanerAction.Refresh)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, featureFlow) {
        viewModel.operationEvents.collect { event ->
            if (event.isNotificationCleanerCompletionSuccess()) {
                if (!viewModel.uiState.value.completionAdInFlight) {
                    viewModel.onAction(NotificationCleanerAction.CompletionAdStarted)
                    featureFlow.handleOperation(event) {
                        viewModel.onAction(NotificationCleanerAction.CompletionAdFinished)
                        viewModel.onAction(NotificationCleanerAction.ShowStatusAfterCompletionAd)
                    }
                }
            } else {
                featureFlow.handleOperation(event)
            }
        }
    }

    NotificationCleanerScreen(
        state = state,
        onAction = { action ->
            when (action) {
                NotificationCleanerAction.Back -> {
                    if (!state.completionAdInFlight) {
                        featureFlow.exit(FeatureKey.NOTIFICATION_CLEANER, FeatureExitReason.Return) { navigator.back() }
                    }
                }
                NotificationCleanerAction.EnableBlocking -> {
                    permissionCoordinator.ensure(ProtectedAction.NotificationCleanerEnable) {
                        viewModel.onAction(action)
                    }
                }
                else -> viewModel.onAction(action)
            }
        },
        onNavigate = navigator::open,
    )
}

private fun FeatureOperationEvent.isNotificationCleanerCompletionSuccess(): Boolean =
    this is FeatureOperationEvent.OperationFinished &&
        feature == FeatureKey.NOTIFICATION_CLEANER &&
        action == OperationAction.CLEAN &&
        success
