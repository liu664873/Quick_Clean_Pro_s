package com.quickcleanpro.phonecleaner.app.runtime.notification

import android.content.Context
import com.quickcleanpro.phonecleaner.feature.applock.AppLockMonitoringService

internal class BackgroundAppLockMonitoringService(context: Context) : AppLockMonitoringService {
    private val appContext = context.applicationContext

    override fun enable() = AppBackgroundRuntimeService.enableMonitoring(appContext)

    override fun disable() = AppBackgroundRuntimeService.disableMonitoring(appContext)
}
