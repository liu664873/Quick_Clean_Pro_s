package com.quickcleanpro.phonecleaner.common.platform.permission

import com.quickcleanpro.phonecleaner.common.platform.permission.core.PermissionType

import com.quickcleanpro.phonecleaner.common.permission.PermissionRequestResult
import com.quickcleanpro.phonecleaner.common.permission.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCoordinatorDecisionTest {
    @Test
    fun `runtime rejection or cancellation ends the current permission request`() {
        val decision =
            resolvePermissionRecheck(
                previousMissingPermission = PermissionType.Location,
                status =
                    PermissionStatus(
                        granted = false,
                        missing = listOf(PermissionType.Location),
                    ),
            )

        assertEquals(PermissionRecheckDecision.Denied, decision)
    }

    @Test
    fun `dialog cancellation reports dismissed`() {
        assertEquals(PermissionRequestResult.Dismissed, permissionDismissResult(notifyRejected = true))
    }

    @Test
    fun `settings return completes when permission is granted`() {
        val decision =
            resolvePermissionRecheck(
                previousMissingPermission = PermissionType.UsageAccess,
                status = PermissionStatus(granted = true, missing = emptyList()),
            )

        assertEquals(PermissionRecheckDecision.Granted, decision)
    }

    @Test
    fun `settings return continues with the next missing permission`() {
        val decision =
            resolvePermissionRecheck(
                previousMissingPermission = PermissionType.UsageAccess,
                status =
                    PermissionStatus(
                        granted = false,
                        missing = listOf(PermissionType.Overlay),
                    ),
            )

        assertTrue(decision is PermissionRecheckDecision.Continue)
        val next = decision as PermissionRecheckDecision.Continue
        assertEquals(PermissionType.Overlay, next.missingPermission)
    }
}
