package com.quickcleanpro.phonecleaner.use.app.runtime

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdRuntimeState
import com.quickcleanpro.phonecleaner.use.core.ads.DefaultInterstitialAdInterceptor
import com.quickcleanpro.phonecleaner.use.core.ads.InterstitialAdManager
import com.quickcleanpro.phonecleaner.use.core.common.operation.DefaultFeatureOperationTracker
import com.quickcleanpro.phonecleaner.use.core.common.platform.ExternalActivityLaunchHandler
import com.quickcleanpro.phonecleaner.use.core.permission.NotificationRuntimePermissionController
import com.quickcleanpro.phonecleaner.use.core.repository.SettingsRepository
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.use.feature.notification.presentation.NotificationPermissionSessionViewModel
import com.quickcleanpro.phonecleaner.use.skin.common.components.NotificationPermissionPrompt
import com.quickcleanpro.phonecleaner.use.skin.permission.CleanXPermissionCoordinatorProvider
import com.quickcleanpro.phonecleaner.use.skin.permission.NotificationPermissionPromptState
import com.quickcleanpro.phonecleaner.use.skin.permission.QuickCleanProPermissionUi
import com.quickcleanpro.phonecleaner.use.skin.permission.rememberNotificationPermissionPromptState
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun AppRuntimeHost(
    launchCoordinator: AppLaunchCoordinator,
    onNotificationPermissionGranted: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsRepository: SettingsRepository = koinInject()
    val notificationSessionViewModel: NotificationPermissionSessionViewModel = koinViewModel()
    val navController = rememberNavController()
    val activity = context.findActivity()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val pendingRequest by launchCoordinator.pendingRequest.collectAsStateWithLifecycle()

    val sessionCoordinator = remember { AppSessionCoordinator() }
    val externalActivityCoordinator = remember { ExternalActivityAdCoordinator() }
    val externalActivityLaunchHandler = remember {
        ExternalActivityLaunchHandler(
            markLaunch = externalActivityCoordinator::markLaunch,
            cancelLaunch = externalActivityCoordinator::cancelLaunch,
            markReturn = externalActivityCoordinator::markReturn,
        )
    }
    val notificationPermissionController = remember(context, externalActivityLaunchHandler) {
        NotificationRuntimePermissionController(context, externalActivityLaunchHandler)
    }
    val notificationPromptState = rememberNotificationPermissionPromptState()
    val pauseSplashForPermission =
        notificationPromptState.shouldPauseSplashForInitialNotificationRequest(
            currentRoute = currentRoute,
            splashRoute = AppDestination.Splash.route,
            settingsRepository = settingsRepository,
            permissionController = notificationPermissionController,
        )

    val latestAdRuntimeState = rememberUpdatedState(
        AdRuntimeState(
            permissionFlowActive =
                notificationPromptState.splashPermissionActive ||
                    notificationPromptState.notificationPermissionUiActive ||
                    pauseSplashForPermission ||
                    sessionCoordinator.isActive(RuntimeBusyReason.Permission),
            externalActivityReturning = externalActivityCoordinator.returning,
            scanningOrCleaning = sessionCoordinator.isActive(RuntimeBusyReason.FeatureOperation),
        ),
    )
    val interstitialAdManager = remember(activity) {
        InterstitialAdManager(
            activityProvider = { activity },
            stateProvider = { latestAdRuntimeState.value },
            onInFlightChanged = { sessionCoordinator.set(RuntimeBusyReason.Interstitial, it) },
        )
    }
    val interstitialAdInterceptor = remember(interstitialAdManager) {
        DefaultInterstitialAdInterceptor(interstitialAdManager.coordinator)
    }
    val operationTracker = remember(interstitialAdInterceptor) {
        DefaultFeatureOperationTracker(
            interstitialAdInterceptor = interstitialAdInterceptor,
            onOperationBusyChanged = { sessionCoordinator.set(RuntimeBusyReason.FeatureOperation, it) },
        )
    }

    ExternalActivityLifecycleEffect(lifecycleOwner, externalActivityCoordinator)
    ExternalActivityReturnCooldownEffect(externalActivityCoordinator)
    RouteAnalyticsEffect(currentRoute, context)

    CleanXPermissionCoordinatorProvider(
        permissionPrompt = QuickCleanProPermissionUi::PermissionPrompt,
        onPermissionFlowActiveChange = { sessionCoordinator.set(RuntimeBusyReason.Permission, it) },
        externalActivityLaunchHandler = externalActivityLaunchHandler,
    ) { permissionCoordinator ->
        AppShell(
            navController = navController,
            launchCoordinator = launchCoordinator,
            pendingRequest = pendingRequest,
            currentRoute = currentRoute,
            splashPaused = notificationPromptState.splashPermissionActive || pauseSplashForPermission,
            externalBlockingPromptActive = notificationPromptState.notificationPermissionUiActive,
            splashNotificationPermissionPrompt = {
                StorageNotificationPermissionPrompt(
                    isSplashVisible = true,
                    isHomeVisible = false,
                    settingsRepository = settingsRepository,
                    permissionController = notificationPermissionController,
                    promptState = notificationPromptState,
                    allowCustomPromptInCurrentSession = true,
                    sessionViewModel = notificationSessionViewModel,
                    onPermissionGranted = onNotificationPermissionGranted,
                )
            },
            homeNotificationPermissionPrompt = {
                StorageNotificationPermissionPrompt(
                    isSplashVisible = false,
                    isHomeVisible = true,
                    settingsRepository = settingsRepository,
                    permissionController = notificationPermissionController,
                    promptState = notificationPromptState,
                    allowCustomPromptInCurrentSession =
                        !notificationSessionViewModel.isHomeCustomPromptDeferredUntilNextLaunch,
                    sessionViewModel = notificationSessionViewModel,
                    onPermissionGranted = onNotificationPermissionGranted,
                )
            },
            interstitialAdInterceptor = interstitialAdInterceptor,
            interstitialAdInFlight = sessionCoordinator.isActive(RuntimeBusyReason.Interstitial),
            routeDependencies =
                AppRouteDependencies(
                    permissions = permissionCoordinator,
                    operations = operationTracker,
                    externalActivities = externalActivityLaunchHandler,
                ),
        )
    }
}

