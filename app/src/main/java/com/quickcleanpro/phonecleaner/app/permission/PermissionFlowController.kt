package com.quickcleanpro.phonecleaner.app.permission

interface PermissionFlowController {
    fun request(
        type: PermissionType,
        showRationale: Boolean,
        onResult: (PermissionOutcome) -> Unit,
    ): PermissionStartResult

    fun consumeRuntimeResult(type: PermissionType, grants: Map<String, Boolean>)

    fun consumeSettingsReturn()

    fun dismiss()
}

class DefaultPermissionFlowController(
    private val gateway: PermissionGateway,
    private val host: PermissionHost,
) : PermissionFlowController {
    private var active: ActiveRequest? = null

    override fun request(
        type: PermissionType,
        showRationale: Boolean,
        onResult: (PermissionOutcome) -> Unit,
    ): PermissionStartResult {
        if (active != null) {
            onResult(PermissionOutcome.Busy)
            return PermissionStartResult.Busy
        }

        val state = gateway.state(type)
        if (state is PermissionState.Granted) {
            return completeImmediately(PermissionOutcome.Granted, onResult)
        }
        if (state is PermissionState.Unavailable) {
            return completeImmediately(PermissionOutcome.Unavailable, onResult)
        }

        val request = gateway.requestPlan(type)
        return when (request) {
            PermissionRequest.AlreadyGranted -> completeImmediately(PermissionOutcome.Granted, onResult)
            PermissionRequest.Unavailable -> completeImmediately(PermissionOutcome.Unavailable, onResult)
            is PermissionRequest.Runtime,
            is PermissionRequest.Settings,
            -> {
                if (!host.launch(request)) {
                    completeImmediately(PermissionOutcome.Unavailable, onResult)
                } else {
                    active = ActiveRequest(type, showRationale, onResult)
                    PermissionStartResult.Started
                }
            }
        }
    }

    override fun consumeRuntimeResult(type: PermissionType, grants: Map<String, Boolean>) {
        val current = active ?: return
        if (current.type != type) return
        finish(gateway.consumeRuntimeResult(type, grants).toOutcome())
    }

    override fun consumeSettingsReturn() {
        val current = active ?: return
        finish(gateway.state(current.type).toOutcome())
    }

    override fun dismiss() {
        if (active != null) finish(PermissionOutcome.Dismissed)
    }

    private fun finish(outcome: PermissionOutcome) {
        val callback = active?.onResult ?: return
        active = null
        callback(outcome)
    }

    private fun completeImmediately(
        outcome: PermissionOutcome,
        callback: (PermissionOutcome) -> Unit,
    ): PermissionStartResult.Completed {
        callback(outcome)
        return PermissionStartResult.Completed(outcome)
    }

    private data class ActiveRequest(
        val type: PermissionType,
        val showRationale: Boolean,
        val onResult: (PermissionOutcome) -> Unit,
    )
}

private fun PermissionState.toOutcome(): PermissionOutcome =
    when (this) {
        PermissionState.Granted -> PermissionOutcome.Granted
        is PermissionState.Denied -> PermissionOutcome.Denied(permanently)
        PermissionState.Unavailable -> PermissionOutcome.Unavailable
    }
