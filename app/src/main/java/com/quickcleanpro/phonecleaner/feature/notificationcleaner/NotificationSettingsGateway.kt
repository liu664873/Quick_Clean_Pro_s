package com.quickcleanpro.phonecleaner.feature.notificationcleaner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

interface NotificationSettingsGateway {
    fun notificationListenerSettingsIntent(): Intent

    fun appNotificationSettingsIntent(packageName: String): Intent

    fun appDetailsSettingsIntent(packageName: String): Intent
}

class AndroidNotificationSettingsGateway : NotificationSettingsGateway {
    override fun notificationListenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    override fun appNotificationSettingsIntent(packageName: String): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            appDetailsSettingsIntent(packageName)
        }

    override fun appDetailsSettingsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
}
