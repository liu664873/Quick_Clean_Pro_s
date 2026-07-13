package com.quickcleanpro.phonecleaner.app.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGatewayContractTest {
    @Test
    fun `permission keys are unique and reversible`() {
        val keys = PermissionType.entries.map(PermissionType::key)

        assertEquals(keys.size, keys.distinct().size)
        PermissionType.entries.forEach { permission ->
            assertEquals(permission, PermissionType.fromKey(permission.key))
        }
    }

    @Test
    fun `granted permission does not require a host launch`() {
        val request =
            resolvePermissionRequest(
                permission = PermissionType.Location,
                state = PermissionState.Granted,
                runtimePermissions = listOf("android.permission.ACCESS_FINE_LOCATION"),
                shouldRequestRuntime = true,
                hasSettings = true,
            )

        assertEquals(PermissionRequest.AlreadyGranted, request)
        assertTrue(PermissionState.Granted.isGranted)
    }

    @Test
    fun `runtime denial requests runtime permission while it can still be requested`() {
        val request =
            resolvePermissionRequest(
                permission = PermissionType.PostNotifications,
                state = PermissionState.Denied(),
                runtimePermissions = listOf("android.permission.POST_NOTIFICATIONS"),
                shouldRequestRuntime = true,
                hasSettings = true,
            )

        assertTrue(request is PermissionRequest.Runtime)
        val runtime = request as PermissionRequest.Runtime
        assertEquals(PermissionType.PostNotifications, runtime.permission)
        assertEquals(listOf("android.permission.POST_NOTIFICATIONS"), runtime.permissions)
    }

    @Test
    fun `permanent denial falls back to settings without exposing an Intent`() {
        val request =
            resolvePermissionRequest(
                permission = PermissionType.Location,
                state = PermissionState.Denied(permanently = true),
                runtimePermissions = listOf("android.permission.ACCESS_FINE_LOCATION"),
                shouldRequestRuntime = false,
                hasSettings = true,
            )

        assertTrue(request is PermissionRequest.Settings)
        val settings = request as PermissionRequest.Settings
        assertEquals(PermissionType.Location, settings.permission)
        assertFalse((PermissionState.Denied(permanently = true)).isGranted)
    }

    @Test
    fun `unavailable permission never launches runtime or settings`() {
        val request =
            resolvePermissionRequest(
                permission = PermissionType.Overlay,
                state = PermissionState.Unavailable,
                runtimePermissions = emptyList(),
                shouldRequestRuntime = false,
                hasSettings = true,
            )

        assertEquals(PermissionRequest.Unavailable, request)
    }
}
