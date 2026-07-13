package com.quickcleanpro.phonecleaner.use.skin.settings

import com.quickcleanpro.phonecleaner.use.feature.settings.presentation.ManagePermissionsViewModel
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionCoordinator
import com.quickcleanpro.phonecleaner.use.skin.settings.views.ManagePermissionsContent
import com.quickcleanpro.phonecleaner.use.skin.settings.views.PermissionRowUi
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXContentPadding

@Composable
fun ManagePermissionsScreen(
    permissionCoordinator: CleanXPermissionCoordinator,
    viewModel: ManagePermissionsViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.onResume(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.load(context)
    }

    CleanXScaffoldPage(
        title = stringResource(R.string.settings_manage_permissions),
        contentPadding = PaddingValues(horizontal = CleanXContentPadding, vertical = 0.dp),
    ) {
        ManagePermissionsContent(
            rows =
                uiState.rows.map { row ->
                    PermissionRowUi(
                        label = stringResource(row.labelRes),
                        checked = row.checked,
                        onClick = {
                            permissionCoordinator.openSettings(row.item) {
                                viewModel.refresh(context)
                            }
                        },
                    )
                },
        )
        Spacer(modifier = Modifier.height(100.dp))
    }
}
