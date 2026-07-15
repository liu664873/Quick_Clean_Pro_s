package com.quickcleanpro.phonecleaner.feature.applock

import com.quickcleanpro.phonecleaner.feature.applock.AppLockPage
import com.quickcleanpro.phonecleaner.feature.applock.AppLockPinStep
import com.quickcleanpro.phonecleaner.feature.applock.AppLockAction
import com.quickcleanpro.phonecleaner.feature.applock.AppLockUiState
import com.quickcleanpro.phonecleaner.feature.applock.AppLockViewModel
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXIconButtonSize
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXPillShape
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.common.permission.ProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator


import com.quickcleanpro.phonecleaner.feature.applock.ui.AppLockScreen

@Composable
internal fun AppLockRoute(
    navigator: AppNavigator,
    viewModel: AppLockViewModel,
    featureFlow: FeatureFlowRuntime,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val permissionCoordinator = LocalPermissionCoordinator.current
    val toastRes = uiState.toastRes
    val toastMessage = toastRes?.let { stringResource(it) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(AppLockAction.RefreshAfterResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            viewModel.onAction(AppLockAction.ConsumeToast)
        }
    }

    LaunchedEffect(uiState.overlayPermissionRequired) {
        if (uiState.overlayPermissionRequired) {
            permissionCoordinator.ensure(
                action = ProtectedAction.AppLockRequestOverlay,
                onGranted = { viewModel.onAction(AppLockAction.ConsumeOverlayPermissionRequest) },
                onDenied = { viewModel.onAction(AppLockAction.ConsumeOverlayPermissionRequest) },
            )
        }
    }

    fun exitBack() {
        featureFlow.exit(FeatureKey.APP_LOCK, FeatureExitReason.Return) { navigator.back() }
    }

    fun handleAction(action: AppLockAction) {
        when (action) {
            AppLockAction.Back -> {
                if (!viewModel.handleBack()) exitBack()
            }
            is AppLockAction.TogglePackage -> {
                if (uiState.page == AppLockPage.SelectApps) {
                    viewModel.onAction(action)
                } else {
                    permissionCoordinator.ensure(ProtectedAction.AppLockOpenProtectedArea) {
                        viewModel.onAction(action)
                    }
                }
            }
            AppLockAction.ToggleAllApps,
            AppLockAction.BeginCreatePin -> {
                permissionCoordinator.ensure(ProtectedAction.AppLockOpenProtectedArea) {
                    viewModel.onAction(action)
                }
            }
            is AppLockAction.MonitoringChanged -> {
                if (!action.enabled) {
                    viewModel.onAction(action)
                } else {
                permissionCoordinator.ensure(ProtectedAction.AppLockEnableMonitoring) {
                        viewModel.onAction(action)
                    }
                }
            }
            else -> viewModel.onAction(action)
        }
    }

    AppLockScreen(
        state = uiState,
        onAction = ::handleAction,
        onNavigate = navigator::open,
    )
}
