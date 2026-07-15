package com.quickcleanpro.phonecleaner.app.runtime.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.common.permission.appSettingsIntent
import com.quickcleanpro.phonecleaner.common.ui.components.NotificationPermissionDialog
import kotlinx.coroutines.delay

@Composable
internal fun NotificationPermissionHost(
    viewModel: NotificationPermissionViewModel,
    currentRoute: String?,
    externalActivityLauncher: ExternalActivityLauncher,
    onPermissionGranted: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
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
                    shouldShowRationale = context.shouldShowPostNotificationsRationale(),
                ),
            )
        }

    LaunchedEffect(isSplashVisible, isHomeVisible) {
        viewModel.onAction(
            NotificationPermissionAction.VisibilityChanged(
                isSplashVisible = isSplashVisible,
                isHomeVisible = isHomeVisible,
                shouldShowRationale = context.shouldShowPostNotificationsRationale(),
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
    LaunchedEffect(viewModel, context, externalActivityLauncher) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationPermissionEffect.RequestSystemPermission ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                NotificationPermissionEffect.OpenAppSettings -> {
                    externalActivityLauncher.markLaunch()
                    val launched = runCatching { context.startActivity(appSettingsIntent(context)) }
                        .onFailure { externalActivityLauncher.cancelLaunch() }
                        .isSuccess
                    viewModel.onAction(NotificationPermissionAction.SettingsLaunchResult(launched))
                }
                NotificationPermissionEffect.NotifyPermissionGranted -> latestOnPermissionGranted()
            }
        }
    }
    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = LifecycleEventObserver { _, event ->
            val current = latestState
            if (event == Lifecycle.Event.ON_RESUME && current.isHomeVisible) {
                viewModel.onAction(
                    NotificationPermissionAction.Refresh(
                        returningFromSettings = current.settingsLaunchPending,
                        shouldShowRationale = context.shouldShowPostNotificationsRationale(),
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

private fun Context.shouldShowPostNotificationsRationale(): Boolean =
    findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
