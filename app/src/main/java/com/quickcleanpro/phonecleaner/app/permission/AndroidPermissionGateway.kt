package com.quickcleanpro.phonecleaner.app.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXRuntimePermissionDenialStore
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionHandler
import com.quickcleanpro.phonecleaner.use.core.permission.RuntimePermissionDenialStore
import com.quickcleanpro.phonecleaner.use.core.permission.commonPermissionHandlers

class AndroidPermissionGateway(
    context: Context,
    handlers: List<PermissionHandler> = commonPermissionHandlers(),
    private val denialStore: RuntimePermissionDenialStore =
        CleanXRuntimePermissionDenialStore(context.applicationContext),
) : PermissionGateway {
    private val context = context
    private val handlersByKey = handlers.associateBy { it.permission.key }
    private val pendingRuntimeRequests = mutableMapOf<PermissionType, Set<String>>()

    override fun state(permission: PermissionType): PermissionState {
        val handler = handlerFor(permission) ?: return PermissionState.Unavailable
        val granted = runCatching { handler.isGranted(context) }.getOrDefault(false)
        if (granted) return PermissionState.Granted

        val runtimePermissions = missingRuntimePermissions(handler)
        val permanentlyDenied =
            runtimePermissions.isNotEmpty() &&
                !denialStore.shouldRequestRuntimePermission(
                    context,
                    permission,
                    runtimePermissions.toTypedArray(),
                )
        return PermissionState.Denied(permanently = permanentlyDenied)
    }

    override fun request(permission: PermissionType): PermissionRequest {
        val handler = handlerFor(permission) ?: return PermissionRequest.Unavailable
        val currentState = state(permission)
        if (currentState is PermissionState.Granted) return PermissionRequest.AlreadyGranted
        if (currentState is PermissionState.Unavailable) return PermissionRequest.Unavailable

        val runtimePermissions = missingRuntimePermissions(handler)
        val shouldRequestRuntime =
            runtimePermissions.isNotEmpty() &&
                denialStore.shouldRequestRuntimePermission(
                    context,
                    permission,
                    runtimePermissions.toTypedArray(),
                )
        val settingsAvailable = handler.settingsIntents(context).isNotEmpty()
        val request =
            resolvePermissionRequest(
                permission = permission,
                state = currentState,
                runtimePermissions = runtimePermissions,
                shouldRequestRuntime = shouldRequestRuntime,
                hasSettings = settingsAvailable,
            )
        when (request) {
            is PermissionRequest.Runtime -> {
                denialStore.markRequested(permission)
                pendingRuntimeRequests[permission] = request.permissions.toSet()
            }
            else -> pendingRuntimeRequests.remove(permission)
        }
        return request
    }

    override fun consumeRuntimeResult(
        permission: PermissionType,
        result: Map<String, Boolean>,
    ): PermissionState {
        val requested = pendingRuntimeRequests.remove(permission).orEmpty()
        val denied =
            result
                .filterValues { granted -> !granted }
                .keys
                .filter { requested.isEmpty() || it in requested }
        if (denied.isNotEmpty()) {
            denialStore.markDenied(permission)
        }
        return state(permission)
    }

    fun settingsIntents(permission: PermissionType): List<Intent> =
        handlerFor(permission)
            ?.settingsIntents(context)
            .orEmpty()
            .distinctBy { it.toUri(0) }

    private fun handlerFor(permission: PermissionType): PermissionHandler? =
        handlersByKey[permission.key]

    private fun missingRuntimePermissions(handler: PermissionHandler): List<String> =
        handler
            .runtimePermissions(context)
            .filter { runtimePermission ->
                ContextCompat.checkSelfPermission(context, runtimePermission) != PackageManager.PERMISSION_GRANTED
            }

}

class AndroidPermissionHost(
    context: Context,
    private val gateway: AndroidPermissionGateway = AndroidPermissionGateway(context),
    private val runtimeLauncher: (List<String>) -> Boolean = { false },
    private val settingsLauncher: (Intent) -> Boolean = { intent ->
        runCatching { context.startActivity(intent) }.isSuccess
    },
) : PermissionHost {
    override fun launch(request: PermissionRequest): Boolean =
        when (request) {
            PermissionRequest.AlreadyGranted -> true
            is PermissionRequest.Runtime -> runtimeLauncher(request.permissions)
            is PermissionRequest.Settings ->
                gateway.settingsIntents(request.permission).any(settingsLauncher)
            PermissionRequest.Unavailable -> false
        }
}
