package com.quickcleanpro.phonecleaner.common.permission.runtime

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quickcleanpro.phonecleaner.common.permission.CleanXPermissionCoordinator
import com.quickcleanpro.phonecleaner.common.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.common.permission.PermissionManager
import com.quickcleanpro.phonecleaner.common.permission.PermissionRequestPlan
import com.quickcleanpro.phonecleaner.common.permission.PermissionRequestTarget
import com.quickcleanpro.phonecleaner.common.permission.PermissionStatus
import com.quickcleanpro.phonecleaner.common.permission.PermissionType

internal class CleanXPermissionCoordinatorState(
    private val context: Context,
    private val actionPermissionManager: PermissionManager<CleanXProtectedAction>,
    private val itemPermissionManager: PermissionManager<PermissionType>,
) : CleanXPermissionCoordinator {
    var session by mutableStateOf<PermissionSession?>(null)
        private set
    var pendingLaunch by mutableStateOf<PermissionLaunch?>(null)
        private set

    override fun isGranted(action: CleanXProtectedAction): Boolean =
        actionStatus(action).granted

    override fun isGranted(item: PermissionType): Boolean =
        itemStatus(item).granted

    override fun guard(
        action: CleanXProtectedAction,
        onRejected: () -> Unit,
        onGranted: () -> Unit,
    ) {
        startSession(
            target = PermissionRequestTarget.Action(action),
            status = actionStatus(action),
            onGranted = onGranted,
            onRejected = onRejected,
        )
    }

    override fun guardDirect(
        action: CleanXProtectedAction,
        onRejected: () -> Unit,
        onGranted: () -> Unit,
    ) {
        startDirectSession(
            target = PermissionRequestTarget.Action(action),
            status = actionStatus(action),
            onGranted = onGranted,
            onRejected = onRejected,
        )
    }

    override fun request(
        item: PermissionType,
        onRejected: () -> Unit,
        onGranted: () -> Unit,
    ) {
        startSession(
            target = PermissionRequestTarget.Item(item),
            status = itemStatus(item),
            onGranted = onGranted,
            onRejected = onRejected,
        )
    }

    override fun openSettings(
        item: PermissionType,
        onRejected: () -> Unit,
        onGranted: () -> Unit,
    ) {
        startSettingsSession(
            target = PermissionRequestTarget.Item(item),
            status = itemStatus(item),
            onGranted = onGranted,
            onRejected = onRejected,
        )
    }

    fun dismiss(notifyRejected: Boolean = true) {
        val current = session
        if (notifyRejected && current != null) {
            PermissionAnalytics.trackDismissed(current.target, current.showDialog)
        }
        val onRejected = if (notifyRejected) current?.onRejected else null
        session = null
        pendingLaunch = null
        onRejected?.invoke()
    }

    fun dismissUnavailable() {
        dismiss(notifyRejected = true)
    }

    fun onDialogSubmit() {
        val current = session ?: return
        PermissionAnalytics.trackDialogAccepted(current.target)
        launchPermissionPlan(current, showSettingsDialog = false)
    }

    fun consumePendingLaunch(): PermissionLaunch? =
        pendingLaunch.also { pendingLaunch = null }

    fun markSettingsLaunchPending(target: PermissionRequestTarget) {
        val current = session ?: return
        if (current.target.key == target.key) {
            session = current.copy(settingsLaunchPending = true, settingsLaunchObservedPause = false)
        }
    }

    fun markSettingsLaunchObservedPause() {
        val current = session ?: return
        if (current.settingsLaunchPending) {
            session = current.copy(settingsLaunchObservedPause = true)
        }
    }

    fun onRuntimeResult(
        target: PermissionRequestTarget,
        grants: Map<String, Boolean>,
    ) {
        when (target) {
            is PermissionRequestTarget.Action ->
                actionPermissionManager.onRuntimeResult(context, target.action, grants)
            is PermissionRequestTarget.Item ->
                itemPermissionManager.onRuntimeResult(context, target.item, grants)
        }
        recheckAfterPermissionReturn(session ?: return)
    }

    fun onSettingsReturnIfReady() {
        val current = session ?: return
        if (!current.settingsLaunchPending || !current.settingsLaunchObservedPause) return
        recheckAfterPermissionReturn(current)
    }

    private fun startSession(
        target: PermissionRequestTarget,
        status: PermissionStatus,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
    ) {
        if (session != null) return
        if (status.granted) {
            onGranted()
            return
        }
        session =
            PermissionSession(
                target = target,
                missingPermission = status.missing.firstOrNull(),
                onGranted = onGranted,
                onRejected = onRejected,
            )
    }

    private fun startDirectSession(
        target: PermissionRequestTarget,
        status: PermissionStatus,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
    ) {
        if (session != null) return
        if (status.granted) {
            onGranted()
            return
        }
        val current =
            PermissionSession(
                target = target,
                missingPermission = status.missing.firstOrNull(),
                onGranted = onGranted,
                onRejected = onRejected,
                showDialog = false,
            )
        session = current
        launchPermissionPlan(current, showSettingsDialog = true)
    }

    private fun startSettingsSession(
        target: PermissionRequestTarget,
        status: PermissionStatus,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
    ) {
        if (session != null) return
        val current =
            PermissionSession(
                target = target,
                missingPermission = status.missing.firstOrNull(),
                onGranted = onGranted,
                onRejected = onRejected,
                showDialog = false,
            )
        session = current
        when (val plan = settingsPlan(target)) {
            PermissionRequestPlan.AlreadyGranted -> finishIfGranted()
            is PermissionRequestPlan.OpenSettings -> {
                pendingLaunch = PermissionLaunch.Settings(current.target, plan.permission)
                session = current.copy(settingsLaunchPending = true)
            }
            PermissionRequestPlan.Unavailable,
            is PermissionRequestPlan.RequestRuntime,
            -> dismissUnavailable()
        }
    }

    private fun launchPermissionPlan(
        current: PermissionSession,
        showSettingsDialog: Boolean,
    ) {
        when (val plan = requestPlan(current.target)) {
            PermissionRequestPlan.AlreadyGranted -> finishIfGranted()
            is PermissionRequestPlan.RequestRuntime -> {
                pendingLaunch = PermissionLaunch.Runtime(current.target, plan.permissions)
                session = current.copy(showDialog = false)
            }
            is PermissionRequestPlan.OpenSettings -> {
                if (showSettingsDialog) {
                    session = current.copy(showDialog = true)
                } else {
                    pendingLaunch = PermissionLaunch.Settings(current.target, plan.permission)
                    session = current.copy(showDialog = false, settingsLaunchPending = true)
                }
            }
            PermissionRequestPlan.Unavailable -> dismissUnavailable()
        }
    }

    private fun recheckAfterPermissionReturn(previous: PermissionSession) {
        when (val decision = resolvePermissionRecheck(previous.missingPermission, status(previous.target))) {
            PermissionRecheckDecision.Granted -> {
                PermissionAnalytics.trackGranted(previous.target)
                val onGranted = previous.onGranted
                dismiss(notifyRejected = false)
                onGranted()
            }
            is PermissionRecheckDecision.Continue -> {
                session =
                    previous.copy(
                        missingPermission = decision.missingPermission,
                        showDialog = true,
                        settingsLaunchPending = false,
                        settingsLaunchObservedPause = false,
                    )
            }
            PermissionRecheckDecision.Denied -> dismiss(notifyRejected = true)
        }
    }

    private fun finishIfGranted() {
        val current = session ?: return
        val status = status(current.target)
        if (status.granted) {
            PermissionAnalytics.trackGranted(current.target)
            val onGranted = current.onGranted
            dismiss(notifyRejected = false)
            onGranted()
        } else {
            session = current.copy(missingPermission = status.missing.firstOrNull(), showDialog = true)
        }
    }

    private fun requestPlan(target: PermissionRequestTarget): PermissionRequestPlan =
        when (target) {
            is PermissionRequestTarget.Action ->
                runCatching { actionPermissionManager.requestPlan(context, target.action) }
                    .getOrDefault(PermissionRequestPlan.Unavailable)
            is PermissionRequestTarget.Item ->
                runCatching { itemPermissionManager.requestPlan(context, target.item) }
                    .getOrDefault(PermissionRequestPlan.Unavailable)
        }

    private fun settingsPlan(target: PermissionRequestTarget): PermissionRequestPlan =
        when (target) {
            is PermissionRequestTarget.Action ->
                runCatching { actionPermissionManager.settingsPlan(context, target.action) }
                    .getOrDefault(PermissionRequestPlan.Unavailable)
            is PermissionRequestTarget.Item ->
                runCatching { itemPermissionManager.settingsPlan(context, target.item) }
                    .getOrDefault(PermissionRequestPlan.Unavailable)
        }

    private fun status(target: PermissionRequestTarget): PermissionStatus =
        when (target) {
            is PermissionRequestTarget.Action -> actionStatus(target.action)
            is PermissionRequestTarget.Item -> itemStatus(target.item)
        }

    private fun actionStatus(action: CleanXProtectedAction): PermissionStatus =
        runCatching { actionPermissionManager.status(context, action) }
            .getOrDefault(PermissionStatus(granted = false, missing = emptyList()))

    private fun itemStatus(item: PermissionType): PermissionStatus =
        runCatching { itemPermissionManager.status(context, item) }
            .getOrDefault(PermissionStatus(granted = false, missing = emptyList()))
}
