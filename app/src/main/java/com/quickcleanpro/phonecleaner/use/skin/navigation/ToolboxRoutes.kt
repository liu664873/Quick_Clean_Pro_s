package com.quickcleanpro.phonecleaner.use.skin.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.quickcleanpro.phonecleaner.use.skin.toolbox.appusage.AppUsageRoute
import com.quickcleanpro.phonecleaner.use.skin.toolbox.battery.BatteryInfoRoute
import com.quickcleanpro.phonecleaner.use.skin.toolbox.deviceinfo.DeviceInfoRoute
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan.NetworkScanDevicesScreen
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkscan.NetworkScanRoute
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkspeed.NetworkSpeedRoute
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkusage.NetworkUsageRoute
import com.quickcleanpro.phonecleaner.use.skin.toolbox.notificationcleaner.NotificationCleanerScreen
import com.quickcleanpro.phonecleaner.use.skin.toolbox.whatsappcleaner.WhatsAppCleanerScreen
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.device.DeviceInfoViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.device.BatteryInfoViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.appusage.AppUsageViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkusage.NetworkUsageViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkscan.NetworkScanDevicesViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.whatsappcleaner.WhatsAppCleanerViewModel
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.notification.NotificationCleanerViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.registerToolboxRoutes(navigator: AppNavigator, dependencies: AppRouteDependencies) {
    composable(AppDestination.DeviceInfo.route) {
        DeviceInfoRoute(navigator, dependencies, koinViewModel<DeviceInfoViewModel>())
    }
    composable(AppDestination.BatteryInfo.route) {
        BatteryInfoRoute(navigator, dependencies, koinViewModel<BatteryInfoViewModel>())
    }
    composable(AppDestination.AppUsage.route) {
        AppUsageRoute(navigator, dependencies, koinViewModel<AppUsageViewModel>())
    }
    composable(AppDestination.NetworkUsage.route) {
        NetworkUsageRoute(navigator, dependencies, koinViewModel<NetworkUsageViewModel>())
    }
    composable(AppDestination.NetworkScan.route) {
        NetworkScanRoute(navigator, dependencies, koinViewModel<NetworkScanViewModel>())
    }
    composable(AppDestination.NetworkScanDevices.route) {
        NetworkScanDevicesScreen(viewModel = koinViewModel<NetworkScanDevicesViewModel>())
    }
    composable(AppDestination.NetworkSpeed.route) {
        NetworkSpeedRoute(navigator, dependencies, koinViewModel<NetworkSpeedViewModel>())
    }
    composable(AppDestination.WhatsAppCleaner.route) {
        WhatsAppCleanerScreen(navigator, dependencies, koinViewModel<WhatsAppCleanerViewModel>())
    }
    composable(AppDestination.NotificationCleaner.route) {
        NotificationCleanerScreen(navigator, dependencies, koinViewModel<NotificationCleanerViewModel>())
    }
}
