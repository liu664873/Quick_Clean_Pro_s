package com.quickcleanpro.phonecleaner.use.skin.junkclean.screens

import com.quickcleanpro.phonecleaner.use.feature.junkclean.presentation.JunkCleanViewModel

import com.quickcleanpro.phonecleaner.use.feature.junkclean.presentation.JunkCleanPhase

import com.quickcleanpro.phonecleaner.use.feature.junkclean.presentation.JunkCleanEvent

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationTracker
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.common.operation.trackReturnHome
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.StopScanDialog
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionCoordinator
import com.quickcleanpro.phonecleaner.use.skin.junkclean.screens.views.JunkCleanContentView
import com.quickcleanpro.phonecleaner.use.skin.junkclean.screens.views.JunkScanResultBottomBar

@Composable
fun JunkCleanScreen(
    viewModel: JunkCleanViewModel,
    permissionCoordinator: CleanXPermissionCoordinator,
    operationTracker: FeatureOperationTracker,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateHomeAfterComplete: () -> Unit = onNavigateHome,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tracker = operationTracker
    var showStopDialog by remember { mutableStateOf(false) }
    var finishAdInFlight by remember { mutableStateOf(false) }

    val deleteAuthorizationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            viewModel.handleAuthorizationResult(result.resultCode == Activity.RESULT_OK)
        }

    LaunchedEffect(viewModel, permissionCoordinator) {
        permissionCoordinator.guard(
            action = CleanXProtectedAction.JunkStartScan,
            onGranted = {
                viewModel.startScanIfNeeded()
            },
            onRejected = {
                tracker.trackWithAd(FeatureOperationEvent.PermissionRejected(FeatureKey.JUNK_CLEAN)) {
                    viewModel.clearResult()
                    onNavigateHome()
                }
            },
        )
    }

    LaunchedEffect(viewModel, tracker) {
        viewModel.operationEvents.collect { event ->
            if (event.isJunkCleanCompletionSuccess()) {
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

    fun exitBackWithReturnAd() {
        tracker.trackReturnHome(FeatureKey.JUNK_CLEAN) {
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
            tracker.trackWithAd(FeatureOperationEvent.ReturnHome(FeatureKey.JUNK_CLEAN)) {
                viewModel.clearResult()
                onNavigateHomeAfterComplete()
            }
        } else {
            viewModel.clearResult()
            onNavigateHome()
        }
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == JunkCleanPhase.Complete) {
            viewModel.markResultShown()
        }
    }

    fun handleBack() {
        if (finishAdInFlight) return
        when (uiState.phase) {
            JunkCleanPhase.Scanning -> {
                viewModel.cancelActiveOperation()
                showStopDialog = true
            }
            JunkCleanPhase.Cleaning -> {
                viewModel.cancelCleaningAndReturnToPreview()
                showStopDialog = true
            }
            JunkCleanPhase.CompleteAnimation,
            JunkCleanPhase.Complete -> exitToHome(showCompletionAd = true)
            else -> exitBackWithReturnAd()
        }
    }

    BackHandler(onBack = ::handleBack)

    CleanXScaffoldPage(
        title = stringResource(R.string.junk_removal),
        onBack = ::handleBack,
        scrollEnabled = false,
        contentPadding = PaddingValues(0.dp),
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFE3ECFD), Color(0xFFDFEBF5)),
        ),
        bottomBar = {
            if (uiState.phase == JunkCleanPhase.Preview) {
                JunkScanResultBottomBar(
                    selectedSummary = uiState.selectedSummary,
                    onClean = {
                        permissionCoordinator.guard(CleanXProtectedAction.JunkCleanSelected) {
                            viewModel.startCleaning(context)
                        }
                    },
                )
            }
        },
    ) {
        JunkCleanContentView(
            uiState = uiState,
            onToggleCategorySelection = viewModel::toggleCategorySelection,
            onToggleItem = { item -> viewModel.toggleItemSelection(item.junkFile.id) },
            onContinueFromResult = { exitToHome(showCompletionAd = true) },
        )
    }

    if (showStopDialog) {
        StopScanDialog(
            onQuit = {
                showStopDialog = false
                viewModel.cancelActiveOperation()
                exitBackWithReturnAd()
            },
            onResume = {
                showStopDialog = false
                if (uiState.phase == JunkCleanPhase.Scanning) {
                    viewModel.startScanIfNeeded()
                }
            },
        )
    }
}

private fun FeatureOperationEvent.isJunkCleanCompletionSuccess(): Boolean =
    this is FeatureOperationEvent.OperationFinished &&
        feature == FeatureKey.JUNK_CLEAN &&
        action == OperationAction.CLEAN &&
        success
