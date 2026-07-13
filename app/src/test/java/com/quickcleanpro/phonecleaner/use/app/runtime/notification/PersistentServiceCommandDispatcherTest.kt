package com.quickcleanpro.phonecleaner.use.app.runtime.notification

import com.quickcleanpro.phonecleaner.use.core.model.device.BatteryHistorySample
import com.quickcleanpro.phonecleaner.use.feature.toolbox.battery.data.service.BatterySamplingCoordinator
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistoryOwner
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistorySampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersistentServiceCommandDispatcherTest {
    @Test
    fun `all external actions keep their command mapping`() {
        val mappings =
            mapOf(
                PersistentServiceActions.START to PersistentServiceCommand.Start,
                PersistentServiceActions.ENABLE_MONITORING to PersistentServiceCommand.EnableMonitoring,
                PersistentServiceActions.DISABLE_MONITORING to PersistentServiceCommand.DisableMonitoring,
                PersistentServiceActions.APP_FOREGROUND to PersistentServiceCommand.AppForeground,
                PersistentServiceActions.APP_BACKGROUND to PersistentServiceCommand.AppBackground,
                PersistentServiceActions.RESTORE_NOTIFICATION to PersistentServiceCommand.RestoreNotification,
                PersistentServiceActions.STOP_SERVICE to PersistentServiceCommand.StopService,
                PersistentServiceActions.PASSWORD_SUCCESS to PersistentServiceCommand.DismissLockScreen,
                PersistentServiceActions.LOCK_SCREEN_CANCELLED to PersistentServiceCommand.DismissLockScreen,
            )

        mappings.forEach { (action, expected) ->
            assertEquals(expected, persistentServiceCommand(action))
        }
        assertNull(persistentServiceCommand(null))
        assertNull(persistentServiceCommand("unknown"))
    }

    @Test
    fun `dispatcher delegates every command once`() {
        val appLock = RecordingAppLockActions()
        val notification = RecordingNotificationActions()
        val foregroundValues = mutableListOf<Boolean>()
        var stopCalls = 0
        val dispatcher =
            PersistentServiceCommandDispatcher(
                appLock = appLock,
                notification = notification,
                setAppInForeground = foregroundValues::add,
                stopService = { stopCalls += 1 },
            )

        PersistentServiceCommand.entries.forEach(dispatcher::dispatch)

        assertEquals(
            listOf("sync", "enable", "disable", "dismiss"),
            appLock.calls,
        )
        assertEquals(1, notification.restoreCalls)
        assertEquals(listOf(true, false), foregroundValues)
        assertEquals(1, stopCalls)
    }

    @Test
    fun `battery coordinator uses only the service owner`() {
        val sampler = RecordingBatterySampler()
        val coordinator = BatterySamplingCoordinator(sampler)

        coordinator.start()
        coordinator.stop()

        assertEquals(listOf(BatteryHistoryOwner.Service), sampler.started)
        assertEquals(listOf(BatteryHistoryOwner.Service), sampler.stopped)
    }
}

private class RecordingAppLockActions : AppLockServiceActions {
    val calls = mutableListOf<String>()

    override fun syncMonitoringState() {
        calls += "sync"
    }

    override fun enableMonitoring() {
        calls += "enable"
    }

    override fun disableMonitoring() {
        calls += "disable"
    }

    override fun dismissLockScreen() {
        calls += "dismiss"
    }
}

private class RecordingNotificationActions : PersistentNotificationActions {
    var restoreCalls = 0

    override fun scheduleRestore() {
        restoreCalls += 1
    }
}

private class RecordingBatterySampler : BatteryHistorySampler {
    val started = mutableListOf<BatteryHistoryOwner>()
    val stopped = mutableListOf<BatteryHistoryOwner>()

    override fun start(owner: BatteryHistoryOwner) {
        started += owner
    }

    override fun stop(owner: BatteryHistoryOwner) {
        stopped += owner
    }

    override fun sampleOnce(force: Boolean): BatteryHistorySample? = null
}
