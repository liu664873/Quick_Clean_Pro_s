package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan

import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanUiState

import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanPhase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.common.components.stableNavigationBarsPadding
import com.quickcleanpro.phonecleaner.use.skin.common.components.buttons.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXBlue
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXButtonHeight
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXButtonShape
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan.views.NetworkScanContentView

@Composable
fun NetworkScanScreen(
    uiState: NetworkScanUiState,
    onBack: () -> Unit,
    onRefreshWifi: () -> Unit,
    onSwitchWifi: () -> Unit,
    onScan: () -> Unit,
    onOpenDevices: () -> Unit,
) {
    CleanXScaffoldPage(
        title = stringResource(R.string.network_scan),
        titleFontSize = 20.sp,
        onBack = onBack,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        bottomBar = {
            NetworkScanBottomBar(
                uiState = uiState,
                onRefreshWifi = onRefreshWifi,
                onSwitchWifi = onSwitchWifi,
                onScan = onScan,
            )
        },
    ) {
        NetworkScanContentView(
            uiState = uiState,
            onDevicesClick = onOpenDevices,
        )
    }
}
@Composable
private fun NetworkScanBottomBar(
    uiState: NetworkScanUiState,
    onRefreshWifi: () -> Unit,
    onSwitchWifi: () -> Unit,
    onScan: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .stableNavigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        when {
            !uiState.hasWifi -> {
                CleanXPrimaryButton(
                    text = stringResource(R.string.scan_again),
                    onClick = onRefreshWifi,
                )
            }
            uiState.phase == NetworkScanPhase.Result -> {
                CleanXPrimaryButton(
                    text = stringResource(R.string.switch_wifi),
                    onClick = onSwitchWifi,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onScan,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(CleanXButtonHeight),
                    shape = CleanXButtonShape,
                    border = BorderStroke(1.56.dp, CleanXBlue),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = CleanXBlue,
                        ),
                ) {
                    Text(
                        text = stringResource(R.string.scan_again),
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            else -> {
                CleanXPrimaryButton(
                    text = stringResource(R.string.scan_wifi),
                    onClick = onScan,
                    enabled = uiState.phase != NetworkScanPhase.Scanning,
                )
            }
        }
    }
}