@Composable
private fun StorageNotificationPermissionPrompt(
    isSplashVisible: Boolean,
    isHomeVisible: Boolean,
    settingsRepository: SettingsRepository,
    permissionController: NotificationRuntimePermissionController,
    promptState: NotificationPermissionPromptState,
    allowCustomPromptInCurrentSession: Boolean,
    sessionViewModel: NotificationPermissionSessionViewModel,
    onPermissionGranted: () -> Unit,
) {
    NotificationPermissionPrompt(
        isSplashVisible = isSplashVisible,
        isHomeVisible = isHomeVisible,
        hasNotificationPermission = permissionController::hasPostNotificationsPermission,
        hasRequestedNotificationPermissionBefore = settingsRepository::hasRequestedNotificationRuntimePermissionBefore,
        saveNotificationPermissionRequestedBefore = settingsRepository::saveNotificationRuntimePermissionRequestedBefore,
        shouldShowNotificationPermissionRationale = permissionController::shouldShowPostNotificationsRationale,
        readLastCustomPromptAt = settingsRepository::readLastNotificationPermissionCustomPromptAt,
        saveLastCustomPromptAt = settingsRepository::saveLastNotificationPermissionCustomPromptAt,
        openAppSettings = permissionController::openAppSettings,
        allowCustomPromptInCurrentSession = allowCustomPromptInCurrentSession,
        onHomeSystemPermissionRejectedThisSession = sessionViewModel::markHomeCustomPromptDeferredUntilNextLaunch,
        onPermissionGranted = onPermissionGranted,
        onSplashPermissionActiveChange = { promptState.splashPermissionActive = it },
        onPermissionUiActiveChange = { promptState.notificationPermissionUiActive = it },
    )
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
