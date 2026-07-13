package com.quickcleanpro.phonecleaner.app.runtime.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.MainActivity

object ToolNotificationIntentFactory {
    const val EXTRA_TARGET_ROUTE = "quickclean_target_route"
    private const val EXTRA_APP_OPEN_FROM = "AppOpenFrom"
    private const val EXTRA_SDK_ROUTE_TITLE_CASE = "Route"
    private const val EXTRA_SDK_ROUTE_LOWER_CASE = "route"
    private const val EXTRA_SDK_TARGET_ROUTE_SNAKE_CASE = "target_route"
    private const val EXTRA_SDK_TARGET_ROUTE_CAMEL_CASE = "targetRoute"
    const val ROUTE_HOME_FILE_MANAGER = AppDestination.HOME_FILE_MANAGER_ROUTE
    const val ROUTE_HOME_TOOLBOX = AppDestination.HOME_TOOLBOX_ROUTE
    val homeTabRoutes: Set<String> =
        setOf(ROUTE_HOME_FILE_MANAGER, ROUTE_HOME_TOOLBOX)

    private const val TOOL_CONTENT_REQUEST_BASE_CODE = 3000
    private const val ACTION_OPEN_TOOL = "com.quickcleanpro.phonecleaner.notification.OPEN_TOOL"
    private const val TAG = "ToolNotificationIntent"
    private val routeExtraKeys =
        listOf(
            EXTRA_TARGET_ROUTE,
            EXTRA_SDK_ROUTE_TITLE_CASE,
            EXTRA_SDK_ROUTE_LOWER_CASE,
            EXTRA_SDK_TARGET_ROUTE_SNAKE_CASE,
            EXTRA_SDK_TARGET_ROUTE_CAMEL_CASE,
        )

    fun pendingIntent(
        context: Context,
        route: String,
        requestCode: Int,
    ): PendingIntent {
        val appContext = context.applicationContext
        val intent =
            Intent(appContext, MainActivity::class.java).apply {
                action = "$ACTION_OPEN_TOOL.$route"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_TARGET_ROUTE, route)
                putExtra(EXTRA_APP_OPEN_FROM, "persistent")
                putExtra(EXTRA_SDK_ROUTE_TITLE_CASE, route)
            }
        return PendingIntent.getActivity(
            appContext,
            TOOL_CONTENT_REQUEST_BASE_CODE + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun targetRoute(intent: Intent?): String? {
        if (intent == null) return null

        return resolveTargetRouteCandidates(intent.routeCandidates())
    }

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

    private fun Intent.routeCandidates(): List<String> =
        buildList {
            routeExtraKeys.forEach { key ->
                getStringExtra(key)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(::add)
            }
            action
                ?.takeIf { it.startsWith("$ACTION_OPEN_TOOL.") }
                ?.removePrefix("$ACTION_OPEN_TOOL.")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::add)
        }

    private val validRoutes: Set<String> = AppDestination.notificationTargetRoutes
}
