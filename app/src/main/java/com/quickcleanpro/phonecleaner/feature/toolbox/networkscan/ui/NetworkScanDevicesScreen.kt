package com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesViewModel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.common.ui.components.buttons.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.common.ui.components.stableNavigationBarsPadding
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
        bottomBar = {
            if (!state.isLoading && state.errorMessage != null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .stableNavigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    CleanXPrimaryButton(
                        text = stringResource(R.string.retry),
                        onClick = { onAction(NetworkScanDevicesAction.Retry) },
                    )
                }
            }
        },
    ) {
        NetworkScanDevicesContentView(uiState = state)
    }
}
