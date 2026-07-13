package com.quickcleanpro.phonecleaner.app.permission

sealed interface PermissionRequest {
    data object AlreadyGranted : PermissionRequest

    data class Runtime(
        val permission: PermissionType,
        val permissions: List<String>,
    ) : PermissionRequest

    data class Settings(
        val permission: PermissionType,
    ) : PermissionRequest

    data object Unavailable : PermissionRequest
}

typealias PermissionRequestPlan = PermissionRequest

interface PermissionGateway {
    fun state(permission: PermissionType): PermissionState

    fun check(permission: PermissionType): PermissionState = state(permission)

    fun request(permission: PermissionType): PermissionRequest

    fun requestPlan(permission: PermissionType): PermissionRequest = request(permission)

    fun consumeRuntimeResult(
        permission: PermissionType,
        result: Map<String, Boolean>,
    ): PermissionState

    fun onRuntimeResult(
        permission: PermissionType,
        result: Map<String, Boolean>,
    ): PermissionState = consumeRuntimeResult(permission, result)
}

interface PermissionHost {
    fun launch(request: PermissionRequest): Boolean
}

fun resolvePermissionRequest(
    permission: PermissionType,
    state: PermissionState,
    runtimePermissions: List<String>,
    shouldRequestRuntime: Boolean,
    hasSettings: Boolean,
): PermissionRequest =
    when {
        state is PermissionState.Granted -> PermissionRequest.AlreadyGranted
        state is PermissionState.Unavailable -> PermissionRequest.Unavailable
        runtimePermissions.isNotEmpty() && shouldRequestRuntime ->
            PermissionRequest.Runtime(permission, runtimePermissions)
        hasSettings -> PermissionRequest.Settings(permission)
        else -> PermissionRequest.Unavailable
    }
