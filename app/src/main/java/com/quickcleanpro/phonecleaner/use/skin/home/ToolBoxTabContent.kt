package com.quickcleanpro.phonecleaner.use.skin.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.feature.iconRes
import com.quickcleanpro.phonecleaner.use.core.feature.destinationOrNull
import com.quickcleanpro.phonecleaner.use.core.feature.titleRes
import com.quickcleanpro.phonecleaner.use.core.model.device.BatteryInfo
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXIconTile
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXInfoPanel
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXPillShape
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXSectionTitle
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXText
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.CleanXSingleActionDialog
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXCompactButtonHeight
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXGridSpacing
import com.quickcleanpro.phonecleaner.use.feature.home.presentation.HomeSummaryUiState

private val ToolboxFeatures =
    listOf(
        FeatureKey.APP_USAGE,
        FeatureKey.NOTIFICATION_CLEANER,
        FeatureKey.WHATSAPP_CLEANER,
        FeatureKey.NETWORK_USAGE,
        FeatureKey.NETWORK_SCAN,
        FeatureKey.NETWORK_SPEED,
    )

@Composable
fun ToolBoxTabContent(
    summaryState: HomeSummaryUiState,
    onFeatureClick: (FeatureKey) -> Unit = {},
) {
    val context = LocalContext.current
    var showWhatsAppMissingDialog by remember { mutableStateOf(false) }
    val tools = remember { ToolboxFeatures.filter { feature -> feature.destinationOrNull() != null } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(CleanXGridSpacing),
        verticalArrangement = Arrangement.spacedBy(CleanXGridSpacing),
    ) {
        item(key = "device_summary", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CleanXGridSpacing),
            ) {
                DeviceInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_device,
                    labels =
                        listOf(
                            R.string.home_device_model to summaryState.deviceModel,
                            R.string.home_system_version to summaryState.androidVersion,
                        ),
                    buttonText = stringResource(R.string.check_now),
                    background = Color(0xFF5866E8),
                    onClick = {
                        onFeatureClick(FeatureKey.DEVICE_INFO)
                    },
                )
                DeviceInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_battery,
                    labels =
                        listOf(
                            R.string.home_battery_status to batteryStatus(summaryState.batteryInfo),
                            R.string.home_health to summaryState.batteryInfo.health,
                        ),
                    buttonText = stringResource(R.string.check_now),
                    background = Color(0xFF35C979),
                    onClick = {
                        onFeatureClick(FeatureKey.BATTERY_INFO)
                    },
                )
            }
        }

        item(key = "toolbox_title", span = { GridItemSpan(maxLineSpan) }) {
            CleanXSectionTitle(
                text = stringResource(R.string.home_toolbox_title),
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            )
        }

        items(tools, key = { feature -> feature.name }) { feature ->
            val title = stringResource(feature.titleRes())
            CleanXIconTile(
                title = title,
                icon = painterResource(id = feature.iconRes()),
                onClick = {
                    if (feature == FeatureKey.WHATSAPP_CLEANER && !context.hasWhatsAppInstalled()) {
                        showWhatsAppMissingDialog = true
                    } else {
                        onFeatureClick(feature)
                    }
                },
            )
        }
    }

    if (showWhatsAppMissingDialog) {
        CleanXSingleActionDialog(
            title = stringResource(R.string.whatsapp_not_found_title),
            message = stringResource(R.string.whatsapp_not_found_message),
            actionText = stringResource(R.string.close),
            onAction = { showWhatsAppMissingDialog = false },
            onDismissRequest = { showWhatsAppMissingDialog = false },
        )
    }
}

private fun android.content.Context.hasWhatsAppInstalled(): Boolean =
    WhatsAppPackageNames.any { packageName ->
        packageManager.getLaunchIntentForPackage(packageName) != null
    }

private val WhatsAppPackageNames = listOf("com.whatsapp", "com.whatsapp.w4b")

private fun batteryStatus(batteryInfo: BatteryInfo): String =
    "${batteryInfo.levelPercent}% ${batteryInfo.technology}".trim()

@Composable
private fun DeviceInfoCard(
    modifier: Modifier,
    icon: Int,
    labels: List<Pair<Int, String>>,
    buttonText: String,
    background: Color,
    onClick: () -> Unit,
) {
    CleanXInfoPanel(
        modifier = modifier.heightIn(min = 184.dp),
        background = background,
        contentPadding = PaddingValues(14.dp),
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            colorFilter = ColorFilter.tint(Color.White),
        )

        Spacer(modifier = Modifier.height(12.dp))

        labels.forEachIndexed { index, pair ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(pair.first),
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = pair.second,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (index != labels.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(CleanXCompactButtonHeight),
            shape = CleanXPillShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = CleanXText,
                ),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(
                text = buttonText,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
