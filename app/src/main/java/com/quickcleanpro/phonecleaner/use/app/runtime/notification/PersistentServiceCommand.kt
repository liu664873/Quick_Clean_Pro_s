package com.quickcleanpro.phonecleaner.use.app.runtime.notification

import com.quickcleanpro.phonecleaner.use.feature.applock.data.service.AppLockBroadcastActions
import com.quickcleanpro.phonecleaner.use.feature.applock.data.service.AppLockServiceActions

internal object PersistentServiceActions {
    const val START = "com.quickcleanpro.phonecleaner.notification.START"
    const val ENABLE_MONITORING = "com.quickcleanpro.phonecleaner.applock.ENABLE_MONITORING"
    const val DISABLE_MONITORING = "com.quickcleanpro.phonecleaner.applock.DISABLE_MONITORING"
    const val APP_FOREGROUND = "com.quickcleanpro.phonecleaner.notification.APP_FOREGROUND"
    const val APP_BACKGROUND = "com.quickcleanpro.phonecleaner.notification.APP_BACKGROUND"
    const val RESTORE_NOTIFICATION = "com.quickcleanpro.phonecleaner.notification.RESTORE_PERSISTENT"
    const val STOP_SERVICE = "com.quickcleanpro.phonecleaner.notification.STOP_SERVICE"
    const val PASSWORD_SUCCESS = AppLockBroadcastActions.PASSWORD_SUCCESS
    const val LOCK_SCREEN_CANCELLED = AppLockBroadcastActions.LOCK_SCREEN_CANCELLED
}

internal enum class PersistentServiceCommand {
    Start,
    EnableMonitoring,
    DisableMonitoring,
    AppForeground,
    AppBackground,
    RestoreNotification,
    StopService,
    DismissLockScreen,
}

internal fun persistentServiceCommand(action: String?): PersistentServiceCommand? =
    when (action) {
        PersistentServiceActions.START -> PersistentServiceCommand.Start
        PersistentServiceActions.ENABLE_MONITORING -> PersistentServiceCommand.EnableMonitoring
        PersistentServiceActions.DISABLE_MONITORING -> PersistentServiceCommand.DisableMonitoring
        PersistentServiceActions.APP_FOREGROUND -> PersistentServiceCommand.AppForeground
        PersistentServiceActions.APP_BACKGROUND -> PersistentServiceCommand.AppBackground
        PersistentServiceActions.RESTORE_NOTIFICATION -> PersistentServiceCommand.RestoreNotification
        PersistentServiceActions.STOP_SERVICE -> PersistentServiceCommand.StopService
        PersistentServiceActions.PASSWORD_SUCCESS,
        PersistentServiceActions.LOCK_SCREEN_CANCELLED,
        -> PersistentServiceCommand.DismissLockScreen
        else -> null
    }

internal interface PersistentNotificationActions {
    fun scheduleRestore()
}

internal class PersistentServiceCommandDispatcher(
    private val appLock: AppLockServiceActions,
    private val notification: PersistentNotificationActions,
    private val setAppInForeground: (Boolean) -> Unit,
    private val stopService: () -> Unit,
) {
    fun dispatch(command: PersistentServiceCommand) {
        when (command) {
            PersistentServiceCommand.Start -> appLock.syncMonitoringState()
            PersistentServiceCommand.EnableMonitoring -> appLock.enableMonitoring()
            PersistentServiceCommand.DisableMonitoring -> appLock.disableMonitoring()
            PersistentServiceCommand.AppForeground -> setAppInForeground(true)
            PersistentServiceCommand.AppBackground -> setAppInForeground(false)
            PersistentServiceCommand.RestoreNotification -> notification.scheduleRestore()
            PersistentServiceCommand.StopService -> stopService()
            PersistentServiceCommand.DismissLockScreen -> appLock.dismissLockScreen()
        }
    }
}
