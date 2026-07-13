package com.quickcleanpro.phonecleaner.use.skin.settings.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.quickcleanpro.phonecleaner.use.skin.common.components.buttons.CleanXSettingsToggleRow
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXCardColor
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXCardShape

internal data class PermissionRowUi(
    val label: String,
    val checked: Boolean,
    val onClick: () -> Unit,
)

@Composable
internal fun ManagePermissionsContent(
    rows: List<PermissionRowUi>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CleanXCardColor,
        shape = CleanXCardShape,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            rows.forEachIndexed { index, row ->
                CleanXSettingsToggleRow(
                    label = row.label,
                    checked = row.checked,
                    onClick = row.onClick,
                )
            }
        }
    }
}
