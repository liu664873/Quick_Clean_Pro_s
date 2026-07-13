package com.quickcleanpro.phonecleaner.use.skin.antivirus

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.common.operation.trackReturnHome
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.use.core.permission.appSettingsIntent
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import com.quickcleanpro.phonecleaner.use.skin.antivirus.views.AntiVirusHomeView
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.VirusInstalledAppsAccessDialog
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanMode
import com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation.VirusScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val INSTALLED_APPS_DIALOG_INTERACTION_THRESHOLD_MS = 1_500L

@Composable
fun AntiVirusScreen(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: VirusScanViewModel) {
    val router = navigator
    val permissionCoordinator = dependencies.permissions
    val tracker = dependencies.operations
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val externalActivityLaunchHandler = dependencies.externalActivities
    val scope = rememberCoroutineScope()
    var pendingScanMode by remember { mutableStateOf<VirusScanMode?>(null) }
    var scanPermissionPending by remember { mutableStateOf(false) }
    var showNotice by remember { mutableStateOf(false) }
    var showInstalledAppsPermissionDialog by remember { mutableStateOf(false) }
    var waitingForSettingsReturn by remember { mutableStateOf(false) }

    fun rememberInstalledAppsAccessFailure() {
        viewModel.setInstalledAppsAccessFailed(true)
    }

    fun clearInstalledAppsAccessFailure() {
        viewModel.setInstalledAppsAccessFailed(false)
    }

    fun launchScan(mode: VirusScanMode) {
        fun navigateToScan() {
            router.open(
                when (mode) {
                    VirusScanMode.Quick -> AppDestination.VirusQuickScan
                    VirusScanMode.Deep -> AppDestination.VirusDeepScan
                },
            )
        }

        if (mode == VirusScanMode.Deep) {
            permissionCoordinator.guard(
                action = CleanXProtectedAction.VirusDeepScanStart,
                onGranted = {
                    scanPermissionPending = false
                    navigateToScan()
                },
                onRejected = {
                    scanPermissionPending = false
                },
            )
        } else {
            scanPermissionPending = false
            navigateToScan()
        }
    }

    fun launchScanAfterInstalledAppsAccess(mode: VirusScanMode) {
        if (scanPermissionPending) return
        scanPermissionPending = true
        scope.launch {
            val hadPreviousFailure = viewModel.hasInstalledAppsAccessFailedBefore()
            val startedAt = System.currentTimeMillis()
            val hasAccess =
                withContext(Dispatchers.IO) {
                    viewModel.hasInstalledAppsAccess()
                }
            val elapsed = System.currentTimeMillis() - startedAt
            if (!hasAccess) {
                rememberInstalledAppsAccessFailure()
                scanPermissionPending = false
                pendingScanMode = mode
                showInstalledAppsPermissionDialog =
                    hadPreviousFailure &&
                    elapsed < INSTALLED_APPS_DIALOG_INTERACTION_THRESHOLD_MS
                return@launch
            }
            clearInstalledAppsAccessFailure()
            launchScan(mode)
        }
    }

    fun retryPendingScanAfterSettingsReturn() {
        val mode = pendingScanMode ?: return
        if (scanPermissionPending) return
        scanPermissionPending = true
        scope.launch {
            val hasAccess =
                withContext(Dispatchers.IO) {
                    viewModel.hasInstalledAppsAccess()
                }
            if (hasAccess) {
                clearInstalledAppsAccessFailure()
                showInstalledAppsPermissionDialog = false
                launchScan(mode)
            } else {
                rememberInstalledAppsAccessFailure()
                scanPermissionPending = false
                showInstalledAppsPermissionDialog = false
            }
        }
    }

    fun openInstalledAppsPermissionSettings() {
        externalActivityLaunchHandler.markLaunch()
        try {
            waitingForSettingsReturn = true
            context.startActivity(appSettingsIntent(context))
        } catch (_: ActivityNotFoundException) {
            waitingForSettingsReturn = false
            externalActivityLaunchHandler.cancelLaunch()
            scanPermissionPending = false
            showInstalledAppsPermissionDialog = true
        } catch (_: Exception) {
            waitingForSettingsReturn = false
            externalActivityLaunchHandler.cancelLaunch()
            scanPermissionPending = false
            showInstalledAppsPermissionDialog = true
        }
    }

    fun requestScan(mode: VirusScanMode) {
        if (viewModel.isScanNoticeAccepted()) {
            launchScanAfterInstalledAppsAccess(mode)
        } else {
            pendingScanMode = mode
            showNotice = true
        }
    }

    fun exitBackWithReturnAd() {
        tracker.trackReturnHome(FeatureKey.ANTI_VIRUS) {
            router.back()
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.isScanNoticeAccepted()) {
            showNotice = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && waitingForSettingsReturn) {
                    waitingForSettingsReturn = false
                    retryPendingScanAfterSettingsReturn()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AntiVirusHomeView(
        onDeepScan = {
            requestScan(VirusScanMode.Deep)
        },
        onQuickScan = {
            requestScan(VirusScanMode.Quick)
        },
        onBack = ::exitBackWithReturnAd,
        enabled = !scanPermissionPending,
    )

    BackHandler(onBack = ::exitBackWithReturnAd)

    if (showNotice) {
        VirusInstalledAppsAccessDialog(
            title = stringResource(R.string.virus_app_list_permission_title),
            message = stringResource(R.string.virus_app_list_permission_message),
            primaryText = stringResource(R.string.virus_app_list_permission_agree),
            onPrimaryAction = {
                viewModel.acceptScanNotice()
                showNotice = false
                pendingScanMode?.let { launchScanAfterInstalledAppsAccess(it) }
            },
            secondaryText = stringResource(R.string.virus_app_list_permission_not_now),
            onSecondaryAction = {
                showNotice = false
                exitBackWithReturnAd()
            },
            onDismissRequest = {
                showNotice = false
                exitBackWithReturnAd()
            },
            showHeroImage = true,
        )
    }

    if (showInstalledAppsPermissionDialog) {
        VirusInstalledAppsAccessDialog(
            title = stringResource(R.string.permission_title_required),
            message = stringResource(R.string.permission_installed_apps_desc),
            primaryText = stringResource(R.string.manage_permission),
            onPrimaryAction = {
                showInstalledAppsPermissionDialog = false
                openInstalledAppsPermissionSettings()
            },
            secondaryText = stringResource(R.string.not_now),
            onSecondaryAction = {
                showInstalledAppsPermissionDialog = false
                scanPermissionPending = false
            },
            onDismissRequest = {
                showInstalledAppsPermissionDialog = false
                scanPermissionPending = false
            },
        )
    }
}
