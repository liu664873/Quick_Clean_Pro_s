package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan.views

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanPhase
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanUiState

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
