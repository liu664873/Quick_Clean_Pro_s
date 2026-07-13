package com.quickcleanpro.phonecleaner.feature.applock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.applock.AppLockUiState

@Composable
internal fun AppLockSettingsView(
    uiState: AppLockUiState,
    onMonitoringChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onStartChangePin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        CheckActionCard(
            title = stringResource(R.string.enable),
            checked = uiState.monitoringEnabled,
            onClick = { onMonitoringChange(!uiState.monitoringEnabled) }
        )
        CheckActionCard(
            title = stringResource(R.string.haptic_feedback),
            checked = uiState.vibrationEnabled,
            onClick = { onVibrationChange(!uiState.vibrationEnabled) }
        )
        SettingsNavigationCard(
            label = stringResource(R.string.change_pin),
            onClick = onStartChangePin
        )
    }
}

@Composable
private fun SettingsNavigationCard(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable { onClick() },
        color = AppLockCardColor,
        shape = RoundedCornerShape(AppLockCardRadius)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = AppLockNavy,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Image(
                painter = painterResource(R.mipmap.ic_next),
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
