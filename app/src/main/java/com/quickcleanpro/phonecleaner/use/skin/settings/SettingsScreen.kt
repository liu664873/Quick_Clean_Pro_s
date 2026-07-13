package com.quickcleanpro.phonecleaner.use.skin.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.common.components.cleanXPressable
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.SettingsRateDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.TemperatureUnitDialog
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXCardColor
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXCardShape
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXContentPadding
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXMutedText
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXRowHeight
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXText

@Composable
fun SettingsScreen(
    temperatureUnit: String,
    showAdPrivacyOptions: Boolean,
    onBack: () -> Unit,
    onOpenManagePermissions: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAdPrivacy: () -> Unit,
    onTemperatureUnitChanged: (String) -> Unit,
) {
    var showTemperatureDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    CleanXScaffoldPage(
        title = stringResource(R.string.nav_settings),
        onBack = onBack,
        contentPadding = PaddingValues(horizontal = CleanXContentPadding, vertical = 0.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        SettingsPanel {
            SettingsValueRow(
                label = stringResource(R.string.settings_manage_permissions),
                value = null,
                onClick = onOpenManagePermissions,
            )
            SettingsValueRow(
                label = stringResource(R.string.settings_temperature_unit),
                value = "${'\u00B0'}$temperatureUnit",
                onClick = { showTemperatureDialog = true },
            )
            SettingsValueRow(
                label = stringResource(R.string.settings_terms_of_service),
                value = null,
                onClick = onOpenTerms,
            )
            SettingsValueRow(
                label = stringResource(R.string.settings_privacy_policy),
                value = null,
                onClick = onOpenPrivacy,
            )
            if (showAdPrivacyOptions) {
                SettingsValueRow(
                    label = stringResource(R.string.settings_ad_privacy_options),
                    value = null,
                    onClick = onOpenAdPrivacy,
                )
            }
            SettingsValueRow(
                label = stringResource(R.string.settings_rate_us),
                value = null,
                onClick = { showRateDialog = true },
            )
        }
    }

    if (showTemperatureDialog) {
        TemperatureUnitDialog(
            selected = temperatureUnit,
            onDismiss = { showTemperatureDialog = false },
            onSelected = { unit ->
                val normalized = unit.normalizeTemperatureUnit()
                onTemperatureUnitChanged(normalized)
                showTemperatureDialog = false
            },
        )
    }

    if (showRateDialog) {
        SettingsRateDialog(onDismiss = { showRateDialog = false })
    }
}

@Composable
private fun SettingsPanel(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CleanXCardColor,
        shape = CleanXCardShape,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
            content = content,
        )
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CleanXRowHeight)
            .cleanXPressable(
                pressedAlpha = 0.72f,
                pressedScale = 1f,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = CleanXText,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                color = CleanXText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = CleanXMutedText,
            )
        }
    }
}

@Composable
internal fun SettingsDivider() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
        color = Color.Transparent,
    ) {}
}

internal fun String.normalizeTemperatureUnit(): String =
    if (equals("F", ignoreCase = true)) "F" else "C"
