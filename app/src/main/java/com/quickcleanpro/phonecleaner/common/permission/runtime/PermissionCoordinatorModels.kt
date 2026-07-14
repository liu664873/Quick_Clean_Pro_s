package com.quickcleanpro.phonecleaner.common.permission.runtime

import com.quickcleanpro.phonecleaner.common.permission.PermissionRequestTarget
import com.quickcleanpro.phonecleaner.common.permission.PermissionStatus
import com.quickcleanpro.phonecleaner.common.permission.PermissionType

internal data class PermissionSession(
    val target: PermissionRequestTarget,
    val missingPermission: PermissionType?,
    val onGranted: () -> Unit,
    val onRejected: () -> Unit,
    val showDialog: Boolean = true,
    val settingsLaunchPending: Boolean = false,
    val settingsLaunchObservedPause: Boolean = false,
)

internal sealed interface PermissionLaunch {
    val target: PermissionRequestTarget

    data class Runtime(
        override val target: PermissionRequestTarget,
        val permissions: Array<String>,
    ) : PermissionLaunch

    data class Settings(
        override val target: PermissionRequestTarget,
        val permission: PermissionType,
    ) : PermissionLaunch
}

internal fun shouldContinuePermissionFlow(
    previousMissingPermission: PermissionType?,
    nextMissingPermission: PermissionType?,
): Boolean =
    previousMissingPermission?.key != null &&
        nextMissingPermission?.key != null &&
        previousMissingPermission.key != nextMissingPermission.key

internal sealed interface PermissionRecheckDecision {
    data object Granted : PermissionRecheckDecision

    data class Continue(
        val missingPermission: PermissionType,
    ) : PermissionRecheckDecision

    data object Denied : PermissionRecheckDecision
}

internal fun resolvePermissionRecheck(
    previousMissingPermission: PermissionType?,
    status: PermissionStatus,
): PermissionRecheckDecision {
    if (status.granted) return PermissionRecheckDecision.Granted
    val nextMissingPermission = status.missing.firstOrNull()
    return if (shouldContinuePermissionFlow(previousMissingPermission, nextMissingPermission)) {
        PermissionRecheckDecision.Continue(requireNotNull(nextMissingPermission))
    } else {
        PermissionRecheckDecision.Denied
    }
}
