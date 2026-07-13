package com.quickcleanpro.phonecleaner.use.app.runtime.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.brand.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class PersistentNotificationController(
    private val service: Service,
    private val scope: CoroutineScope,
    private val shouldStop: () -> Boolean,
) : PersistentNotificationActions {
    private var watchdogJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    fun initialize() {
        NotificationChannelManager.createAllChannels(service)
        showForeground()
        acquireWakeLock()
        startWatchdog()
    }

    fun ensureForeground() {
        showForeground()
        startWatchdog()
    }

    override fun scheduleRestore() {
        if (shouldStop()) return
        scope.launch {
            delay(PERSISTENT_NOTIFICATION_RESTORE_DELAY_MS)
            if (!shouldStop() && AppConfig.hasPostNotificationsPermission(service)) {
                runCatching { showForeground() }
            }
        }
    }

    fun stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            service.stopForeground(true)
        }
    }

    fun destroy() {
        watchdogJob?.cancel()
        watchdogJob = null
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun showForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(
                PersistentNotificationService.PERSISTENT_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            service.startForeground(PersistentNotificationService.PERSISTENT_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(service, NotificationChannelManager.PERSISTENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_n_notification_cleaner)
            .setContentTitle(service.getString(R.string.app_name))
            .setContentText(service.getString(R.string.running_in_background))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDeleteIntent(deletedIntent())
            .build()

    private fun deletedIntent(): PendingIntent {
        val intent = Intent(PersistentServiceActions.RESTORE_NOTIFICATION).setPackage(service.packageName)
        return PendingIntent.getBroadcast(
            service,
            PERSISTENT_NOTIFICATION_DELETE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun startWatchdog() {
        if (watchdogJob?.isActive == true) return
        watchdogJob =
            scope.launch {
                while (isActive) {
                    delay(PERSISTENT_NOTIFICATION_CHECK_INTERVAL_MS)
                    if (shouldStop()) break
                    if (AppConfig.hasPostNotificationsPermission(service) && !isNotificationActive()) {
                        runCatching { showForeground() }
                    }
                }
            }
    }

    private fun isNotificationActive(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return runCatching {
            val manager = service.getSystemService(NotificationManager::class.java)
            manager.activeNotifications.any {
                it.id == PersistentNotificationService.PERSISTENT_NOTIFICATION_ID &&
                    it.packageName == service.packageName
            }
        }.getOrDefault(true)
    }

    private fun acquireWakeLock() {
        val powerManager =
            runCatching { service.getSystemService(Context.POWER_SERVICE) as PowerManager }
                .getOrNull()
                ?: return
        wakeLock =
            runCatching {
                powerManager
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${service.javaClass.name}:Persistent")
                    .apply { runCatching { acquire(WAKE_LOCK_TIMEOUT_MS) } }
            }.getOrNull()
    }

    private companion object {
        const val PERSISTENT_NOTIFICATION_DELETE_REQUEST_CODE = 17
        const val WAKE_LOCK_TIMEOUT_MS = 10L * 60L * 1000L
        const val PERSISTENT_NOTIFICATION_RESTORE_DELAY_MS = 1_000L
        const val PERSISTENT_NOTIFICATION_CHECK_INTERVAL_MS = 60_000L
    }
}
