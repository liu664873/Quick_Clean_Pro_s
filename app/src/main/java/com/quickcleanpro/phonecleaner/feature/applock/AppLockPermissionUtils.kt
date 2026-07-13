package com.quickcleanpro.phonecleaner.feature.applock

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.provider.Settings

object AppLockPermissionUtils {
    fun canDrawOverlays(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps =
            runCatching {
                context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            }.getOrNull() ?: return false
        val mode =
            runCatching {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            }.getOrDefault(AppOpsManager.MODE_ERRORED)
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
