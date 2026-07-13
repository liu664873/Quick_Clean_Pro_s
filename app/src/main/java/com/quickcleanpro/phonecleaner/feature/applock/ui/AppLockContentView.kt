package com.quickcleanpro.phonecleaner.feature.applock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quickcleanpro.phonecleaner.feature.applock.AppLockPage
import com.quickcleanpro.phonecleaner.feature.applock.AppLockAction
import com.quickcleanpro.phonecleaner.feature.applock.AppLockUiState

@Composable
internal fun AppLockContentView(
    uiState: AppLockUiState,
    onAction: (AppLockAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLockBackground)
    ) {
        when (uiState.page) {
            AppLockPage.SelectApps -> AppLockSelectAppsView(
                uiState = uiState,
                onTogglePackage = { onAction(AppLockAction.TogglePackage(it)) }
            )
            AppLockPage.Pin -> AppLockPinView(
                uiState = uiState,
                onDigit = { onAction(AppLockAction.AddPinDigit(it)) },
                onDelete = { onAction(AppLockAction.RemovePinDigit) }
            )
            AppLockPage.Manage -> AppLockManageView(
                uiState = uiState,
                onOpenSearch = { onAction(AppLockAction.OpenSearch) },
                onTogglePackage = { onAction(AppLockAction.TogglePackage(it)) },
                onToggleAll = { onAction(AppLockAction.ToggleAllApps) },
                onAutoLockChange = { onAction(AppLockAction.AutoLockChanged(it)) }
            )
            AppLockPage.Search -> AppLockSearchView(
                uiState = uiState,
                onSearch = { onAction(AppLockAction.SearchQueryChanged(it)) },
                onTogglePackage = { onAction(AppLockAction.TogglePackage(it)) }
            )
            AppLockPage.Settings -> AppLockSettingsView(
                uiState = uiState,
                onMonitoringChange = { onAction(AppLockAction.MonitoringChanged(it)) },
                onVibrationChange = { onAction(AppLockAction.VibrationChanged(it)) },
                onStartChangePin = { onAction(AppLockAction.StartChangePin) }
            )
        }
    }
}
