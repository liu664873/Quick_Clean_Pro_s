package com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.*


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.NetworkSpeedUiState

@Composable
internal fun NetworkSpeedTestingView(uiState: NetworkSpeedUiState) {
    NetworkSpeedInfoCard(uiState = uiState)
    Spacer(modifier = Modifier.height(20.dp))
    NetworkSpeedMetricCard(
        uiState = uiState,
        isDownloadTesting = uiState.progress.phase == "latency" || uiState.progress.phase == "download",
        isUploadTesting = uiState.progress.phase == "upload",
    )
    Spacer(modifier = Modifier.height(22.dp))
    NetworkSpeedGauge(isAnimating = true)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = networkSpeedPhaseLabel(uiState.progress.phase),
        color = NetworkSpeedNavyMuted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(96.dp))
}

@Composable
private fun networkSpeedPhaseLabel(phase: String): String =
    when (phase) {
        "latency" -> stringResource(R.string.network_speed_testing_latency)
        "download" -> stringResource(R.string.network_speed_testing_download)
        "upload" -> stringResource(R.string.network_speed_testing_upload)
        "done" -> stringResource(R.string.onboarding_done)
        else -> stringResource(R.string.network_speed_testing)
    }
