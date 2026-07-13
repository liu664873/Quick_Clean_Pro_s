package com.quickcleanpro.phonecleaner.feature.antivirus.ui

import com.quickcleanpro.phonecleaner.feature.antivirus.*

import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanViewModel

import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanMode

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.VirusScanningView
import kotlinx.coroutines.delay

@Composable
fun QuickScanVirusScreen(
    navigator: AppNavigator,
    viewModel: VirusScanViewModel,
) {
    VirusScanContent(
        mode = VirusScanMode.Quick,
        navigator = navigator,
        viewModel = viewModel,
    )
}

@Composable
fun DeepScanVirusScreen(
    navigator: AppNavigator,
    viewModel: VirusScanViewModel,
) {
    VirusScanContent(
        mode = VirusScanMode.Deep,
        navigator = navigator,
        viewModel = viewModel,
    )
}

@Composable
private fun VirusScanContent(
    mode: VirusScanMode,
    navigator: AppNavigator,
    viewModel: VirusScanViewModel,
) {
    val router = navigator
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var scanStarted by remember(mode) { mutableStateOf(false) }

    LaunchedEffect(mode) {
        scanStarted = false
        viewModel.startScan(mode)
        scanStarted = true
    }

    LaunchedEffect(scanStarted, uiState.scanCompleted, uiState.effectiveThreatCount) {
        if (scanStarted && uiState.scanCompleted) {
            if (uiState.effectiveThreatCount > 0) {
                router.replace(AppDestination.VirusResult)
            } else {
                router.replace(AppDestination.NoVirusResult)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
            delay(200L)
            router.back()
        }
    }

    DisposableEffect(mode) {
        onDispose { viewModel.cancelScan() }
    }

    VirusScanningView(
        mode = mode,
        uiState = uiState,
    )
}
