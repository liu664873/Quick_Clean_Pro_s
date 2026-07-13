package com.quickcleanpro.phonecleaner.use.feature.applock.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.quickcleanpro.phonecleaner.use.feature.applock.data.AppLockMonitorHandler
import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockRepository
import kotlinx.coroutines.CoroutineScope

internal interface AppLockServiceActions {
    fun syncMonitoringState()
    fun enableMonitoring()
    fun disableMonitoring()
    fun dismissLockScreen()
}

internal object AppLockBroadcastActions {
    const val PASSWORD_SUCCESS = "com.quickcleanpro.phonecleaner.applock.PASSWORD_SUCCESS"
    const val LOCK_SCREEN_CANCELLED = "com.quickcleanpro.phonecleaner.applock.LOCK_SCREEN_CANCELLED"
}

internal class AppLockServiceCoordinator(
    private val context: Context,
    repository: AppLockRepository,
    scope: CoroutineScope,
) : AppLockServiceActions {
    private val monitor = AppLockMonitorHandler(context, repository, scope)
    private val packageEvents = AppLockPackageEventHandler(repository, monitor::syncMonitoringState)
    private var packageEventReceiver: BroadcastReceiver? = null

    override fun syncMonitoringState() = monitor.syncMonitoringState()

    override fun enableMonitoring() {
        monitor.enableMonitoring()
    }

    override fun disableMonitoring() = monitor.disableMonitoring()

    override fun dismissLockScreen() = monitor.dismissLockScreen()

    fun registerPackageEvents() {
        if (packageEventReceiver != null) return
        packageEventReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context?, intent: Intent?) {
                    packageEvents.handle(
                        action = intent?.action,
                        packageName = intent?.data?.schemeSpecificPart,
                        replacing = intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true,
                    )
                }
            }.also { receiver ->
                val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_PACKAGE_ADDED)
                        addAction(Intent.ACTION_PACKAGE_REMOVED)
                        addDataScheme("package")
                    }
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            }
    }

    fun destroy() {
        disableMonitoring()
        packageEventReceiver?.let { receiver -> runCatching { context.unregisterReceiver(receiver) } }
        packageEventReceiver = null
    }
}

internal class AppLockPackageEventHandler(
    private val repository: AppLockRepository,
    private val onPackagesChanged: () -> Unit,
) {
    fun handle(
        action: String?,
        packageName: String?,
        replacing: Boolean,
    ) {
        if (replacing || packageName.isNullOrBlank()) return
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> runCatching { repository.handlePackageAdded(packageName) }
            Intent.ACTION_PACKAGE_REMOVED -> runCatching { repository.handlePackageRemoved(packageName) }
            else -> return
        }
        onPackagesChanged()
    }
}
