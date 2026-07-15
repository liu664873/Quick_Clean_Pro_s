package com.quickcleanpro.phonecleaner.common.permission

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionEngineTest {
    private val context: Context = contextWithoutAndroidRuntime()

    @Test
    fun `granted permission produces granted decision`() {
        val handler = FakeHandler(PermissionType.Location, granted = true)
        val engine = engine(handler)

        assertEquals(PermissionDecision.Granted, engine.decide(context, listOf(PermissionType.Location)))
    }

    @Test
    fun `missing runtime permission is requested before settings`() {
        val handler = FakeHandler(PermissionType.Location, runtimePermissions = listOf("location"))
        val store = FakeDenialStore()
        val engine = engine(handler, store)

        val decision = engine.decide(context, listOf(PermissionType.Location))

        assertTrue(decision is PermissionDecision.RequestRuntime)
        assertEquals(listOf("location"), (decision as PermissionDecision.RequestRuntime).permissions.toList())
        assertEquals(listOf(PermissionType.Location), store.requested)
    }

    @Test
    fun `previously denied runtime permission opens settings`() {
        val handler = FakeHandler(
            PermissionType.Location,
            runtimePermissions = listOf("location"),
            settingsIntents = listOf(intentWithoutAndroidRuntime()),
        )
        val store = FakeDenialStore(denied = true)
        val engine = engine(handler, store)

        assertTrue(engine.decide(context, listOf(PermissionType.Location)) is PermissionDecision.OpenSettings)
    }

    @Test
    fun `missing handler and handler failure are unavailable`() {
        val missingEngine = engine()
        val failingEngine = engine(FakeHandler(PermissionType.Location, fail = true))

        assertEquals(
            PermissionDecision.Unavailable,
            missingEngine.decide(context, listOf(PermissionType.Location)),
        )
        assertEquals(
            PermissionDecision.Unavailable,
            failingEngine.decide(context, listOf(PermissionType.Location)),
        )
    }

    @Test
    fun `runtime denial is recorded once`() {
        val store = FakeDenialStore()
        val engine = engine(
            FakeHandler(PermissionType.Location, runtimePermissions = listOf("location")),
            store,
        )
        engine.decide(context, listOf(PermissionType.Location))

        engine.onRuntimeResult(mapOf("location" to false))

        assertEquals(listOf(PermissionType.Location), store.deniedPermissions)
    }

    @Test
    fun `protected action keys are unique and app lock order is stable`() {
        assertEquals(ProtectedAction.entries.size, ProtectedAction.entries.map { it.key }.distinct().size)
        assertEquals(
            listOf(PermissionType.UsageAccess, PermissionType.Overlay),
            ProtectedAction.AppLockEnableMonitoring.requiredPermissions,
        )
    }

    private fun engine(
        vararg handlers: PermissionHandler,
        store: FakeDenialStore = FakeDenialStore(),
    ): PermissionEngine = engine(handlers.toList(), store)

    private fun engine(
        handler: PermissionHandler,
        store: FakeDenialStore,
    ): PermissionEngine = engine(listOf(handler), store)

    private fun engine(
        handlers: List<PermissionHandler>,
        store: FakeDenialStore,
    ): PermissionEngine =
        PermissionEngine(
            handlers = handlers,
            denialStore = store,
            isRuntimePermissionGranted = { _, _ -> false },
            settingsIntentKey = { System.identityHashCode(it).toString() },
        )
}

private fun contextWithoutAndroidRuntime(): Context {
    val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
    return (field.get(null) as sun.misc.Unsafe).allocateInstance(ContextWrapper::class.java) as Context
}

private fun intentWithoutAndroidRuntime(): Intent {
    val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
    return (field.get(null) as sun.misc.Unsafe).allocateInstance(Intent::class.java) as Intent
}

private class FakeHandler(
    override val permission: PermissionType,
    var granted: Boolean = false,
    private val runtimePermissions: List<String> = emptyList(),
    private val settingsIntents: List<Intent> = emptyList(),
    private val fail: Boolean = false,
) : PermissionHandler {
    override fun isGranted(context: Context): Boolean = if (fail) error("failed") else granted
    override fun runtimePermissions(context: Context): List<String> = if (fail) error("failed") else runtimePermissions
    override fun settingsIntents(context: Context): List<Intent> = if (fail) error("failed") else settingsIntents
}

private class FakeDenialStore(
    private val denied: Boolean = false,
) : RuntimePermissionDenialStore {
    val requested = mutableListOf<PermissionType>()
    val deniedPermissions = mutableListOf<PermissionType>()

    override fun hasDenied(permission: PermissionType): Boolean = denied
    override fun markDenied(permission: PermissionType) {
        deniedPermissions += permission
    }
    override fun markRequested(permission: PermissionType) {
        requested += permission
    }
}
