package com.quickcleanpro.phonecleaner.use.app.runtime.notification

import android.content.Context
import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockMonitoringService

internal class PersistentAppLockMonitoringService(context: Context) : AppLockMonitoringService {
    private val appContext = context.applicationContext

    override fun enable() = PersistentNotificationService.enableMonitoring(appContext)

    override fun disable() = PersistentNotificationService.disableMonitoring(appContext)
}
