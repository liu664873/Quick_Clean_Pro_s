package com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.*


import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.ui.NetworkUsageScreenState
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.NetworkUsageUiState

@Composable
fun NetworkUsageScreen(
    state: NetworkUsageUiState,
    onAction: (NetworkUsageAction) -> Unit,
) {
    NetworkUsageScreenState(
        uiState = state,
        showScanning = state.isScanning,
        showStopDialog = state.showStopDialog,
        onBack = { onAction(NetworkUsageAction.Back) },
        onTabSelected = { onAction(NetworkUsageAction.TabSelected(it)) },
        onQuitScan = { onAction(NetworkUsageAction.QuitScan) },
        onResumeScan = { onAction(NetworkUsageAction.ResumeScan) },
    )
}
