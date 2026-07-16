package com.quickcleanpro.phonecleaner.app.runtime

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Starts application SDK work without allowing one optional integration to cancel the others.
 * The state is also the readiness barrier used by startup code that must wait before showing ads.
 */
class SdkInitializationCoordinator(
    private val scope: CoroutineScope,
    private val advertiseInitializer: suspend () -> Unit,
    private val analyticsInitializer: suspend () -> Unit,
    private val notificationDefaultsInitializer: suspend () -> Unit,
    private val logger: (String, Throwable?) -> Unit = { _, _ -> },
) {
    private val started = AtomicBoolean(false)
    private val _state = MutableStateFlow(AppSdkInitializationState())
    private val advertiseFinished = CompletableDeferred<Unit>()

    val state: StateFlow<AppSdkInitializationState> = _state.asStateFlow()

    /** Starts each initializer once. Individual failures are reflected in state and isolated. */
    fun start() {
        if (!started.compareAndSet(false, true)) return

        scope.launch {
            runComponent(
                component = SdkComponent.ADVERTISE,
                initializer = advertiseInitializer,
            )
            advertiseFinished.complete(Unit)
        }
        scope.launch {
            awaitAdvertiseFinishedWithTimeout()
            runComponent(
                component = SdkComponent.ANALYTICS,
                initializer = analyticsInitializer,
            )
        }
        scope.launch {
            advertiseFinished.await()
            runComponent(
                component = SdkComponent.NOTIFICATION_DEFAULTS,
                initializer = notificationDefaultsInitializer,
            )
        }
    }

    private suspend fun awaitAdvertiseFinishedWithTimeout() {
        withTimeoutOrNull(DEPENDENT_INITIALIZER_WAIT_MS) {
            advertiseFinished.await()
        }
    }

    /**
     * Waits for the advertisement initializer to finish. A false result means failed or timed
     * out; callers can inspect [state] for the failure cause.
     */
    suspend fun awaitAdvertiseReady(timeoutMillis: Long = DEFAULT_ADVERTISE_WAIT_MS): Boolean {
        return awaitReady(timeoutMillis) { current ->
            val advertise = current.advertise
            when (advertise.status) {
                InitializationStatus.SUCCEEDED -> ReadinessResult.Ready
                InitializationStatus.FAILED -> ReadinessResult.Failed
                else -> ReadinessResult.Waiting
            }
        }
    }

    suspend fun awaitNotificationDefaultsReady(
        timeoutMillis: Long = DEFAULT_NOTIFICATION_DEFAULTS_WAIT_MS,
    ): Boolean =
        awaitReady(timeoutMillis) { current ->
            val advertise = current.advertise.status
            val notificationDefaults = current.notificationDefaults.status
            when {
                advertise == InitializationStatus.FAILED ||
                    notificationDefaults == InitializationStatus.FAILED -> ReadinessResult.Failed
                advertise == InitializationStatus.SUCCEEDED &&
                    notificationDefaults == InitializationStatus.SUCCEEDED -> ReadinessResult.Ready
                else -> ReadinessResult.Waiting
            }
        }

    private suspend fun awaitReady(
        timeoutMillis: Long,
        evaluate: (AppSdkInitializationState) -> ReadinessResult,
    ): Boolean {
        when (evaluate(state.value)) {
            ReadinessResult.Ready -> return true
            ReadinessResult.Failed -> return false
            ReadinessResult.Waiting -> if (timeoutMillis <= 0L) return false
        }
        return try {
            withTimeoutOrNull(timeoutMillis) {
                state
                    .map(evaluate)
                    .first { it != ReadinessResult.Waiting } == ReadinessResult.Ready
            } ?: false
        } catch (error: CancellationException) {
            throw error
        }
    }

    private suspend fun runComponent(
        component: SdkComponent,
        initializer: suspend () -> Unit,
    ) {
        update(component, ComponentInitializationState(InitializationStatus.RUNNING))
        try {
            initializer()
            update(component, ComponentInitializationState(InitializationStatus.SUCCEEDED))
            logger("${component.logName} initialization succeeded", null)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            update(component, ComponentInitializationState(InitializationStatus.FAILED, error))
            logger("${component.logName} initialization failed", error)
        }
    }

    private fun update(
        component: SdkComponent,
        value: ComponentInitializationState,
    ) {
        _state.update { current ->
            when (component) {
                SdkComponent.ADVERTISE -> current.copy(advertise = value)
                SdkComponent.ANALYTICS -> current.copy(analytics = value)
                SdkComponent.NOTIFICATION_DEFAULTS -> current.copy(notificationDefaults = value)
            }
        }
    }

    private enum class SdkComponent(val logName: String) {
        ADVERTISE("AdvertiseSdk"),
        ANALYTICS("Analytics"),
        NOTIFICATION_DEFAULTS("Advertise notification defaults"),
    }

    private enum class ReadinessResult {
        Waiting,
        Ready,
        Failed,
    }

    private companion object {
        const val DEFAULT_ADVERTISE_WAIT_MS = 6_500L
        const val DEFAULT_NOTIFICATION_DEFAULTS_WAIT_MS = 15_000L
        const val DEPENDENT_INITIALIZER_WAIT_MS = 6_500L
    }
}

enum class InitializationStatus {
    NOT_STARTED,
    RUNNING,
    SUCCEEDED,
    FAILED,
}

data class ComponentInitializationState(
    val status: InitializationStatus = InitializationStatus.NOT_STARTED,
    val error: Throwable? = null,
)

data class AppSdkInitializationState(
    val advertise: ComponentInitializationState = ComponentInitializationState(),
    val analytics: ComponentInitializationState = ComponentInitializationState(),
    val notificationDefaults: ComponentInitializationState = ComponentInitializationState(),
)
