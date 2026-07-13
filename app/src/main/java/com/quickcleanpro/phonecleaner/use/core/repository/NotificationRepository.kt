package com.quickcleanpro.phonecleaner.use.core.repository

import com.quickcleanpro.phonecleaner.use.core.model.notification.BlockableNotificationApp

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
