package com.quickcleanpro.phonecleaner.feature.toolbox.appusage

import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.*


import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.common.permission.PermissionType
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.AppUsageEffect
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.AppUsageViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.ui.AppUsageScreen

@Composable
fun AppUsageRoute(
    navigator: AppNavigator,
    viewModel: AppUsageViewModel,
    featureFlow: FeatureFlowRuntime,
    externalActivities: ExternalActivityLauncher,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val permissionCoordinator = LocalPermissionCoordinator.current
    LaunchedEffect(viewModel, context, externalActivities) {
        viewModel.effects.collect { effect ->
            when (effect) {
                    is AppUsageEffect.OpenAppInfo -> {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${effect.packageName}"),
                        )
                        try {
                            externalActivities.markLaunch()
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            externalActivities.cancelLaunch()
                        } catch (_: Exception) {
                            externalActivities.cancelLaunch()
                        }
                    }
                    is AppUsageEffect.Exit -> {
                        val reason = when (effect.reason) {
                            AppUsageExitReason.Return -> FeatureExitReason.Return
                            AppUsageExitReason.PermissionRejected -> FeatureExitReason.PermissionRejected
                        }
                        featureFlow.exit(FeatureKey.APP_USAGE, reason) { navigator.back() }
                    }
                }
            }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.onAction(AppUsageAction.Resumed)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.permissionRequestPending) {
        if (uiState.permissionRequestPending) {
            viewModel.onAction(AppUsageAction.PermissionRequestConsumed)
            permissionCoordinator.request(
                item = PermissionType.UsageAccess,
                onGranted = { viewModel.onAction(AppUsageAction.PermissionGranted) },
                onRejected = { viewModel.onAction(AppUsageAction.PermissionRejected) },
            )
        }
    }

    BackHandler { viewModel.onAction(AppUsageAction.Back) }

    AppUsageScreen(
        state = uiState,
        onAction = viewModel::onAction,
    )
}
