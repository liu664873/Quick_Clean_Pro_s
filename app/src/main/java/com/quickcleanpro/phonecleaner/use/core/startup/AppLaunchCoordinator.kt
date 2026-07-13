package com.quickcleanpro.phonecleaner.use.core.startup

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AppLaunchRequest {
    data object Normal : AppLaunchRequest
    data class NotificationTarget(
        val route: String,
        val source: NotificationLaunchSource = NotificationLaunchSource.InitialIntent,
    ) : AppLaunchRequest
}

enum class NotificationLaunchSource {
    InitialIntent,
    NewIntent,
}

class AppLaunchCoordinator(
    private val targetRouteResolver: (Intent?) -> String? = { null },
) {
    private val _pendingRequest = MutableStateFlow<AppLaunchRequest>(AppLaunchRequest.Normal)
    val pendingRequest: StateFlow<AppLaunchRequest> = _pendingRequest.asStateFlow()

    fun onCreate(intent: Intent?) {
        _pendingRequest.value = targetRouteResolver(intent)
            ?.let { route ->
                AppLaunchRequest.NotificationTarget(
                    route = route,
                    source = NotificationLaunchSource.InitialIntent,
                )
            }
            ?: AppLaunchRequest.Normal
    }

    fun onNewIntent(intent: Intent?) {
        targetRouteResolver(intent)?.let { route ->
            _pendingRequest.value =
                AppLaunchRequest.NotificationTarget(
                    route = route,
                    source = NotificationLaunchSource.NewIntent,
                )
        }
    }

    fun consumeRequest(): AppLaunchRequest {
        val request = _pendingRequest.value
        _pendingRequest.value = AppLaunchRequest.Normal
        return request
    }

    fun consumeRequestIfCurrent(request: AppLaunchRequest): Boolean {
        if (_pendingRequest.value != request) return false
        _pendingRequest.value = AppLaunchRequest.Normal
        return true
    }
}
