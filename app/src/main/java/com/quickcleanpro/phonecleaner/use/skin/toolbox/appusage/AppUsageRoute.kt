package com.quickcleanpro.phonecleaner.use.skin.toolbox.appusage

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.permission.PermissionType
import com.quickcleanpro.phonecleaner.use.core.common.operation.exitHandler
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.appusage.AppUsageEffect
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.appusage.AppUsageViewModel
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import kotlinx.coroutines.delay

private const val TOOLBOX_SCAN_DURATION_MILLIS = 1_500L

@Composable
fun AppUsageRoute(
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
    viewModel: AppUsageViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val featureExit = dependencies.operations.exitHandler()
    var hasRequestedUsageAccess by remember { mutableStateOf(false) }
    var showScanning by remember { mutableStateOf(uiState.hasAccess) }
    var showStopDialog by remember { mutableStateOf(false) }
    var scanSession by remember { mutableStateOf(if (uiState.hasAccess) 1 else 0) }
    var scanDelayFinished by remember { mutableStateOf(false) }

    fun startScanningIfAccessGranted() {
        if (!viewModel.uiState.value.hasAccess) return
        showScanning = true
        showStopDialog = false
        scanDelayFinished = false
        scanSession += 1
    }

    fun exitAfterPermissionRejected() {
        featureExit.exitAfterPermissionRejected(FeatureKey.APP_USAGE) { navigator.back() }
    }

    fun handleBack() {
        when {
            uiState.hasAccess && showScanning -> showStopDialog = true
            !uiState.hasAccess -> exitAfterPermissionRejected()
            else -> featureExit.exitBack(FeatureKey.APP_USAGE) { navigator.back() }
        }
    }

    LaunchedEffect(viewModel, context, dependencies.externalActivities) {
        viewModel.effects.collect { effect ->
            val intent =
                when (effect) {
                    is AppUsageEffect.OpenAppInfo ->
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${effect.packageName}"),
                        )
                    AppUsageEffect.OpenUsageSettings -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                }
            try {
                dependencies.externalActivities.markLaunch()
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                dependencies.externalActivities.cancelLaunch()
            } catch (_: Exception) {
                dependencies.externalActivities.cancelLaunch()
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshAfterResume()
                    startScanningIfAccessGranted()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.hasAccess) {
        if (uiState.hasAccess) {
            startScanningIfAccessGranted()
        } else {
            showScanning = false
            showStopDialog = false
            scanDelayFinished = false
        }
    }

    LaunchedEffect(scanSession) {
        if (scanSession <= 0) return@LaunchedEffect
        delay(TOOLBOX_SCAN_DURATION_MILLIS)
        scanDelayFinished = true
    }

    LaunchedEffect(uiState.hasAccess, uiState.isLoading, showScanning, showStopDialog, scanDelayFinished) {
        if (uiState.hasAccess && showScanning && !showStopDialog && scanDelayFinished && !uiState.isLoading) {
            showScanning = false
        }
    }

    LaunchedEffect(uiState.hasAccess) {
        if (!uiState.hasAccess && !hasRequestedUsageAccess) {
            hasRequestedUsageAccess = true
            dependencies.permissions.request(
                item = PermissionType.UsageAccess,
                onGranted = viewModel::refreshAfterResume,
                onRejected = ::exitAfterPermissionRejected,
            )
        }
    }

    BackHandler(onBack = ::handleBack)

    AppUsageScreen(
        uiState = uiState,
        showScanning = showScanning,
        showStopDialog = showStopDialog,
        onBack = ::handleBack,
        onRangeSelected = viewModel::selectRange,
        onTabSelected = viewModel::selectTab,
        onStopApp = viewModel::openAppInfo,
        onQuitScan = {
            showStopDialog = false
            featureExit.exitBack(FeatureKey.APP_USAGE) { navigator.back() }
        },
        onResumeScan = {
            showStopDialog = false
            scanDelayFinished = false
            scanSession += 1
        },
    )
}
