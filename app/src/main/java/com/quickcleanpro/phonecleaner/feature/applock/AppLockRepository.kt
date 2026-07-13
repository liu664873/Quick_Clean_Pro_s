package com.quickcleanpro.phonecleaner.feature.applock

import com.quickcleanpro.phonecleaner.feature.applock.AppLockApp

interface AppLockRepository {
    fun isPinSet(): Boolean

    fun savePin(pin: String)

    fun verifyPin(pin: String): Boolean

    fun lockedPackages(): Set<String>

    fun lockedAppCount(): Int

    fun isPackageLocked(packageName: String): Boolean

    fun setPackageLocked(
        packageName: String,
        locked: Boolean,
    )

    fun setLockedPackages(packageNames: Set<String>)

    suspend fun lockableApps(): List<AppLockApp>

    fun isMonitoringEnabled(): Boolean

    fun setMonitoringEnabled(enabled: Boolean)

    fun isAutoLockEnabled(): Boolean

    fun setAutoLockEnabled(enabled: Boolean)

    fun isVibrationEnabled(): Boolean

    fun setVibrationEnabled(enabled: Boolean)

    fun hasUsageAccess(): Boolean

    fun hasOverlayPermission(): Boolean

    fun handlePackageAdded(packageName: String)

    fun handlePackageRemoved(packageName: String)
}
