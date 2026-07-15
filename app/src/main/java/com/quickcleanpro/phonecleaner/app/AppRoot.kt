package com.quickcleanpro.phonecleaner.app

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quickcleanpro.phonecleaner.common.ads.AdRuntimeState
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.app.navigation.AppNavGraph
import com.quickcleanpro.phonecleaner.app.runtime.AppSessionCoordinator
import com.quickcleanpro.phonecleaner.app.runtime.AdRuntimeLifecycleEffect
import com.quickcleanpro.phonecleaner.app.runtime.InterstitialInteractionBlocker
import com.quickcleanpro.phonecleaner.app.runtime.NotificationLaunchEffect
import com.quickcleanpro.phonecleaner.app.runtime.RouteAnalyticsEffect
import com.quickcleanpro.phonecleaner.app.runtime.RuntimeBusyReason
import com.quickcleanpro.phonecleaner.app.runtime.rememberAppRuntimeBindings
import com.quickcleanpro.phonecleaner.app.runtime.permission.AppPermissionHost
import com.quickcleanpro.phonecleaner.app.runtime.permission.NotificationPermissionHost
import com.quickcleanpro.phonecleaner.app.runtime.permission.NotificationPermissionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(
    launchCoordinator: AppLaunchCoordinator,
    onNotificationPermissionGranted: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationPermissionViewModel: NotificationPermissionViewModel = koinViewModel()
    val notificationState by notificationPermissionViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val pendingRequest by launchCoordinator.pendingRequest.collectAsStateWithLifecycle()

    val sessionCoordinator = remember { AppSessionCoordinator() }
    val runtimeBindings =
        rememberAppRuntimeBindings(
            context = context,
            adRuntimeState =
                AdRuntimeState(
                    permissionFlowActive =
                        notificationState.permissionFlowActive ||
                            sessionCoordinator.isActive(RuntimeBusyReason.Permission),
                    featureOperationActive = sessionCoordinator.isActive(RuntimeBusyReason.FeatureOperation),
                ),
            sessionCoordinator = sessionCoordinator,
        )
    AdRuntimeLifecycleEffect(lifecycleOwner, runtimeBindings.ads)
    RouteAnalyticsEffect(currentRoute, context, runtimeBindings.ads)

    AppPermissionHost(
        externalActivityLauncher = runtimeBindings.externalActivities,
        onPermissionFlowActiveChange = { sessionCoordinator.set(RuntimeBusyReason.Permission, it) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavGraph(
                navController = navController,
                launchCoordinator = launchCoordinator,
                runtime = runtimeBindings,
                splashPaused = notificationState.splashPaused,
                notificationPermissionUiActive = notificationState.permissionUiActive,
            )
            InterstitialInteractionBlocker(sessionCoordinator.isActive(RuntimeBusyReason.Interstitial))
        }
        NotificationPermissionHost(
            viewModel = notificationPermissionViewModel,
            currentRoute = currentRoute,
            externalActivityLauncher = runtimeBindings.externalActivities,
            onPermissionGranted = onNotificationPermissionGranted,
        )
        NotificationLaunchEffect(pendingRequest, currentRoute, navController)
    }
}
