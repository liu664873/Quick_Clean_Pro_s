package com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.*


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.AllToolFeatures
import com.quickcleanpro.phonecleaner.common.ui.components.CommonResultCheckIcon
import com.quickcleanpro.phonecleaner.common.ui.components.ToolFeatureBanners
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.NetworkSpeedUiState

@Composable
internal fun NetworkSpeedResultView(
    uiState: NetworkSpeedUiState,
    onNavigateTool: (AppDestination) -> Unit,
) {
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//    ) {
//        CommonResultCheckIcon()
//        Spacer(modifier = Modifier.height(20.dp))
//        Text(
//            text = stringResource(R.string.speed_test_complete),
//            color = NetworkSpeedNavyMuted,
//            fontSize = 16.sp,
//            lineHeight = 20.sp,
//            textAlign = TextAlign.Center,
//        )
//        Spacer(modifier = Modifier.height(22.dp))
//        NetworkSpeedMetricCard(uiState = uiState)
//    }
    NetworkSpeedMetricCard(uiState = uiState)
    Spacer(modifier = Modifier.height(40.dp))
    ToolFeatureBanners(
        onNavigateTool = onNavigateTool,
        features =
            AllToolFeatures.filter {
                it.destination == AppDestination.DeviceInfo || it.destination == AppDestination.NetworkScan
            },
    )
    Spacer(modifier = Modifier.height(32.dp))
}
