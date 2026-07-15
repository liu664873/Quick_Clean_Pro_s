package com.quickcleanpro.phonecleaner.feature.toolbox.networkscan

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.permission.ProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.PermissionPromptMode
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanPhase
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui.NetworkScanScreen

@Composable
fun NetworkScanRoute(
    navigator: AppNavigator,
    viewModel: NetworkScanViewModel,
    featureFlow: FeatureFlowRuntime,
    externalActivities: ExternalActivityLauncher,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionCoordinator = LocalPermissionCoordinator.current

    fun handleBack() {
        if (uiState.phase == NetworkScanPhase.Scanning) viewModel.cancelScan()
        featureFlow.exit(FeatureKey.NETWORK_SCAN, FeatureExitReason.Return) { navigator.back() }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshNetworkStateUntilWifiConnected()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(onBack = ::handleBack)

    NetworkScanScreen(
        uiState = uiState,
        onBack = ::handleBack,
        onRefreshWifi = viewModel::refreshNetworkStateUntilWifiConnected,
        onSwitchWifi = {
            val openedSettings =
                openWifiSettings(
                    context = context,
                    onLaunchingSettings = externalActivities.markLaunch,
                    onSettingsLaunchFailed = externalActivities.cancelLaunch,
                )
            if (!openedSettings) externalActivities.cancelLaunch()
            viewModel.refreshNetworkStateUntilWifiConnected()
        },
        onScan = {
            permissionCoordinator.ensure(
                ProtectedAction.NetworkScanStart,
                mode = PermissionPromptMode.Direct,
            ) {
                viewModel.startScan()
            }
        },
        onOpenDevices = { navigator.open(AppDestination.NetworkScanDevices) },
    )
}

private fun openWifiSettings(
    context: Context,
    onLaunchingSettings: () -> Unit,
    onSettingsLaunchFailed: () -> Unit,
): Boolean {
    val intents =
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Intent(Settings.Panel.ACTION_WIFI))
            add(Intent(Settings.ACTION_WIFI_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }
    intents.forEach { intent ->
        try {
            onLaunchingSettings()
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            onSettingsLaunchFailed()
        } catch (_: Exception) {
            onSettingsLaunchFailed()
        }
    }
    return false
}
