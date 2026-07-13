package com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.common.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner.WhatsAppCleanerAction
import com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner.WhatsAppCleanerViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner.ui.WhatsAppCleanerScreen

@Composable
fun WhatsAppCleanerRoute(
    navigator: AppNavigator,
    viewModel: WhatsAppCleanerViewModel,
    featureFlow: FeatureFlowRuntime,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionCoordinator = LocalPermissionCoordinator.current

    fun exitToHomeAfterPermissionRejected() {
        featureFlow.exit(FeatureKey.WHATSAPP_CLEANER, FeatureExitReason.PermissionRejected) {
            viewModel.onAction(WhatsAppCleanerAction.ClearResult)
            navigator.home()
        }
    }

    LaunchedEffect(viewModel, permissionCoordinator) {
        permissionCoordinator.guard(
            action = CleanXProtectedAction.WhatsAppStartScan,
            onGranted = { viewModel.onAction(WhatsAppCleanerAction.StartScan) },
            onRejected = ::exitToHomeAfterPermissionRejected,
        )
    }

    LaunchedEffect(viewModel, featureFlow) {
        viewModel.operationEvents.collect { event ->
            if (event.isWhatsAppCompletionSuccess()) {
                if (!viewModel.uiState.value.completionAdInFlight) {
                    viewModel.onAction(WhatsAppCleanerAction.CompletionAdStarted)
                    featureFlow.handleOperation(event) {
                        viewModel.onAction(WhatsAppCleanerAction.CompletionAdFinished)
                        viewModel.onAction(WhatsAppCleanerAction.ShowResultAfterCompletionAd)
                    }
                }
            } else {
                featureFlow.handleOperation(event)
            }
        }
    }

    WhatsAppCleanerScreen(
        state = state,
        onAction = { action ->
            when (action) {
                WhatsAppCleanerAction.Back -> {
                    featureFlow.exit(FeatureKey.WHATSAPP_CLEANER, FeatureExitReason.Return) { navigator.back() }
                }
                WhatsAppCleanerAction.ExitAfterComplete -> {
                    featureFlow.exit(FeatureKey.WHATSAPP_CLEANER, FeatureExitReason.Return) {
                        viewModel.onAction(WhatsAppCleanerAction.ClearResult)
                        navigator.home()
                    }
                }
                WhatsAppCleanerAction.CleanSelected -> {
                    permissionCoordinator.guard(CleanXProtectedAction.WhatsAppCleanSelected) {
                        viewModel.onAction(action)
                    }
                }
                else -> viewModel.onAction(action)
            }
        },
        onNavigate = navigator::open,
    )
}

private fun FeatureOperationEvent.isWhatsAppCompletionSuccess(): Boolean =
    this is FeatureOperationEvent.OperationFinished &&
        feature == FeatureKey.WHATSAPP_CLEANER &&
        action == OperationAction.CLEAN &&
        success
