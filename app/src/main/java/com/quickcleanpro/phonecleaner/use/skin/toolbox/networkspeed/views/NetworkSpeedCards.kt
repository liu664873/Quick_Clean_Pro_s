package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkspeed.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedPhase
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedUiState
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.RotatingRingAnimation
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.SemiCircularGauge
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXBlue

@Composable
internal fun NetworkSpeedInfoCard(uiState: NetworkSpeedUiState) {
    val showNetworkInfo =
        uiState.hasNetwork &&
            (uiState.phase == NetworkSpeedPhase.Testing || uiState.phase == NetworkSpeedPhase.Completing)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
        color = NetworkSpeedCardBg,
        shape = RoundedCornerShape(NetworkSpeedCardRadius),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            NetworkSpeedInfoLine(
                label = stringResource(R.string.network_type),
                value = if (showNetworkInfo) uiState.networkInfo.type else "--",
            )
            NetworkSpeedInfoDivider()
            NetworkSpeedInfoLine(
                label = stringResource(R.string.wifi_name),
                value = if (showNetworkInfo) uiState.networkInfo.ssid else "--",
            )
            NetworkSpeedInfoDivider()
            NetworkSpeedInfoLine(
                label = stringResource(R.string.ip),
                value = if (showNetworkInfo) uiState.networkInfo.ip else "--",
            )
        }
    }
}

@Composable
internal fun NetworkSpeedMetricCard(
    uiState: NetworkSpeedUiState,
    isDownloadTesting: Boolean = false,
    isUploadTesting: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp),
        color = NetworkSpeedCardBg,
        shape = RoundedCornerShape(NetworkSpeedCardRadius),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkSpeedMetricColumn(
                direction = "download",
                label = stringResource(R.string.download),
                value = uiState.downloadLabel,
                isTesting = isDownloadTesting,
                modifier = Modifier.weight(1f),
            )
            NetworkSpeedVerticalDivider()
            NetworkSpeedMetricColumn(
                direction = "upload",
                label = stringResource(R.string.upload),
                value = uiState.uploadLabel,
                isTesting = isUploadTesting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun NetworkSpeedGauge(isAnimating: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        SemiCircularGauge(
            modifier = Modifier.size(280.dp),
            isAnimating = isAnimating,
            arcStartColor = Color(0xFF00C9FF),
            arcEndColor = Color(0xFF92FE9D),
            needleColor = Color(0xFF3366FF),
            tickColor = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
internal fun NetworkSpeedEmptyCard(
    title: String,
    message: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NetworkSpeedCardBg,
        shape = RoundedCornerShape(NetworkSpeedCardRadius),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = NetworkSpeedNavy,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = NetworkSpeedNavyMuted,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
internal fun NetworkSpeedErrorText(message: String?) {
    if (message.isNullOrBlank()) return

    Text(
        text = message,
        color = NetworkSpeedDanger,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun NetworkSpeedInfoLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(39.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = NetworkSpeedNavyMuted,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = value.ifBlank { "--" },
            color = NetworkSpeedNavy,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun NetworkSpeedInfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 0.dp),
        color = NetworkSpeedDivider,
        thickness = 1.dp,
    )
}

@Composable
private fun NetworkSpeedMetricColumn(
    direction: String,
    label: String,
    value: String,
    isTesting: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.height(44.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (isTesting) {
                RotatingRingAnimation(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .padding(bottom = 4.dp),
                    ringWidth = 4.dp,
                    ringColor = CleanXBlue,
                    backgroundColor = CleanXBlue.copy(alpha = 0.16f),
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        color = NetworkSpeedNavy,
                        fontSize = if (value == "--") 26.sp else 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.mbps),
                        color = NetworkSpeedNavy,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TransferBubble(direction = direction)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label\n${stringResource(R.string.mbps)}",
                color = NetworkSpeedNavyMuted,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TransferBubble(direction: String) {
    Box(
        modifier =
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter =
                painterResource(
                    if (direction == "download") {
                        R.drawable.ic_download
                    } else {
                        R.drawable.ic_upload
                    },
                ),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun NetworkSpeedVerticalDivider() {
    Box(
        modifier =
            Modifier
                .width(1.dp)
                .height(58.dp)
                .background(NetworkSpeedDivider),
    )
}
