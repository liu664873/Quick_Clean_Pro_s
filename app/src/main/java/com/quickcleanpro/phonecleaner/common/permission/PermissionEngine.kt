package com.quickcleanpro.phonecleaner.common.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionEngine(
    handlers: List<PermissionHandler> = commonPermissionHandlers(),
    private val denialStore: RuntimePermissionDenialStore,
    private val isRuntimePermissionGranted: (Context, String) -> Boolean = { context, permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    },
    private val settingsIntentKey: (android.content.Intent) -> String = { it.toUri(0) },
) {
    private val handlersByPermission = handlers.associateBy(PermissionHandler::permission)
    private var pendingRuntimePermissions: Map<String, PermissionType> = emptyMap()

    fun status(
        context: Context,
        requiredPermissions: List<PermissionType>,
    ): PermissionStatus {
        val missing = missingPermissions(context, requiredPermissions)
        return PermissionStatus(granted = missing.isEmpty(), missing = missing)
    }

    fun decide(
        context: Context,
        requiredPermissions: List<PermissionType>,
    ): PermissionDecision {
        val permission = missingPermissions(context, requiredPermissions).firstOrNull()
            ?: return PermissionDecision.Granted
        val handler = handlersByPermission[permission] ?: return PermissionDecision.Unavailable
        val runtimePermissions =
            runCatching { handler.runtimePermissions(context) }
                .getOrElse { return PermissionDecision.Unavailable }
                .filterNot { isRuntimePermissionGranted(context, it) }
                .toTypedArray()

        if (
            runtimePermissions.isNotEmpty() &&
            denialStore.shouldRequestRuntimePermission(context, permission, runtimePermissions)
        ) {
            denialStore.markRequested(permission)
            pendingRuntimePermissions = runtimePermissions.associateWith { permission }
            return PermissionDecision.RequestRuntime(runtimePermissions)
        }

        pendingRuntimePermissions = emptyMap()
        return settingsDecision(context, permission)
    }

    fun settingsDecision(
        context: Context,
        permission: PermissionType,
    ): PermissionDecision {
        val handler = handlersByPermission[permission] ?: return PermissionDecision.Unavailable
        val intents =
            runCatching { handler.settingsIntents(context) }
                .getOrElse { return PermissionDecision.Unavailable }
                .distinctBy(settingsIntentKey)
        return if (intents.isEmpty()) PermissionDecision.Unavailable
        else PermissionDecision.OpenSettings(intents)
    }

    fun onRuntimeResult(result: Map<String, Boolean>) {
        if (result.isNotEmpty() && result.values.any { !it }) {
            result.filterValues { !it }
                .keys
                .mapNotNull(pendingRuntimePermissions::get)
                .distinct()
                .forEach(denialStore::markDenied)
        }
        pendingRuntimePermissions = emptyMap()
    }

    private fun missingPermissions(
        context: Context,
        requiredPermissions: List<PermissionType>,
    ): List<PermissionType> =
        requiredPermissions.filter { permission ->
            val handler = handlersByPermission[permission] ?: return@filter true
            !runCatching { handler.isGranted(context) }.getOrDefault(false)
        }
}
