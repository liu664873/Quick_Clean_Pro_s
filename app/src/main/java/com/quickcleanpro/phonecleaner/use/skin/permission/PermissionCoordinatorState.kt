package com.quickcleanpro.phonecleaner.use.skin.permission

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionCoordinator
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionManager
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionRequestPlan
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionRequestResult
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionRequestTarget
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionStatus

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
        onGranted: () -> Unit,
        onRejected: () -> Unit,
        onResult: (PermissionRequestResult) -> Unit,
    ): PermissionRequestResult =
        startSession(
            target = PermissionRequestTarget.Action(action),
            status = actionStatus(action),
            onGranted = onGranted,
            onRejected = onRejected,
            onResult = onResult,
        )

    override fun guardDirect(
        action: CleanXProtectedAction,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
        onResult: (PermissionRequestResult) -> Unit,
    ): PermissionRequestResult =
        startDirectSession(
            target = PermissionRequestTarget.Action(action),
            status = actionStatus(action),
            onGranted = onGranted,
            onRejected = onRejected,
            onResult = onResult,
        )

    override fun request(
        item: PermissionType,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
        onResult: (PermissionRequestResult) -> Unit,
    ): PermissionRequestResult =
        startSession(
            target = PermissionRequestTarget.Item(item),
            status = itemStatus(item),
            onGranted = onGranted,
            onRejected = onRejected,
            onResult = onResult,
        )

    override fun openSettings(
        item: PermissionType,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
        onResult: (PermissionRequestResult) -> Unit,
    ): PermissionRequestResult =
        startSettingsSession(
            target = PermissionRequestTarget.Item(item),
            status = itemStatus(item),
            onGranted = onGranted,
            onRejected = onRejected,
            onResult = onResult,
        )

    fun dismiss(notifyRejected: Boolean = true) {
        dismiss(
            notifyRejected = notifyRejected,
            result = permissionDismissResult(notifyRejected),
        )
    }

    fun dismissUnavailable() {
        dismiss(
            notifyRejected = true,
            result = PermissionRequestResult.SettingsUnavailable,
        )
    }

    private fun dismiss(
        notifyRejected: Boolean,
        result: PermissionRequestResult,
    ) {
        val current = session
        if (notifyRejected && current != null) {
            PermissionAnalytics.trackDismissed(current.target, current.showDialog)
        }
        val onRejected = if (notifyRejected) current?.onRejected else null
        val onResult = current?.onResult
        session = null
        pendingLaunch = null
        onRejected?.invoke()
        onResult?.invoke(result)
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
        recheckAfterRuntimeRequest()
    }

    fun onSettingsReturnIfReady() {
        val current = session ?: return
        if (!current.settingsLaunchPending || !current.settingsLaunchObservedPause) return
        recheckAfterSettingsReturn(current)
    }

    private fun startSession(
        target: PermissionRequestTarget,
        status: PermissionStatus,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
        onResult: (PermissionRequestResult) -> Unit,
    ): PermissionRequestResult {
        if (session != null) {
            onResult(PermissionRequestResult.Busy)
            return PermissionRequestResult.Busy
        }
        if (status.granted) {
            onGranted()
            onResult(PermissionRequestResult.Granted)
            return PermissionRequestResult.Granted
        }
        session =
            PermissionSession(
                target = target,
                missingPermission = status.missing.firstOrNull(),
                onGranted = onGranted,
                onRejected = onRejected,
                onResult = onResult,
            )
        return PermissionRequestResult.Started
    }

    private fun startDirectSession(
        target: PermissionRequestTarget,
        status: PermissionStatus,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
        onResult: (PermissionRequestResult) -> Unit,
    ): PermissionRequestResult {
        if (session != null) {
            onResult(PermissionRequestResult.Busy)
            return PermissionRequestResult.Busy
        }
        if (status.granted) {
            onGranted()
            onResult(PermissionRequestResult.Granted)
            return PermissionRequestResult.Granted
        }
        val current =
            PermissionSession(
                target = target,
                missingPermission = status.missing.firstOrNull(),
                onGranted = onGranted,
                onRejected = onRejected,
                onResult = onResult,
                showDialog = false,
            )
        session = current
        return launchPermissionPlan(current, showSettingsDialog = true)
    }

    private fun startSettingsSession(
        target: PermissionRequestTarget,
        status: PermissionStatus,
        onGranted: () -> Unit,
        onRejected: () -> Unit,
        onResult: (PermissionRequestResult) -> Unit,
    ): PermissionRequestResult {
        if (session != null) {
            onResult(PermissionRequestResult.Busy)
            return PermissionRequestResult.Busy
        }
        val current =
            PermissionSession(
                target = target,
                missingPermission = status.missing.firstOrNull(),
                onGranted = onGranted,
                onRejected = onRejected,
                onResult = onResult,
                showDialog = false,
            )
        session = current
        return when (val plan = settingsPlan(target)) {
            PermissionRequestPlan.AlreadyGranted -> {
                finishIfGranted()
                PermissionRequestResult.Granted
            }
            is PermissionRequestPlan.OpenSettings -> {
                pendingLaunch = PermissionLaunch.Settings(current.target, plan.permission)
                session = current.copy(settingsLaunchPending = true)
                PermissionRequestResult.Started
            }
            PermissionRequestPlan.Unavailable,
            is PermissionRequestPlan.RequestRuntime,
            -> {
                dismiss(notifyRejected = true, result = PermissionRequestResult.SettingsUnavailable)
                PermissionRequestResult.SettingsUnavailable
            }
        }
    }

    private fun launchPermissionPlan(
        current: PermissionSession,
        showSettingsDialog: Boolean,
    ): PermissionRequestResult {
        return when (val plan = requestPlan(current.target)) {
            PermissionRequestPlan.AlreadyGranted -> {
                finishIfGranted()
                PermissionRequestResult.Granted
            }
            is PermissionRequestPlan.RequestRuntime -> {
                pendingLaunch = PermissionLaunch.Runtime(current.target, plan.permissions)
                session = current.copy(showDialog = false)
                PermissionRequestResult.Started
            }
            is PermissionRequestPlan.OpenSettings -> {
                if (showSettingsDialog) {
                    session = current.copy(showDialog = true)
                } else {
                    pendingLaunch = PermissionLaunch.Settings(current.target, plan.permission)
                    session = current.copy(showDialog = false, settingsLaunchPending = true)
                }
                PermissionRequestResult.Started
            }
            PermissionRequestPlan.Unavailable -> {
                dismiss(notifyRejected = true, result = PermissionRequestResult.SettingsUnavailable)
                PermissionRequestResult.SettingsUnavailable
            }
        }
    }

    private fun recheckAfterRuntimeRequest() {
        val current = session ?: return
        recheckAfterPermissionReturn(current)
    }

    private fun recheckAfterSettingsReturn(previous: PermissionSession) {
        recheckAfterPermissionReturn(previous)
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
            PermissionRecheckDecision.Denied ->
                dismiss(notifyRejected = true, result = PermissionRequestResult.Denied)
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
