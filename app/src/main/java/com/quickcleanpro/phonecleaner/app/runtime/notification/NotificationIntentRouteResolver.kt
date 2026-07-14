package com.quickcleanpro.phonecleaner.app.runtime.notification

import android.content.Intent
import android.util.Log
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

internal object NotificationIntentRouteResolver {
    private const val EXTRA_SDK_ROUTE = "Route"
    private const val EXTRA_LEGACY_TARGET_ROUTE = "quickclean_target_route"
    private const val EXTRA_LEGACY_ROUTE_LOWER_CASE = "route"
    private const val EXTRA_LEGACY_TARGET_ROUTE_SNAKE_CASE = "target_route"
    private const val EXTRA_LEGACY_TARGET_ROUTE_CAMEL_CASE = "targetRoute"
    private const val TAG = "NotificationRoute"
    private val routeExtraKeys =
        listOf(
            EXTRA_SDK_ROUTE,
            EXTRA_LEGACY_TARGET_ROUTE,
            EXTRA_LEGACY_ROUTE_LOWER_CASE,
            EXTRA_LEGACY_TARGET_ROUTE_SNAKE_CASE,
            EXTRA_LEGACY_TARGET_ROUTE_CAMEL_CASE,
        )

    fun targetRoute(intent: Intent?): String? {
        if (intent == null) return null

        return resolveTargetRouteValues(intent::getStringExtra)
    }

    internal fun targetRouteFromExtras(extras: Map<String, String?>): String? =
        resolveTargetRouteValues { key -> extras[key] }

    internal fun resolveTargetRouteCandidates(candidates: List<String>): String? {
        for (candidate in candidates) {
            val route = AppDestination.normalizeNotificationRoute(candidate)
            if (route != null && isValidRoute(route)) {
                return route
            }
        }

        if (candidates.isNotEmpty()) {
            runCatching {
                Log.w(TAG, "unknown notification route candidates=$candidates; fallback to home")
            }
            return AppDestination.Home.route
        }
        return null
    }

    fun isValidRoute(route: String): Boolean =
        AppDestination.normalizeNotificationRoute(route) in validRoutes

    private fun resolveTargetRouteValues(valueForKey: (String) -> String?): String? =
        resolveTargetRouteCandidates(
            routeExtraKeys.mapNotNull { key ->
                valueForKey(key)?.trim()?.takeIf { it.isNotEmpty() }
            },
        )

    private val validRoutes: Set<String> = AppDestination.notificationTargetRoutes
}
