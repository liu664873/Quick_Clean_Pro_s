package com.quickcleanpro.phonecleaner.feature.antivirus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusHomeAction
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusHomeUiState
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanMode

@Composable
fun AntiVirusScreen(
    state: VirusHomeUiState,
    onAction: (VirusHomeAction) -> Unit,
) {
    AntiVirusHomeView(
        onDeepScan = { onAction(VirusHomeAction.ScanRequested(VirusScanMode.Deep)) },
        onQuickScan = { onAction(VirusHomeAction.ScanRequested(VirusScanMode.Quick)) },
        onBack = { onAction(VirusHomeAction.Back) },
        enabled = !state.scanRequestPending,
    )

    BackHandler { onAction(VirusHomeAction.Back) }

    if (state.showScanNotice) {
        VirusInstalledAppsAccessDialog(
            title = stringResource(R.string.virus_app_list_permission_title),
            message = stringResource(R.string.virus_app_list_permission_message),
            primaryText = stringResource(R.string.virus_app_list_permission_agree),
            onPrimaryAction = { onAction(VirusHomeAction.ScanNoticeAccepted) },
            secondaryText = stringResource(R.string.virus_app_list_permission_not_now),
            onSecondaryAction = { onAction(VirusHomeAction.ScanNoticeDeclined) },
            onDismissRequest = { onAction(VirusHomeAction.ScanNoticeDeclined) },
            showHeroImage = true,
        )
    }

    if (state.showInstalledAppsPermissionDialog) {
        VirusInstalledAppsAccessDialog(
            title = stringResource(R.string.permission_title_required),
            message = stringResource(R.string.permission_installed_apps_desc),
            primaryText = stringResource(R.string.manage_permission),
            onPrimaryAction = { onAction(VirusHomeAction.InstalledAppsSettingsRequested) },
            secondaryText = stringResource(R.string.not_now),
            onSecondaryAction = { onAction(VirusHomeAction.InstalledAppsDialogDismissed) },
            onDismissRequest = { onAction(VirusHomeAction.InstalledAppsDialogDismissed) },
        )
    }
}
