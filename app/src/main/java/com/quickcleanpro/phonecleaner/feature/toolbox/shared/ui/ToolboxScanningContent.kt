package com.quickcleanpro.phonecleaner.feature.toolbox.shared.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXScanProgress

@Composable
internal fun ToolboxScanningContent(
    @DrawableRes centerIconRes: Int,
    captionText: String,
    modifier: Modifier = Modifier,
    ringColor: Color = Color(0xFF22A9E8),
    captionColor: Color = Color(0xFF2D3748),
    centerIconSize: Dp = 74.dp,
) {
    CleanXScanProgress(
        captionText = captionText,
        modifier = modifier,
        captionColor = captionColor,
        ringColor = ringColor,
        ringBackgroundColor = ringColor.copy(alpha = 0.12f),
    ) {
        ToolboxScanCenterIcon(centerIconRes, centerIconSize)
    }
}

@Composable
private fun ToolboxScanCenterIcon(
    @DrawableRes centerIconRes: Int,
    centerIconSize: Dp,
) {
    Image(
        painter = painterResource(centerIconRes),
        contentDescription = null,
        modifier = Modifier.size(centerIconSize),
    )
}
