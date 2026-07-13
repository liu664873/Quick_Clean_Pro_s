package com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.*

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.buttons.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesUiState

@Composable
internal fun NetworkScanDevicesErrorView(
    uiState: NetworkScanDevicesUiState,
    onRetry: () -> Unit,
) {
    NetworkScanDevicesHeaderCard(count = uiState.devices.size)
    Spacer(modifier = Modifier.height(20.dp))
    NetworkScanMessageCard(
        title = stringResource(R.string.scan_failed),
        message = uiState.errorMessage.orEmpty(),
    )
    Spacer(modifier = Modifier.height(20.dp))
    CleanXPrimaryButton(
        text = stringResource(R.string.retry),
        onClick = onRetry,
    )
    Spacer(modifier = Modifier.height(96.dp))
}
