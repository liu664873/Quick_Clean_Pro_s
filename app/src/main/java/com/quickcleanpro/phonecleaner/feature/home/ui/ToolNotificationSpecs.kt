package com.quickcleanpro.phonecleaner.feature.home.ui

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.quickcleanpro.phonecleaner.R

data class ToolNotificationSpec(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val route: String,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val actionRes: Int,
)

val ToolNotificationSpecs: List<ToolNotificationSpec> =
    listOf(
        ToolNotificationSpec(
            titleRes = R.string.device_info,
            descriptionRes = R.string.common_tool_device_desc,
            route = AppDestination.DeviceInfo.route,
            iconRes = R.drawable.ic_n_device_info,
            actionRes = R.string.view_now,
        ),
        ToolNotificationSpec(
            titleRes = R.string.junk_removal,
            descriptionRes = R.string.notification_tool_junk_desc,
            route = AppDestination.JunkClean.route,
            iconRes = R.drawable.ic_n_junk_removal,
            actionRes = R.string.scan_now,
        ),
        ToolNotificationSpec(
            titleRes = R.string.battery_info,
            descriptionRes = R.string.common_tool_battery_desc,
            route = AppDestination.BatteryInfo.route,
            iconRes = R.drawable.ic_n_battery_info,
            actionRes = R.string.view_now,
        ),
        ToolNotificationSpec(
            titleRes = R.string.network_scan,
            descriptionRes = R.string.notification_tool_network_scan_desc,
            route = AppDestination.NetworkScan.route,
            iconRes = R.drawable.ic_n_network_scan,
            actionRes = R.string.scan_now,
        ),
        ToolNotificationSpec(
            titleRes = R.string.network_usage,
            descriptionRes = R.string.notification_tool_network_usage_desc,
            route = AppDestination.NetworkUsage.route,
            iconRes = R.drawable.ic_n_network_usage,
            actionRes = R.string.view_now,
        ),
        ToolNotificationSpec(
            titleRes = R.string.notification_cleaner,
            descriptionRes = R.string.common_tool_notification_cleaner_desc,
            route = AppDestination.NotificationCleaner.route,
            iconRes = R.drawable.ic_n_notification_cleaner,
            actionRes = R.string.check_now,
        ),
    )

internal fun toolNotificationSpec(route: String): ToolNotificationSpec? =
    ToolNotificationSpecs.firstOrNull { it.route == route }
