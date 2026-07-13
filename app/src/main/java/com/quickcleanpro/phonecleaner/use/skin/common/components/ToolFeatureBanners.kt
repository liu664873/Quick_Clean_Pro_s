package com.quickcleanpro.phonecleaner.use.skin.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.skin.common.theme.LocalAppThemeTokens
import kotlin.random.Random

private val CardBg: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppThemeTokens.current.colors.virusBackgroundCard
private val Navy: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppThemeTokens.current.colors.navy
private val NavyMuted: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppThemeTokens.current.colors.navyMuted
private val Blue: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppThemeTokens.current.colors.primary
private val CardRadius = 8.dp

data class ToolFeature(
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val destination: AppDestination,
    val iconRes: Int,
    val gradient: Brush,
    @param:StringRes val actionLabelRes: Int,
)

val AllToolFeatures
    get() =
        listOf(
            ToolFeature(
                R.string.junk_removal,
                R.string.file_suggestion_junk_desc,
                AppDestination.JunkClean,
                R.drawable.ic_n_junk_removal,
                Brush.linearGradient(listOf(Color(0xFFFF6B4A), Color(0xFFFF3D2E))),
                R.string.scan_now,
            ),
            ToolFeature(
                R.string.nav_device_info,
                R.string.common_tool_device_desc,
                AppDestination.DeviceInfo,
                R.drawable.ic_n_device_info,
                Brush.linearGradient(listOf(Color(0xFF7F6CFF), Color(0xFF462BF9))),
                R.string.view_now,
            ),
            ToolFeature(
                R.string.nav_battery_info,
                R.string.common_tool_battery_desc,
                AppDestination.BatteryInfo,
                R.drawable.ic_battery,
                Brush.linearGradient(listOf(Color(0xFF90FB9C), Color(0xFF8AFB88))),
                R.string.view_now,
            ),
            /*
            ToolFeature(
                R.string.nav_app_usage,
                R.string.common_tool_app_usage_desc,
                AppDestination.AppUsage,
                R.drawable.ic_app_usage,
                Brush.linearGradient(listOf(Color(0xFF6EC6FF), Color(0xFF2196F3))),
                R.string.view_now,
            ),
            */
            ToolFeature(
                R.string.nav_notification_cleaner,
                R.string.common_tool_notification_cleaner_desc,
                AppDestination.NotificationCleaner,
                R.drawable.ic_n_notification_cleaner,
                Brush.linearGradient(listOf(Color(0xFFFF9A80), Color(0xFFFF6E40))),
                R.string.view_now,
            ),
            ToolFeature(
                R.string.nav_whatsapp_cleaner,
                R.string.common_tool_whatsapp_desc,
                AppDestination.WhatsAppCleaner,
                R.drawable.ic_whats_app_cleaner,
                Brush.linearGradient(listOf(Color(0xFF81C784), Color(0xFF4CAF50))),
                R.string.view_now,
            ),
            ToolFeature(
                R.string.nav_network_usage,
                R.string.common_tool_network_usage_desc,
                AppDestination.NetworkUsage,
                R.drawable.ic_n_network_usage,
                Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF1976D2))),
                R.string.view_now,
            ),
            ToolFeature(
                R.string.nav_network_scan,
                R.string.common_tool_network_scan_desc,
                AppDestination.NetworkScan,
                R.drawable.ic_network_scan,
                Brush.linearGradient(listOf(Color(0xFFFFC745), Color(0xFFFE9915))),
                R.string.scan_now,
            ),
            /*
            ToolFeature(
                R.string.nav_network_speed,
                R.string.common_tool_network_speed_desc,
                AppDestination.NetworkSpeed,
                R.drawable.ic_network_speed,
                Brush.linearGradient(listOf(Color(0xFFCE93D8), Color(0xFF9C27B0))),
                R.string.test_now,
            ),
            */
        )

@Composable
fun ToolFeatureBanners(
    onNavigateTool: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    excludeRoutes: Set<String> = emptySet(),
    seed: Long = System.currentTimeMillis(),
) {
    val features =
        remember(seed) {
            val pool = AllToolFeatures.filter { it.destination.route !in excludeRoutes }
            pool.shuffled(Random(seed)).take(2)
        }

    Column(modifier = modifier.fillMaxWidth()) {
        features.forEach { feature ->
            ToolFeatureBanner(
                feature = feature,
                onClick = { onNavigateTool(feature.destination) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ToolFeatureBanners(
    features: List<ToolFeature>,
    onNavigateTool: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        features.forEach { feature ->
            ToolFeatureBanner(
                feature = feature,
                onClick = { onNavigateTool(feature.destination) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToolFeatureBanner(
    feature: ToolFeature,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(color = CardBg, shape = RoundedCornerShape(CardRadius))
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(top = 1.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(feature.gradient),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(feature.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(27.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = stringResource(feature.titleRes),
                    color = Navy,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(feature.subtitleRes),
                    color = NavyMuted,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(feature.actionLabelRes),
                color = Blue,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
    }
}
