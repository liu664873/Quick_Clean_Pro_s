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
    val surface =
        when {
            currentRoute == null || currentRoute == AppDestination.Splash.route ->
                NotificationPermissionSurface.Splash
            currentRoute in AppDestination.homeRoutes -> NotificationPermissionSurface.Home
            else -> NotificationPermissionSurface.Other
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onSystemPermissionResult(
                granted = granted,
                shouldShowRationale = context.shouldShowPostNotificationsRationale(),
            )
        }

    LaunchedEffect(surface) {
        viewModel.onSurfaceChanged(
            surface = surface,
            shouldShowRationale = context.shouldShowPostNotificationsRationale(),
        )
    }
    LaunchedEffect(surface) {
        if (surface == NotificationPermissionSurface.Home) {
            delay(HOME_NOTIFICATION_PERMISSION_PROMPT_DELAY_MILLIS)
            viewModel.onHomePromptDelayElapsed()
        }
    }
    LaunchedEffect(viewModel, context, externalActivityLauncher) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationPermissionCommand.RequestSystemPermission ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                NotificationPermissionCommand.OpenAppSettings -> {
                    externalActivityLauncher.markLaunch()
                    val launched = runCatching { context.startActivity(appSettingsIntent(context)) }
                        .onFailure { externalActivityLauncher.cancelLaunch() }
                        .isSuccess
                    viewModel.onSettingsLaunchResult(launched)
                }
                NotificationPermissionCommand.NotifyPermissionGranted -> latestOnPermissionGranted()
            }
        }
    }
    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = LifecycleEventObserver { _, event ->
            val current = latestState
            if (event == Lifecycle.Event.ON_RESUME &&
                current.surface == NotificationPermissionSurface.Home
            ) {
                viewModel.onAppResumed(context.shouldShowPostNotificationsRationale())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.customDialogVisible &&
        state.surface == NotificationPermissionSurface.Home &&
        !state.hasPermission
    ) {
        NotificationPermissionDialog(
            onConfirm = viewModel::onCustomPromptConfirmed,
            onDismiss = viewModel::onCustomPromptDismissed,
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
