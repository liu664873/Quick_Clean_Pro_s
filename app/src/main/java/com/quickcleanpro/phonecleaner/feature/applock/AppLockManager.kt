package com.quickcleanpro.phonecleaner.feature.applock

import com.quickcleanpro.phonecleaner.feature.applock.AppLockRepositoryImpl

object AppLockManager {
    fun isMonitoringEnabled(): Boolean = AppPrefsUtils.getBoolean(AppLockRepositoryImpl.KEY_MONITORING_ENABLED, true)

    fun isAppLocked(packageName: String): Boolean {
        val lockedStr = AppPrefsUtils.getString(AppLockRepositoryImpl.KEY_LOCKED_PACKAGES, "")
        if (lockedStr.isBlank()) return false
        return packageName in lockedStr.split(',').filter { it.isNotBlank() }.toSet()
    }
}
