package com.quickcleanpro.phonecleaner.feature.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.common.permission.AppPermissionCoordinator
import com.quickcleanpro.phonecleaner.feature.settings.ui.ManagePermissionsContent
import com.quickcleanpro.phonecleaner.feature.settings.ui.PermissionRowUi
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXContentPadding
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator


import com.quickcleanpro.phonecleaner.feature.settings.ui.ManagePermissionsScreen

@Composable
fun ManagePermissionsRoute(
    navigator: AppNavigator,
    permissionCoordinator: AppPermissionCoordinator,
    viewModel: ManagePermissionsViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    fun refreshStatuses() {
        viewModel.onAction(
            ManagePermissionsAction.StatusesChanged(
                uiState.rows.associate { row -> row.item to permissionCoordinator.isGranted(row.item) },
            ),
        )
    }

    DisposableEffect(lifecycleOwner, viewModel, permissionCoordinator) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshStatuses()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, permissionCoordinator) {
        refreshStatuses()
    }

    ManagePermissionsScreen(
        state = uiState,
        onAction = { action ->
            when (action) {
                ManagePermissionsAction.Back -> navigator.back()
                ManagePermissionsAction.Refresh -> refreshStatuses()
                is ManagePermissionsAction.PermissionClicked -> {
                    permissionCoordinator.openSettings(action.item) {
                        refreshStatuses()
                    }
                }
                is ManagePermissionsAction.StatusesChanged -> viewModel.onAction(action)
            }
        },
        onNavigate = navigator::open,
    )
}
