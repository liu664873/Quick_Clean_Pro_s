package com.quickcleanpro.phonecleaner.feature.antivirus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanMode
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanUiState
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXScanRingAnimation
import com.quickcleanpro.phonecleaner.common.ui.components.stableNavigationBarsPadding

@Composable
internal fun VirusScanningView(
    mode: VirusScanMode,
    uiState: VirusScanUiState,
) {
    VirusPageScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .stableNavigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CleanXScanRingAnimation(
                        modifier = Modifier
                            .padding(top = 15.dp)
                            .size(300.dp),
                        ringModifier = Modifier.size(210.dp),
                        backgroundResId = R.mipmap.ic_scan_nor_bg,
                        ringColor = VirusBlue,
                        backgroundColor = androidx.compose.ui.graphics.Color(0xFFF1F9FC)
                    ) {
                        ThreatDrawableImage(
                            drawable = uiState.currentIcon,
                            fallback = R.drawable.ic_virus_protection_shield,
                            modifier = Modifier.size(75.dp)
                        )
                    }
                }

                Text(
                    text = uiState.currentLabel,
                    color = VirusTitle,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VirusProgressTrack(
                    mode = mode,
                    progress = uiState.progressFraction,
                    hasAdbRisk = uiState.hasAdbRisk,
                    appThreatCount = uiState.appThreatCount,
                    fileThreatCount = uiState.fileThreatCount
                )
                Spacer(modifier = Modifier.height(50.dp))
                Text(
                    text = stringResource(R.string.powered_by_trustlook),
                    color = androidx.compose.ui.graphics.Color(0xFF999999),
                    fontSize = 16.sp,
                )
            }
        }
    }
}
