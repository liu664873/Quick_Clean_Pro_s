package com.quickcleanpro.phonecleaner.feature.antivirus.ui

import com.quickcleanpro.phonecleaner.feature.antivirus.*

import com.quickcleanpro.phonecleaner.feature.antivirus.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.VirusBlue
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.VirusFeatureRow
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.VirusPageScaffold
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.VirusPrimaryButton
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.VirusTitle
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXScanRingAnimation
import com.quickcleanpro.phonecleaner.common.ui.components.stableNavigationBarsPadding

@Composable
internal fun AntiVirusHomeView(
    onDeepScan: () -> Unit,
    onQuickScan: () -> Unit,
    onBack: () -> Unit,
    enabled: Boolean = true,
) {
    VirusPageScaffold(
        onBack = onBack,
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .stableNavigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 25.dp, bottom = 16.dp),
            ) {
                VirusPrimaryButton(
                    text = stringResource(R.string.deep_scan),
                    onClick = onDeepScan,
                    enabled = enabled,
                )

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    onClick = onQuickScan,
                    enabled = enabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAF7FD),
                            contentColor = VirusBlue,
                            disabledContainerColor = Color(0xFFEAF7FD).copy(alpha = 0.55f),
                            disabledContentColor = VirusBlue.copy(alpha = 0.45f),
                        ),
                ) {
                    Text(
                        text = stringResource(R.string.quick_scan),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(330.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                CleanXScanRingAnimation(
                    modifier =
                        Modifier
                            .padding(top = 15.dp)
                            .size(300.dp),
                    ringModifier = Modifier.size(210.dp),
                    backgroundResId = R.mipmap.ic_scan_nor_bg,
                    ringColor = VirusBlue,
                    backgroundColor = Color(0xFFF1F9FC),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_virus_protection_shield),
                        contentDescription = null,
                        modifier = Modifier.size(75.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Text(
                text = stringResource(R.string.engine_is_ready),
                color = VirusTitle,
                fontSize = 16.sp,
            )

            Spacer(modifier = Modifier.height(15.dp))

            VirusFeatureRow(
                icon = R.mipmap.ic_virus,
                label = stringResource(R.string.virus),
            )
            Spacer(modifier = Modifier.height(10.dp))
            VirusFeatureRow(
                icon = R.mipmap.ic_malware,
                label = stringResource(R.string.malware),
            )
            Spacer(modifier = Modifier.height(10.dp))
            VirusFeatureRow(
                icon = R.mipmap.ic_privacy,
                label = stringResource(R.string.privacy),
            )

        }
    }
}
