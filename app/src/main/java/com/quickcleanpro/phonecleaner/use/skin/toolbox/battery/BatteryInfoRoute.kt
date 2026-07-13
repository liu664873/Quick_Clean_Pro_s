package com.quickcleanpro.phonecleaner.use.skin.toolbox.battery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.core.common.operation.trackReturnHome
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.device.BatteryInfoViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.device.DeviceInfoMode
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import kotlinx.coroutines.delay

private const val INFO_SCAN_DURATION_MILLIS = 1_500L

@Composable
fun BatteryInfoRoute(
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
    viewModel: BatteryInfoViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showScanning by remember { mutableStateOf(true) }
    var showExitDialog by remember { mutableStateOf(false) }
    var scanSession by remember { mutableStateOf(0) }

    fun startPage() {
        showScanning = true
        showExitDialog = false
        scanSession += 1
        viewModel.load(DeviceInfoMode.Battery)
        viewModel.startBatterySampling()
    }

    fun exit() {
        dependencies.operations.trackReturnHome(FeatureKey.BATTERY_INFO) { navigator.back() }
    }

    fun handleBack() {
        if (showScanning) showExitDialog = true else exit()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        startPage()
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> startPage()
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    -> viewModel.stopBatterySampling()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopBatterySampling()
        }
    }

    LaunchedEffect(scanSession, showScanning, showExitDialog) {
        if (!showScanning || showExitDialog) return@LaunchedEffect
        delay(INFO_SCAN_DURATION_MILLIS)
        showScanning = false
    }

    BackHandler(onBack = ::handleBack)

    BatteryInfoScreen(
        uiState = uiState,
        showScanning = showScanning,
        showExitDialog = showExitDialog,
        onBack = ::handleBack,
        onQuitScan = {
            showExitDialog = false
            exit()
        },
        onResumeScan = {
            showExitDialog = false
            scanSession += 1
        },
        onRangeSelected = viewModel::selectCurrentRange,
    )
}
