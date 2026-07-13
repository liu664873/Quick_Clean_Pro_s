package com.quickcleanpro.phonecleaner.feature.antivirus

data class VirusHomeUiState(
    val pendingScanMode: VirusScanMode? = null,
    val scanRequestPending: Boolean = false,
    val showScanNotice: Boolean = false,
    val showInstalledAppsPermissionDialog: Boolean = false,
    val waitingForSettingsReturn: Boolean = false,
)

sealed interface VirusHomeAction {
    data object Entered : VirusHomeAction
    data object Back : VirusHomeAction
    data class ScanRequested(val mode: VirusScanMode) : VirusHomeAction
    data object ScanNoticeAccepted : VirusHomeAction
    data object ScanNoticeDeclined : VirusHomeAction
    data object InstalledAppsSettingsRequested : VirusHomeAction
    data object InstalledAppsDialogDismissed : VirusHomeAction
    data object Resumed : VirusHomeAction
    data object SettingsLaunchFailed : VirusHomeAction
    data class DeepScanPermissionResult(val granted: Boolean) : VirusHomeAction
}

sealed interface VirusHomeEffect {
    data object Exit : VirusHomeEffect
    data object OpenInstalledAppsSettings : VirusHomeEffect
    data class RequestDeepScanPermission(val mode: VirusScanMode) : VirusHomeEffect
    data class NavigateToScan(val mode: VirusScanMode) : VirusHomeEffect
}
