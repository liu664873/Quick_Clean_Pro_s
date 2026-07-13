package com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanPhase
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanUiState

@Composable
internal fun NetworkScanContentView(
    uiState: NetworkScanUiState,
    onDevicesClick: () -> Unit,
) {
    when (uiState.phase) {
        NetworkScanPhase.Idle -> NetworkScanIdleView(uiState = uiState)
        NetworkScanPhase.Scanning -> NetworkScanScanningView(uiState = uiState)
        NetworkScanPhase.Result -> NetworkScanResultView(
            uiState = uiState,
            onDevicesClick = onDevicesClick,
        )
        NetworkScanPhase.Error -> NetworkScanErrorView(uiState = uiState)
    }
}
