package com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesViewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui.NetworkScanDevicesContentView
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

@Composable
fun NetworkScanDevicesScreen(
    state: NetworkScanDevicesUiState,
    onAction: (NetworkScanDevicesAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {

    CleanXScaffoldPage(
        title = stringResource(R.string.network_scan),
        modifier = modifier,
        titleFontSize = 20.sp,
        onBack = { onAction(NetworkScanDevicesAction.Back) },
    ) {
        NetworkScanDevicesContentView(
            uiState = state,
            onRetry = { onAction(NetworkScanDevicesAction.Retry) },
        )
    }
}
