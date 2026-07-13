package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan

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
import com.quickcleanpro.phonecleaner.use.core.common.operation.exitHandler
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanPhase
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanViewModel
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies

@Composable
fun NetworkScanRoute(
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
    viewModel: NetworkScanViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val featureExit = dependencies.operations.exitHandler()

    fun handleBack() {
        if (uiState.phase == NetworkScanPhase.Scanning) viewModel.cancelScan()
        featureExit.exitBack(FeatureKey.NETWORK_SCAN) { navigator.back() }
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
                    onLaunchingSettings = dependencies.externalActivities.markLaunch,
                    onSettingsLaunchFailed = dependencies.externalActivities.cancelLaunch,
                )
            if (!openedSettings) dependencies.externalActivities.cancelLaunch()
            viewModel.refreshNetworkStateUntilWifiConnected()
        },
        onScan = {
            dependencies.permissions.guardDirect(CleanXProtectedAction.NetworkScanStart) {
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
