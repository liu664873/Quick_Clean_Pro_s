package com.quickcleanpro.phonecleaner.feature.junkclean

import com.quickcleanpro.phonecleaner.feature.junkclean.*

import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanViewModel

import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanPhase

import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanEvent

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import com.quickcleanpro.phonecleaner.common.permission.ProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.AppPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.junkclean.ui.JunkCleanScreen

@Composable
fun JunkCleanRoute(
    viewModel: JunkCleanViewModel,
    permissionCoordinator: AppPermissionCoordinator,
    featureFlow: FeatureFlowRuntime,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateHomeAfterComplete: () -> Unit = onNavigateHome,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var finishAdInFlight by remember { mutableStateOf(false) }

    val deleteAuthorizationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            viewModel.handleAuthorizationResult(result.resultCode == Activity.RESULT_OK)
        }

    LaunchedEffect(viewModel, permissionCoordinator) {
        permissionCoordinator.ensure(
            action = ProtectedAction.JunkStartScan,
            onGranted = {
                viewModel.startScanIfNeeded()
            },
            onDenied = {
                featureFlow.exit(FeatureKey.JUNK_CLEAN, FeatureExitReason.PermissionRejected) {
                    viewModel.clearResult()
                    onNavigateHome()
                }
            },
        )
    }

    LaunchedEffect(viewModel, featureFlow) {
        viewModel.operationEvents.collect { event ->
            if (event.isJunkCleanCompletionSuccess()) {
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

    fun exitBackWithReturnAd() {
        featureFlow.exit(FeatureKey.JUNK_CLEAN, FeatureExitReason.Return) {
            onNavigateBack()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is JunkCleanEvent.RequestDeleteAuthorization -> {
                    deleteAuthorizationLauncher.launch(
                        IntentSenderRequest.Builder(event.deleteRequest.intentSender).build(),
                    )
                }
            }
        }
    }

    fun exitToHome(showCompletionAd: Boolean = false) {
        if (showCompletionAd) {
            featureFlow.exit(FeatureKey.JUNK_CLEAN, FeatureExitReason.Return) {
                viewModel.clearResult()
                onNavigateHomeAfterComplete()
            }
        } else {
            viewModel.clearResult()
            onNavigateHome()
        }
    }

    fun handleBack() {
        if (finishAdInFlight) return
        when (uiState.phase) {
            JunkCleanPhase.Scanning -> {
                viewModel.requestStopDialog()
            }
            JunkCleanPhase.Cleaning -> {
                viewModel.requestStopDialog()
            }
            JunkCleanPhase.CompleteAnimation,
            JunkCleanPhase.Complete -> exitToHome(showCompletionAd = true)
            else -> exitBackWithReturnAd()
        }
    }

    JunkCleanScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                JunkCleanUiAction.Back -> handleBack()
                JunkCleanUiAction.CleanSelected -> {
                    permissionCoordinator.ensure(ProtectedAction.JunkCleanSelected) {
                        viewModel.startCleaning(context)
                    }
                }
                is JunkCleanUiAction.ToggleCategories -> viewModel.toggleCategorySelection(action.categories)
                is JunkCleanUiAction.ToggleItem -> viewModel.toggleItemSelection(action.itemId)
                JunkCleanUiAction.ContinueFromResult -> exitToHome(showCompletionAd = true)
                JunkCleanUiAction.QuitStoppedOperation -> {
                    viewModel.dismissStopDialog(resume = false)
                    exitBackWithReturnAd()
                }
                JunkCleanUiAction.ResumeStoppedOperation -> viewModel.dismissStopDialog(resume = true)
            }
        },
    )
}

private fun FeatureOperationEvent.isJunkCleanCompletionSuccess(): Boolean =
    this is FeatureOperationEvent.OperationFinished &&
        feature == FeatureKey.JUNK_CLEAN &&
        action == OperationAction.CLEAN &&
        success
