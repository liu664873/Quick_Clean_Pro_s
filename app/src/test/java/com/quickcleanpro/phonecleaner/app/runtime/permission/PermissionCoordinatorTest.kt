package com.quickcleanpro.phonecleaner.app.runtime.permission

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.quickcleanpro.phonecleaner.common.permission.PermissionEngine
import com.quickcleanpro.phonecleaner.common.permission.PermissionHandler
import com.quickcleanpro.phonecleaner.common.permission.PermissionPromptMode
import com.quickcleanpro.phonecleaner.common.permission.PermissionTarget
import com.quickcleanpro.phonecleaner.common.permission.PermissionType
import com.quickcleanpro.phonecleaner.common.permission.ProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.RuntimePermissionDenialStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCoordinatorTest {
    private val context: Context = coordinatorContextWithoutAndroidRuntime()

    @Test
    fun `explained and direct modes produce different first UI steps`() {
        val handler = CoordinatorFakeHandler(
            PermissionType.Location,
            runtimePermissions = listOf("location"),
        )
        val explained = coordinator(handler)
        val direct = coordinator(handler)

        explained.ensure(ProtectedAction.NetworkScanStart) {}
        direct.ensure(
            ProtectedAction.NetworkScanStart,
            mode = PermissionPromptMode.Direct,
        ) {}

        assertTrue(explained.session?.showDialog == true)
        assertNull(explained.pendingLaunch)
        assertFalse(direct.session?.showDialog == true)
        assertTrue(direct.pendingLaunch is PermissionLaunch.Runtime)
    }

    @Test
    fun `active session ignores a second request`() {
        val coordinator = coordinator(CoordinatorFakeHandler(PermissionType.Location))
        var secondGranted = false

        coordinator.ensure(ProtectedAction.NetworkScanStart) {}
        coordinator.ensure(PermissionType.UsageAccess) { secondGranted = true }

        assertEquals(
            PermissionTarget.Action(ProtectedAction.NetworkScanStart),
            coordinator.session?.target,
        )
        assertFalse(secondGranted)
    }

    @Test
    fun `settings return waits for pause and continues app lock permissions in order`() {
        val usage = CoordinatorFakeHandler(
            PermissionType.UsageAccess,
            settingsIntents = listOf(coordinatorIntentWithoutAndroidRuntime()),
        )
        val overlay = CoordinatorFakeHandler(
            PermissionType.Overlay,
            settingsIntents = listOf(coordinatorIntentWithoutAndroidRuntime()),
        )
        val coordinator = coordinator(usage, overlay)
        var grantedCount = 0

        coordinator.ensure(ProtectedAction.AppLockEnableMonitoring) { grantedCount += 1 }
        coordinator.onDialogSubmit()
        usage.granted = true
        coordinator.onSettingsReturnIfReady()

        assertEquals(PermissionType.UsageAccess, coordinator.session?.missingPermission)
        assertEquals(0, grantedCount)

        coordinator.markSettingsLaunchObservedPause()
        coordinator.onSettingsReturnIfReady()
        assertEquals(PermissionType.Overlay, coordinator.session?.missingPermission)
        assertTrue(coordinator.session?.showDialog == true)

        coordinator.onDialogSubmit()
        overlay.granted = true
        coordinator.markSettingsLaunchObservedPause()
        coordinator.onSettingsReturnIfReady()

        assertEquals(1, grantedCount)
        assertNull(coordinator.session)
    }

    @Test
    fun `open settings callback runs on return but not when launch is unavailable`() {
        val available = coordinator(
            CoordinatorFakeHandler(
                PermissionType.Overlay,
                settingsIntents = listOf(coordinatorIntentWithoutAndroidRuntime()),
            ),
        )
        var returnCount = 0
        available.openSettings(PermissionType.Overlay) { returnCount += 1 }
        available.markSettingsLaunchObservedPause()
        available.onSettingsReturnIfReady()
        assertEquals(1, returnCount)

        val unavailable = coordinator(CoordinatorFakeHandler(PermissionType.Overlay))
        unavailable.openSettings(PermissionType.Overlay) { returnCount += 1 }
        assertEquals(1, returnCount)
    }

    private fun coordinator(vararg handlers: PermissionHandler): PermissionCoordinator =
        PermissionCoordinator(
            context,
            PermissionEngine(
                handlers = handlers.toList(),
                denialStore = CoordinatorDenialStore,
                isRuntimePermissionGranted = { _, _ -> false },
                settingsIntentKey = { System.identityHashCode(it).toString() },
            ),
        )
}

private fun coordinatorContextWithoutAndroidRuntime(): Context {
    val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
    return (field.get(null) as sun.misc.Unsafe).allocateInstance(ContextWrapper::class.java) as Context
}

private fun coordinatorIntentWithoutAndroidRuntime(): Intent {
    val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
    return (field.get(null) as sun.misc.Unsafe).allocateInstance(Intent::class.java) as Intent
}

private class CoordinatorFakeHandler(
    override val permission: PermissionType,
    var granted: Boolean = false,
    private val runtimePermissions: List<String> = emptyList(),
    private val settingsIntents: List<Intent> = emptyList(),
) : PermissionHandler {
    override fun isGranted(context: Context): Boolean = granted
    override fun runtimePermissions(context: Context): List<String> = runtimePermissions
    override fun settingsIntents(context: Context): List<Intent> = settingsIntents
}

private object CoordinatorDenialStore : RuntimePermissionDenialStore {
    override fun hasDenied(permission: PermissionType): Boolean = false
    override fun markDenied(permission: PermissionType) = Unit
}
