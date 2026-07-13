package com.quickcleanpro.phonecleaner.common.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher

class NotificationRuntimePermissionController(
    private val context: Context,
    private val externalActivityLaunchHandler: ExternalActivityLauncher = ExternalActivityLauncher(),
) {
    fun hasPostNotificationsPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            runCatching {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)

    fun shouldShowPostNotificationsRationale(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.findActivity()?.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS,
            ) == true

    fun openAppSettings(): Boolean {
        externalActivityLaunchHandler.markLaunch()
        return runCatching {
            context.startActivity(appSettingsIntent(context))
        }.onFailure {
            externalActivityLaunchHandler.cancelLaunch()
        }.isSuccess
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
