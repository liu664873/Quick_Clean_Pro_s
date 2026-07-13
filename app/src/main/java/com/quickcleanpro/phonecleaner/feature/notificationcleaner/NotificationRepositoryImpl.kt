package com.quickcleanpro.phonecleaner.feature.notificationcleaner

import android.content.Context
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.BlockableNotificationApp
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationRepository

class NotificationRepositoryImpl(
    context: Context,
) : NotificationRepository {
    private val appContext = context.applicationContext

    override fun hasNotificationListenerAccess(): Boolean = NotificationDataSource.hasNotificationListenerAccess(appContext)

    override fun isNotificationBlockingEnabled(): Boolean = NotificationDataSource.isEnabled(appContext)

    override fun setNotificationBlockingEnabled(enabled: Boolean) {
        NotificationDataSource.setEnabled(appContext, enabled)
    }

    override fun blockedNotificationCount(): Int = NotificationDataSource.blockedCount(appContext)

    override fun blockedNotificationCountsByPackage(): Map<String, Int> = NotificationDataSource.blockedCountsByPackage(appContext)

    override fun selectedNotificationPackages(): Set<String> = NotificationDataSource.selectedPackages(appContext)

    override fun notificationApps(): List<BlockableNotificationApp> = NotificationDataSource.apps(appContext)

    override fun setNotificationPackageSelected(
        packageName: String,
        selected: Boolean,
    ) {
        NotificationDataSource.setPackageSelected(appContext, packageName, selected)
    }

}
