package com.quickcleanpro.phonecleaner.app.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.AppUsageRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.BatteryInfoRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.DeviceInfoRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.NetworkSpeedRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.NetworkUsageRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner.WhatsAppCleanerRoute
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationCleanerRoute
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.DeviceInfoViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.BatteryInfoViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.AppUsageViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.NetworkUsageViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.NetworkSpeedViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner.WhatsAppCleanerViewModel
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationCleanerViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerToolboxRoutes(
    navigator: AppNavigator,
    featureFlow: FeatureFlowRuntime,
    externalActivities: ExternalActivityLauncher,
) {
    composable(AppDestination.DeviceInfo.route) {
        DeviceInfoRoute(
            navigator = navigator,
            viewModel = koinViewModel<DeviceInfoViewModel>(),
            featureFlow = featureFlow,
        )
    }
    composable(AppDestination.BatteryInfo.route) {
        BatteryInfoRoute(
            navigator = navigator,
            viewModel = koinViewModel<BatteryInfoViewModel>(),
            featureFlow = featureFlow,
        )
    }
    composable(AppDestination.AppUsage.route) {
        AppUsageRoute(
            navigator = navigator,
            viewModel = koinViewModel<AppUsageViewModel>(),
            featureFlow = featureFlow,
            externalActivities = externalActivities,
        )
    }
    composable(AppDestination.NetworkUsage.route) {
        NetworkUsageRoute(
            navigator = navigator,
            viewModel = koinViewModel<NetworkUsageViewModel>(),
            featureFlow = featureFlow,
        )
    }
    composable(AppDestination.NetworkScan.route) {
        NetworkScanRoute(
            navigator = navigator,
            viewModel = koinViewModel<NetworkScanViewModel>(),
            featureFlow = featureFlow,
            externalActivities = externalActivities,
        )
    }
    composable(AppDestination.NetworkScanDevices.route) {
        NetworkScanDevicesRoute(navigator, koinViewModel<NetworkScanDevicesViewModel>())
    }
    composable(AppDestination.NetworkSpeed.route) {
        NetworkSpeedRoute(
            navigator = navigator,
            viewModel = koinViewModel<NetworkSpeedViewModel>(),
            featureFlow = featureFlow,
        )
    }
    composable(AppDestination.WhatsAppCleaner.route) {
        WhatsAppCleanerRoute(
            navigator = navigator,
            viewModel = koinViewModel<WhatsAppCleanerViewModel>(),
            featureFlow = featureFlow,
        )
    }
    composable(AppDestination.NotificationCleaner.route) {
        NotificationCleanerRoute(
            navigator = navigator,
            viewModel = koinViewModel<NotificationCleanerViewModel>(),
            featureFlow = featureFlow,
        )
    }
}
