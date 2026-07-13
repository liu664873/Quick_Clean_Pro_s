package com.quickcleanpro.phonecleaner.feature.toolbox.networkscan

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesViewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui.NetworkScanDevicesContentView
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator


import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui.NetworkScanDevicesScreen

@Composable
fun NetworkScanDevicesRoute(
    navigator: AppNavigator,
    viewModel: NetworkScanDevicesViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NetworkScanDevicesScreen(
        state = uiState,
        onAction = { action ->
            if (action == NetworkScanDevicesAction.Back) navigator.back() else viewModel.onAction(action)
        },
        onNavigate = navigator::open,
    )
}

