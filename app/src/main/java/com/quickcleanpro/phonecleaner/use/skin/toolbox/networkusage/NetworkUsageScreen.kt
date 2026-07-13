package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkusage

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkusage.views.NetworkUsageScreenState
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkusage.NetworkUsageUiState

@Composable
fun NetworkUsageScreen(
    uiState: NetworkUsageUiState,
    showScanning: Boolean,
    showStopDialog: Boolean,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onQuitScan: () -> Unit,
    onResumeScan: () -> Unit,
) {
    NetworkUsageScreenState(
        uiState = uiState,
        showScanning = showScanning,
        showStopDialog = showStopDialog,
        onBack = onBack,
        onTabSelected = onTabSelected,
        onQuitScan = onQuitScan,
        onResumeScan = onResumeScan,
    )
}
