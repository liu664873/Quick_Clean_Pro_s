package com.quickcleanpro.phonecleaner.feature.settings.ui

import com.quickcleanpro.phonecleaner.feature.settings.*

import com.quickcleanpro.phonecleaner.feature.settings.ManagePermissionsViewModel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.feature.settings.ui.ManagePermissionsContent
import com.quickcleanpro.phonecleaner.feature.settings.ui.PermissionRowUi
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXContentPadding
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

@Composable
fun ManagePermissionsScreen(
    state: ManagePermissionsUiState,
    onAction: (ManagePermissionsAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {

    CleanXScaffoldPage(
        title = stringResource(R.string.settings_manage_permissions),
        modifier = modifier,
        onBack = { onAction(ManagePermissionsAction.Back) },
        contentPadding = PaddingValues(horizontal = CleanXContentPadding, vertical = 0.dp),
    ) {
        ManagePermissionsContent(
            rows =
                state.rows.map { row ->
                    PermissionRowUi(
                        label = stringResource(row.labelRes),
                        checked = row.checked,
                        onClick = {
                            onAction(ManagePermissionsAction.PermissionClicked(row.item))
                        },
                    )
                },
        )
        Spacer(modifier = Modifier.height(100.dp))
    }
}
