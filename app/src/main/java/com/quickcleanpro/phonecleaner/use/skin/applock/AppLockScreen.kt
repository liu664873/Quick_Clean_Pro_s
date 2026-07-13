package com.quickcleanpro.phonecleaner.use.skin.applock

import com.quickcleanpro.phonecleaner.use.feature.applock.presentation.AppLockPage
import com.quickcleanpro.phonecleaner.use.feature.applock.presentation.AppLockPinStep
import com.quickcleanpro.phonecleaner.use.feature.applock.presentation.AppLockUiState
import com.quickcleanpro.phonecleaner.use.feature.applock.presentation.AppLockViewModel
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.common.operation.trackReturnHome
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXIconButtonSize
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXPillShape
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.use.skin.applock.views.AppLockBottomBar
import com.quickcleanpro.phonecleaner.use.skin.applock.views.AppLockContentView

@Composable
internal fun AppLockRoute(
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
    viewModel: AppLockViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val permissionCoordinator = dependencies.permissions
    val tracker = dependencies.operations
    val toastRes = uiState.toastRes
    val toastMessage = toastRes?.let { stringResource(it) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAfterResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(uiState.overlayPermissionRequired) {
        if (uiState.overlayPermissionRequired) {
            permissionCoordinator.guard(
                action = CleanXProtectedAction.AppLockRequestOverlay,
                onGranted = viewModel::consumeOverlayPermissionRequest,
                onRejected = viewModel::consumeOverlayPermissionRequest,
            )
        }
    }

    AppLockScreen(
        uiState = uiState,
        onExitBack = {
            tracker.trackReturnHome(FeatureKey.APP_LOCK) { navigator.back() }
        },
        onLeavePinPage = viewModel::leavePinPage,
        onReturnToManage = viewModel::returnToManage,
        onOpenSearch = viewModel::openSearch,
        onOpenSettings = viewModel::openSettings,
        onSearch = viewModel::updateSearchQuery,
        onTogglePackage = { packageName ->
            if (uiState.page == AppLockPage.SelectApps) {
                viewModel.togglePackage(packageName)
            } else {
                permissionCoordinator.guard(CleanXProtectedAction.AppLockOpenProtectedArea) {
                    viewModel.togglePackage(packageName)
                }
            }
        },
        onToggleAll = {
            permissionCoordinator.guard(CleanXProtectedAction.AppLockOpenProtectedArea) {
                viewModel.toggleAllApps()
            }
        },
        onBeginCreatePin = {
            permissionCoordinator.guard(CleanXProtectedAction.AppLockOpenProtectedArea) {
                viewModel.beginCreatePin()
            }
        },
        onStartChangePin = viewModel::startChangePin,
        onDigit = viewModel::addPinDigit,
        onDeleteDigit = viewModel::removePinDigit,
        onMonitoringChange = { enabled ->
            if (enabled) {
                permissionCoordinator.guard(CleanXProtectedAction.AppLockEnableMonitoring) {
                    viewModel.setMonitoringEnabled(true)
                }
            } else {
                viewModel.setMonitoringEnabled(false)
            }
        },
        onAutoLockChange = viewModel::setAutoLockEnabled,
        onVibrationChange = viewModel::setVibrationEnabled,
    )
}

@Composable
private fun AppLockScreen(
    uiState: AppLockUiState,
    onExitBack: () -> Unit,
    onLeavePinPage: () -> Unit,
    onReturnToManage: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: (String) -> Unit,
    onTogglePackage: (String) -> Unit,
    onToggleAll: () -> Unit,
    onBeginCreatePin: () -> Unit,
    onStartChangePin: () -> Unit,
    onDigit: (Char) -> Unit,
    onDeleteDigit: () -> Unit,
    onMonitoringChange: (Boolean) -> Unit,
    onAutoLockChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
) {
    fun handleBack() {
        when (uiState.page) {
            AppLockPage.Search,
            AppLockPage.Settings -> onReturnToManage()
            AppLockPage.Pin -> {
                if (uiState.pinStep == AppLockPinStep.Verify) {
                    onExitBack()
                } else {
                    onLeavePinPage()
                }
            }
            else -> onExitBack()
        }
    }

    BackHandler(onBack = ::handleBack)

    CleanXScaffoldPage(
        title = stringResource(
            when (uiState.page) {
                AppLockPage.Settings -> R.string.setting
                AppLockPage.Pin -> uiState.pinStep.titleRes
                else -> R.string.app_lock
            }
        ),
        scrollEnabled = false,
        contentPadding = PaddingValues(0.dp),
        onBack = ::handleBack,
        actions = {
            if (uiState.page == AppLockPage.Manage) {
                Box(
                    modifier = Modifier
                        .size(CleanXIconButtonSize)
                        .clip(CleanXPillShape)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_setting),
                        contentDescription = stringResource(R.string.setting),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (uiState.page == AppLockPage.SelectApps) {
                AppLockBottomBar(
                    text = stringResource(R.string.lock_selected_apps),
                    onClick = onBeginCreatePin,
                    enabled = uiState.hasSelectedApps
                )
            }
        }
    ) {
        AppLockContentView(
            uiState = uiState,
            onOpenSearch = onOpenSearch,
            onSearch = onSearch,
            onTogglePackage = onTogglePackage,
            onToggleAll = onToggleAll,
            onStartChangePin = onStartChangePin,
            onDigit = onDigit,
            onDeleteDigit = onDeleteDigit,
            onMonitoringChange = onMonitoringChange,
            onAutoLockChange = onAutoLockChange,
            onVibrationChange = onVibrationChange,
        )
    }
}
