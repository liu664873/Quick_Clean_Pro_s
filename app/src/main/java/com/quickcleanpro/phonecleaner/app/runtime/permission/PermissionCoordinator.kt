package com.quickcleanpro.phonecleaner.app.runtime.permission

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quickcleanpro.phonecleaner.common.permission.AppPermissionCoordinator
import com.quickcleanpro.phonecleaner.common.permission.PermissionDecision
import com.quickcleanpro.phonecleaner.common.permission.PermissionEngine
import com.quickcleanpro.phonecleaner.common.permission.PermissionPromptMode
import com.quickcleanpro.phonecleaner.common.permission.PermissionStatus
import com.quickcleanpro.phonecleaner.common.permission.PermissionTarget
import com.quickcleanpro.phonecleaner.common.permission.PermissionType
import com.quickcleanpro.phonecleaner.common.permission.ProtectedAction

internal class PermissionCoordinator(
    private val context: Context,
    private val engine: PermissionEngine,
) : AppPermissionCoordinator {
    var session by mutableStateOf<PermissionSession?>(null)
        private set
    var pendingLaunch by mutableStateOf<PermissionLaunch?>(null)
        private set

    override fun isGranted(permission: PermissionType): Boolean =
        status(PermissionTarget.Permission(permission)).granted

    override fun ensure(
        action: ProtectedAction,
        mode: PermissionPromptMode,
        onDenied: () -> Unit,
        onGranted: () -> Unit,
    ) = startSession(PermissionTarget.Action(action), mode, onDenied, onGranted)

    override fun ensure(
        permission: PermissionType,
        mode: PermissionPromptMode,
        onDenied: () -> Unit,
        onGranted: () -> Unit,
    ) = startSession(PermissionTarget.Permission(permission), mode, onDenied, onGranted)

    override fun openSettings(
        permission: PermissionType,
        onReturn: () -> Unit,
    ) {
        if (session != null) return
        val target = PermissionTarget.Permission(permission)
        val current = PermissionSession(
            target = target,
            missingPermission = permission,
            onGranted = onReturn,
            onDenied = onReturn,
            showDialog = false,
        )
        session = current
        when (val decision = engine.settingsDecision(context, permission)) {
            is PermissionDecision.OpenSettings -> queueSettings(current, decision)
            else -> dismiss(notifyDenied = false)
        }
    }

    fun dismiss(notifyDenied: Boolean = true) {
        val current = session
        if (notifyDenied && current != null) {
            PermissionAnalytics.trackDismissed(current.target, current.showDialog)
        }
        val onDenied = if (notifyDenied) current?.onDenied else null
        session = null
        pendingLaunch = null
        onDenied?.invoke()
    }

    fun dismissUnavailable() = dismiss(notifyDenied = true)

    fun onDialogSubmit() {
        val current = session ?: return
        PermissionAnalytics.trackDialogAccepted(current.target)
        launchDecision(current, showSettingsDialog = false)
    }

    fun consumePendingLaunch(): PermissionLaunch? = pendingLaunch.also { pendingLaunch = null }

    fun markSettingsLaunchObservedPause() {
        val current = session ?: return
        if (current.settingsLaunchPending) {
            session = current.copy(settingsLaunchObservedPause = true)
        }
    }

    fun onRuntimeResult(grants: Map<String, Boolean>) {
        engine.onRuntimeResult(grants)
        recheckAfterPermissionReturn(session ?: return)
    }

    fun onSettingsReturnIfReady() {
        val current = session ?: return
        if (!current.settingsLaunchPending || !current.settingsLaunchObservedPause) return
        recheckAfterPermissionReturn(current)
    }

    private fun startSession(
        target: PermissionTarget,
        mode: PermissionPromptMode,
        onDenied: () -> Unit,
        onGranted: () -> Unit,
    ) {
        if (session != null) return
        val status = status(target)
        if (status.granted) {
            onGranted()
            return
        }
        val current = PermissionSession(
            target = target,
            missingPermission = status.missing.firstOrNull(),
            onGranted = onGranted,
            onDenied = onDenied,
            showDialog = mode == PermissionPromptMode.Explained,
        )
        session = current
        if (mode == PermissionPromptMode.Direct) {
            launchDecision(current, showSettingsDialog = true)
        }
    }

    private fun launchDecision(
        current: PermissionSession,
        showSettingsDialog: Boolean,
    ) {
        when (val decision = engine.decide(context, current.target.requiredPermissions)) {
            PermissionDecision.Granted -> finishIfGranted()
            is PermissionDecision.RequestRuntime -> {
                pendingLaunch = PermissionLaunch.Runtime(current.target, decision.permissions)
                session = current.copy(showDialog = false)
            }
            is PermissionDecision.OpenSettings -> {
                if (showSettingsDialog) {
                    session = current.copy(showDialog = true)
                } else {
                    queueSettings(current, decision)
                }
            }
            PermissionDecision.Unavailable -> dismissUnavailable()
        }
    }

    private fun queueSettings(
        current: PermissionSession,
        decision: PermissionDecision.OpenSettings,
    ) {
        pendingLaunch = PermissionLaunch.Settings(current.target, decision.intents)
        session = current.copy(
            showDialog = false,
            settingsLaunchPending = true,
            settingsLaunchObservedPause = false,
        )
    }

    private fun recheckAfterPermissionReturn(previous: PermissionSession) {
        when (val decision = resolvePermissionRecheck(previous.missingPermission, status(previous.target))) {
            PermissionRecheckDecision.Granted -> {
                PermissionAnalytics.trackGranted(previous.target)
                val onGranted = previous.onGranted
                dismiss(notifyDenied = false)
                onGranted()
            }
            is PermissionRecheckDecision.Continue -> {
                session = previous.copy(
                    missingPermission = decision.missingPermission,
                    showDialog = true,
                    settingsLaunchPending = false,
                    settingsLaunchObservedPause = false,
                )
            }
            PermissionRecheckDecision.Denied -> dismiss(notifyDenied = true)
        }
    }

    private fun finishIfGranted() {
        val current = session ?: return
        val status = status(current.target)
        if (!status.granted) {
            session = current.copy(missingPermission = status.missing.firstOrNull(), showDialog = true)
            return
        }
        PermissionAnalytics.trackGranted(current.target)
        val onGranted = current.onGranted
        dismiss(notifyDenied = false)
        onGranted()
    }

    private fun status(target: PermissionTarget): PermissionStatus =
        runCatching { engine.status(context, target.requiredPermissions) }
            .getOrDefault(PermissionStatus(granted = false, missing = target.requiredPermissions))
}
