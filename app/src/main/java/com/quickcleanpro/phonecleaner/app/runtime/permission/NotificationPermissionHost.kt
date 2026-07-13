package com.quickcleanpro.phonecleaner.app.runtime.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.common.permission.NotificationRuntimePermissionController
import com.quickcleanpro.phonecleaner.common.ui.components.NotificationPermissionDialog
import kotlinx.coroutines.delay

@Composable
internal fun NotificationPermissionHost(
    viewModel: NotificationPermissionViewModel,
    currentRoute: String?,
    permissionController: NotificationRuntimePermissionController,
    onPermissionGranted: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestState by rememberUpdatedState(state)
    val latestOnPermissionGranted by rememberUpdatedState(onPermissionGranted)
    val isSplashVisible = currentRoute == null || currentRoute == AppDestination.Splash.route
    val isHomeVisible = currentRoute in AppDestination.homeRoutes
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onAction(
                NotificationPermissionAction.PermissionResult(
                    granted = granted,
                    shouldShowRationale = permissionController.shouldShowPostNotificationsRationale(),
                ),
            )
        }

    LaunchedEffect(isSplashVisible, isHomeVisible) {
        viewModel.onAction(
            NotificationPermissionAction.VisibilityChanged(
                isSplashVisible = isSplashVisible,
                isHomeVisible = isHomeVisible,
                shouldShowRationale = permissionController.shouldShowPostNotificationsRationale(),
            ),
        )
    }
    LaunchedEffect(isHomeVisible) {
        if (isHomeVisible) {
            delay(HOME_NOTIFICATION_PERMISSION_PROMPT_DELAY_MILLIS)
            viewModel.onAction(NotificationPermissionAction.HomePromptDelayElapsed)
        }
    }
    LaunchedEffect(
        state.suppressHomePromptUntilMillis,
        state.isHomeVisible,
        state.hasPermission,
        state.requestSource,
    ) {
        val delayMillis = state.suppressHomePromptUntilMillis - System.currentTimeMillis()
        if (state.isHomeVisible &&
            !state.hasPermission &&
            state.requestSource == null &&
            delayMillis > 0L
        ) {
            delay(delayMillis)
            viewModel.onAction(NotificationPermissionAction.HomePromptCooldownElapsed)
        }
    }
    LaunchedEffect(viewModel, permissionController) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationPermissionEffect.RequestSystemPermission ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                NotificationPermissionEffect.OpenAppSettings -> {
                    val launched = permissionController.openAppSettings()
                    viewModel.onAction(NotificationPermissionAction.SettingsLaunchResult(launched))
                }
                NotificationPermissionEffect.NotifyPermissionGranted -> latestOnPermissionGranted()
            }
        }
    }
    DisposableEffect(lifecycleOwner, viewModel, permissionController) {
        val observer = LifecycleEventObserver { _, event ->
            val current = latestState
            if (event == Lifecycle.Event.ON_RESUME && current.isHomeVisible) {
                viewModel.onAction(
                    NotificationPermissionAction.Refresh(
                        returningFromSettings = current.settingsLaunchPending,
                        shouldShowRationale = permissionController.shouldShowPostNotificationsRationale(),
                    ),
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.customDialogVisible && state.isHomeVisible && !state.hasPermission) {
        NotificationPermissionDialog(
            onConfirm = { viewModel.onAction(NotificationPermissionAction.CustomPromptConfirmed) },
            onDismiss = { viewModel.onAction(NotificationPermissionAction.CustomPromptDismissed) },
        )
    }
}
