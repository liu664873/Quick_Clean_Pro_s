package com.quickcleanpro.phonecleaner.use.skin.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.model.device.StorageInfo
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXBlue
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXCard
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXMutedText
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXPillShape
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXText
import com.quickcleanpro.phonecleaner.use.feature.home.presentation.HomeSummaryUiState

private val PrimaryText = CleanXText
private val SecondaryText = CleanXMutedText
private val SelectedBlue = CleanXBlue
private val CleanXSubtlePanel = Color(0xFFEFF6FC)
private val CleanXWarning = Color(0xFFECAD1A)
private val CleanXDanger = Color(0xFFFF6B3D)

@Composable
fun HomeTabContent(
    summaryState: HomeSummaryUiState,
    onFeatureClick: (FeatureKey) -> Unit = {},
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
    ) {
        StorageSummary(
            storageInfo = summaryState.storageInfo,
            onClick = {
                onFeatureClick(FeatureKey.JUNK_CLEAN)
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        GradientCleanCard(
            title = stringResource(R.string.home_virus_title),
            description = stringResource(R.string.home_virus_desc),
            gradient = Brush.horizontalGradient(listOf(Color(0xFF86B4FF), Color(0xFFB6D9FF))),
            iconRes = R.drawable.home_virus_shield,
            iconOnStart = true,
            onClick = {
                onFeatureClick(FeatureKey.ANTI_VIRUS)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        GradientCleanCard(
            title =
                pluralStringResource(
                    R.plurals.locked_apps_count,
                    summaryState.lockedAppCount,
                    summaryState.lockedAppCount,
                ),
            description = stringResource(R.string.home_locked_apps_desc),
            gradient = Brush.horizontalGradient(listOf(Color(0xFFB486FF), Color(0xFFD0B6FF))),
            iconRes = R.drawable.home_app_lock,
            iconOnStart = false,
            onClick = {
                onFeatureClick(FeatureKey.APP_LOCK)
            },
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun StorageSummary(
    storageInfo: StorageInfo,
    onClick: () -> Unit,
) {
    CleanXCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp),
        contentPadding = PaddingValues(16.dp),
        containerColor = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.home_storage_label),
                        color = PrimaryText,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = storageInfo.formattedUsed,
                            color = SelectedBlue,
                            fontSize = 19.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = "/${storageInfo.formattedTotal}",
                            color = SecondaryText,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(bottom = 2.dp),
                            maxLines = 1,
                        )
                    }
                }

                StorageProgress(usagePercent = storageInfo.usagePercent)

                Button(
                    onClick = onClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    shape = CleanXPillShape,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = removeJunkButtonColor(storageInfo.usagePercent),
                            contentColor = Color.White,
                        ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_remove_junk),
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.trash_can),
                contentDescription = null,
                modifier = Modifier.size(104.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun removeJunkButtonColor(usagePercent: Int): Color {
    val usageRatio = usagePercent.coerceIn(0, 100) / 100f
    return when {
        usageRatio < 1f / 3f -> CleanXBlue
        usageRatio < 2f / 3f -> CleanXWarning
        else -> CleanXDanger
    }
}

@Composable
private fun StorageProgress(usagePercent: Int) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CleanXPillShape)
                .background(CleanXSubtlePanel),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(usagePercent.coerceIn(0, 100) / 100f)
                    .height(12.dp)
                    .clip(CleanXPillShape)
                    .background(removeJunkButtonColor(usagePercent)),
        )
    }
}

@Composable
private fun GradientCleanCard(
    title: String,
    description: String,
    gradient: Brush,
    iconRes: Int,
    iconOnStart: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(181.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(gradient)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(81.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconOnStart) {
                HomeFeatureIcon(iconRes = iconRes)
                HomeFeatureCopy(
                    title = title,
                    description = description,
                    modifier = Modifier.weight(1f),
                )
            } else {
                HomeFeatureCopy(
                    title = title,
                    description = description,
                    modifier = Modifier.weight(1f),
                )
                HomeFeatureIcon(iconRes = iconRes)
            }
        }

        Button(
            onClick = onClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            shape = CleanXPillShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryText,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.clean_now),
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HomeFeatureIcon(iconRes: Int) {
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(81.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun HomeFeatureCopy(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 16.sp,
            lineHeight = 19.sp,
        )
    }
}
