package com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesUiState

@Composable
internal fun NetworkScanDevicesContentView(
    uiState: NetworkScanDevicesUiState,
) {
    when {
        uiState.isLoading -> NetworkScanDevicesLoadingView(uiState = uiState)
        uiState.errorMessage != null -> NetworkScanDevicesErrorView(uiState = uiState)
        else -> NetworkScanDevicesResultView(uiState = uiState)
    }
}
