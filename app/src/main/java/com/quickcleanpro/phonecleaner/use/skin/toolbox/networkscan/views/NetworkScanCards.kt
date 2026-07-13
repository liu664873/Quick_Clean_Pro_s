package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkDeviceInfo
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkScanResult
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXStatusBadge
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXStatusBadgeState
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanPhase
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanUiState

private enum class NetworkScanDetailStatus {
    Unknown,
    Active,
    Complete,
}

@Composable
internal fun NetworkScanResultCard(uiState: NetworkScanUiState) {
    NetworkScanSectionCard(title = stringResource(R.string.wifi_scan_result)) {
        NetworkScanInfoRow(
            label = stringResource(R.string.wifi_ssid_label),
            value = uiState.scan?.ssid?.displayUnknownSsid() ?: uiState.networkInfo.ssid.displayBlank(),
        )
        NetworkScanDivider()
        NetworkScanInfoRow(
            label = stringResource(R.string.last_scan),
            value = uiState.scanTime,
        )
    }
}

@Composable
internal fun NetworkScanDetailsCard(uiState: NetworkScanUiState) {
    val detailItems =
        listOf(
            stringResource(R.string.wifi_auth),
            stringResource(R.string.arp_spoofing),
            stringResource(R.string.ssl_stripping),
            stringResource(R.string.ssl_splitting),
            stringResource(R.string.dns_spoofing),
            stringResource(R.string.devices),
        )
    val completedCount =
        when (uiState.phase) {
            NetworkScanPhase.Result -> detailItems.size
            NetworkScanPhase.Scanning -> uiState.completedDetailCount
            else -> 0
        }

    NetworkScanSectionCard(
        title = stringResource(R.string.details),
        verticalPadding = 18.dp,
        shapeRadius = 12.dp,
    ) {
        detailItems.forEachIndexed { index, label ->
            if (index > 0) NetworkScanDivider()
            NetworkScanDetailRow(
                label = label,
                status =
                    when {
                        index < completedCount -> NetworkScanDetailStatus.Complete
                        uiState.phase == NetworkScanPhase.Scanning && index == completedCount -> NetworkScanDetailStatus.Active
                        else -> NetworkScanDetailStatus.Unknown
                    },
            )
        }
    }
}

@Composable
internal fun NetworkScanDevicesSummaryCard(
    scan: NetworkScanResult,
    onDevicesClick: () -> Unit,
) {
    val devices = scan.devices
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = devices.isNotEmpty(), onClick = onDevicesClick),
        color = NetworkScanCardBg,
        shape = RoundedCornerShape(NetworkScanCardRadius),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.devices_count, devices.size),
                    color = NetworkScanNavy,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (devices.isNotEmpty()) NetworkScanNavy else Color.Transparent,
                )
            }
            if (devices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                devices.take(2).forEachIndexed { index, device ->
                    if (index > 0) NetworkScanDivider()
                    NetworkScanDeviceSummaryRow(device = device)
                }
            }
        }
    }
}

@Composable
internal fun NetworkScanMessageCard(
    title: String,
    message: String,
) {
    NetworkScanSectionCard(title = title) {
        Text(
            text = message,
            color = NetworkScanNavyMuted,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
internal fun NetworkScanErrorText(message: String?) {
    if (message.isNullOrBlank()) return

    Text(
        text = message,
        color = NetworkScanDanger,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
internal fun NetworkScanDeviceDetailCard(device: NetworkDeviceInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NetworkScanCardBg,
        shape = RoundedCornerShape(NetworkScanCardRadius),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            NetworkScanDeviceSummaryRow(device = device)
            NetworkScanDivider()
            NetworkScanInfoRow(
                label = stringResource(R.string.vendor),
                value = stringResource(R.string.unknown),
            )
            NetworkScanDivider()
            NetworkScanInfoRow(
                label = stringResource(R.string.name),
                value = device.hostName.displayUnknown(),
            )
            NetworkScanDivider()
            NetworkScanInfoRow(
                label = stringResource(R.string.mac_address),
                value = device.macAddress.displayUnknown(),
            )
        }
    }
}

@Composable
internal fun NetworkScanDevicesHeaderCard(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NetworkScanCardBg,
        shape = RoundedCornerShape(NetworkScanCardRadius),
    ) {
        Text(
            text = stringResource(R.string.devices_count, count),
            color = NetworkScanNavy,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun NetworkScanSectionCard(
    title: String,
    verticalPadding: Dp = 16.dp,
    shapeRadius: Dp = NetworkScanCardRadius,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NetworkScanCardBg,
        shape = RoundedCornerShape(shapeRadius),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = verticalPadding),
        ) {
            Text(
                text = title,
                color = NetworkScanNavy,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun NetworkScanInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(39.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = NetworkScanNavyMuted,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = value.ifBlank { "--" },
            color = NetworkScanNavy,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun NetworkScanDetailRow(
    label: String,
    status: NetworkScanDetailStatus,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = NetworkScanNavyMuted,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        )
        when (status) {
            NetworkScanDetailStatus.Unknown ->
                Image(
                    painter = painterResource(id = R.drawable.ic_network_scan_unknown),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            NetworkScanDetailStatus.Active ->
                CleanXStatusBadge(
                    state = CleanXStatusBadgeState.Active,
                )
            NetworkScanDetailStatus.Complete ->
                CleanXStatusBadge(
                    state = CleanXStatusBadgeState.Complete,
                )
        }
    }
}

@Composable
private fun NetworkScanDeviceSummaryRow(device: NetworkDeviceInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = NetworkScanNavy,
            )
            Text(
                text = device.ip,
                color = NetworkScanNavy,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = device.hostName.displayUnknown(),
            color = NetworkScanNavy,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun NetworkScanDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 0.dp),
        color = NetworkScanDivider,
        thickness = 1.dp,
    )
}

private fun String.displayUnknownSsid(): String =
    takeUnless { it == "<unknown ssid>" || it.isBlank() } ?: "<unknown ssid>"

private fun String.displayUnknown(): String =
    takeUnless { it.equals("unknown", ignoreCase = true) || it.isBlank() } ?: "unknown"

private fun String.displayBlank(): String = ifBlank { "--" }
