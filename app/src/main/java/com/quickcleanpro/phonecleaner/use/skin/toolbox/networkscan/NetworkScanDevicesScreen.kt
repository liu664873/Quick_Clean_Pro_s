package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan

import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanDevicesViewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan.views.NetworkScanDevicesContentView

@Composable
fun NetworkScanDevicesScreen(viewModel: NetworkScanDevicesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CleanXScaffoldPage(
        title = stringResource(R.string.network_scan),
        titleFontSize = 20.sp,
    ) {
        NetworkScanDevicesContentView(
            uiState = uiState,
            onRetry = viewModel::loadDevices,
        )
    }
}
