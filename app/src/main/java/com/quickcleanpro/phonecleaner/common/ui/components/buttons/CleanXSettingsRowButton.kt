package com.quickcleanpro.phonecleaner.common.ui.components.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.cleanXPressable
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXBlue
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXMutedText
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXRowHeight
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXSubtlePanel
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXText

@Composable
fun CleanXSettingsToggleRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CleanXSettingsRowFrame(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = CleanXText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f),
        )
        CleanXMiniSwitch(checked = checked)
    }
}

@Composable
fun CleanXSettingsNavigationRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIconRes: Int = R.mipmap.ic_next,
) {
    CleanXSettingsRowFrame(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = CleanXText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailingIconRes == R.mipmap.ic_next) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = CleanXMutedText,
            )
        } else {
            Image(
                painter = painterResource(trailingIconRes),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
fun CleanXMiniSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        modifier =
            modifier
                .size(width = 34.dp, height = 20.dp)
                .scale(0.92f),
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CleanXBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = CleanXSubtlePanel,
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent,
            ),
    )
}

@Composable
private fun CleanXSettingsRowFrame(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(CleanXRowHeight)
                .cleanXPressable(
                    pressedAlpha = 0.72f,
                    pressedScale = 1f,
                    onClick = onClick,
                ),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
