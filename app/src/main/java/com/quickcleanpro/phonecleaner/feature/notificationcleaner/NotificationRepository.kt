package com.quickcleanpro.phonecleaner.feature.notificationcleaner

import com.quickcleanpro.phonecleaner.feature.notificationcleaner.BlockableNotificationApp

interface NotificationRepository {
    fun hasNotificationListenerAccess(): Boolean

    fun isNotificationBlockingEnabled(): Boolean

    fun setNotificationBlockingEnabled(enabled: Boolean)

    fun blockedNotificationCount(): Int

    fun blockedNotificationCountsByPackage(): Map<String, Int>

    fun selectedNotificationPackages(): Set<String>

    fun notificationApps(): List<BlockableNotificationApp>

    fun setNotificationPackageSelected(
        packageName: String,
        selected: Boolean,
    )

}
