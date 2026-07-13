package com.quickcleanpro.phonecleaner.common.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

interface InterstitialAdRunner {
    fun run(
        scene: AdScene?,
        requestId: String,
        onContinue: () -> Unit,
    )

    fun runRouteEntry(
        fromRoute: String?,
        targetRoute: String?,
        onContinue: () -> Unit,
    )
}

enum class AdPreloadStage {
    Startup,
    MainPage,
    InterstitialClosed,
}

data class AdRuntimeState(
    val permissionFlowActive: Boolean = false,
    val featureOperationActive: Boolean = false,
)

class AdRuntime internal constructor(
    private val stateProvider: () -> AdRuntimeState,
    private val onInterstitialStateChanged: (Boolean) -> Unit,
    private val driver: AdRuntimeDriver,
    private val scheduler: AdRuntimeScheduler,
    private val navigationPolicy: AdNavigationPolicy,
) : InterstitialAdRunner {
    constructor(
        activityProvider: () -> Activity?,
        stateProvider: () -> AdRuntimeState = { AdRuntimeState() },
        onInterstitialStateChanged: (Boolean) -> Unit = {},
        navigationPolicy: AdNavigationPolicy = AdNavigationPolicy(),
    ) : this(
        stateProvider = stateProvider,
        onInterstitialStateChanged = onInterstitialStateChanged,
        driver = AdvertiseAdRuntimeDriver(activityProvider),
        scheduler = HandlerAdRuntimeScheduler(),
        navigationPolicy = navigationPolicy,
    )

    private val lock = Any()
    private val queuedRequests = ArrayDeque<AdRequest>()
    private val knownRequestIds = mutableSetOf<String>()
    private val appOpenGuard = AppOpenGuard(driver)
    private var activeRequest: AdRequest? = null
    private var launchPending = false
    private var externalActivityRelease: (() -> Unit)? = null
    private var externalReturnCooldown: AdRuntimeCancellation? = null

    @Volatile
    var externalActivityReturning: Boolean = false
        private set

    override fun run(
        scene: AdScene?,
        requestId: String,
        onContinue: () -> Unit,
    ) {
        if (scene == null) {
            onContinue()
            return
        }
        enqueue(
            AdRequest(
                scene = scene,
                requestId = requestId,
                areaKey = AdPlacementRegistry.interstitialArea(scene),
                onContinue = once(onContinue),
            ),
        )
    }

    override fun runRouteEntry(
        fromRoute: String?,
        targetRoute: String?,
        onContinue: () -> Unit,
    ) {
        val decision = navigationPolicy.entryAdDecision(fromRoute, targetRoute)
        if (decision == null) {
            onContinue()
            return
        }
        run(
            scene = AdScene.EnterFeature(decision.feature, decision.route),
            requestId = "route_enter_${decision.feature.name}_${decision.route}",
            onContinue = onContinue,
        )
    }

    fun runColdStart(
        context: Context,
        onOpenAdStateChanged: (Boolean) -> Unit = {},
        onFinished: () -> Unit,
    ) = runColdStart(
        preloadStartup = { preload(AdPreloadStage.Startup, context) },
        onOpenAdStateChanged = onOpenAdStateChanged,
        onFinished = onFinished,
    )

    internal fun runColdStart(
        preloadStartup: () -> Unit,
        onOpenAdStateChanged: (Boolean) -> Unit = {},
        onFinished: () -> Unit,
    ) {
        val releaseSuppression = appOpenGuard.acquire(AppOpenReason.Startup)
        val finish = once {
            releaseSuppression()
            onFinished()
        }
        showStartupConsent {
            preloadStartup()
            showOpenAd(
                onOpenAdStateChanged = onOpenAdStateChanged,
                onContinue = finish,
            )
        }
    }

    fun preload(stage: AdPreloadStage, context: Context) {
        runCatching {
            when (stage) {
                AdPreloadStage.Startup -> driver.preloadStartup(context.applicationContext)
                AdPreloadStage.MainPage -> driver.preloadMainPage(context.applicationContext)
                AdPreloadStage.InterstitialClosed -> driver.preloadAfterInterstitial()
            }
        }.onFailure { throwable ->
            logW(TAG_PRELOAD, "preload failed for ${stage.name}", throwable)
        }
    }

    fun markExternalActivityLaunch() {
        launchPending = true
        externalActivityReturning = false
        externalReturnCooldown?.cancel()
        externalReturnCooldown = null
        if (externalActivityRelease == null) {
            externalActivityRelease = appOpenGuard.acquire(AppOpenReason.ExternalActivity)
        }
        driver.suppressNextAppOpen(true)
    }

    fun cancelExternalActivityLaunch() {
        launchPending = false
        externalActivityReturning = false
        restoreExternalActivityAppOpen()
    }

    fun onHostResumed() {
        if (!launchPending) return
        launchPending = false
        externalActivityReturning = true
        externalReturnCooldown?.cancel()
        externalReturnCooldown = scheduler.schedule(EXTERNAL_ACTIVITY_RETURN_COOLDOWN_MS) {
            if (!launchPending) {
                externalActivityReturning = false
                restoreExternalActivityAppOpen()
            }
        }
    }

    fun dispose() {
        launchPending = false
        externalActivityReturning = false
        externalReturnCooldown?.cancel()
        externalReturnCooldown = null
        restoreExternalActivityAppOpen()
    }

    private fun enqueue(request: AdRequest) {
        var startNow = false
        val accepted = synchronized(lock) {
            if (!knownRequestIds.add(request.requestId)) {
                false
            } else {
                if (activeRequest == null) {
                    activeRequest = request
                    startNow = true
                } else {
                    queuedRequests.addLast(request)
                }
                true
            }
        }
        if (!accepted) return
        if (startNow) {
            onInterstitialStateChanged(true)
            execute(request)
        }
    }

    private fun execute(request: AdRequest) {
        val finish = once { complete(request) }
        if (!canShow(request)) {
            finish()
            return
        }
        val areaKey = request.areaKey?.takeIf(String::isNotBlank)
        if (areaKey == null) {
            finish()
            return
        }
        val closed = once {
            logD(
                TAG_INTERSTITIAL,
                "interstitial closed scene=${request.scene} areaKey=$areaKey requestId=${request.requestId}",
            )
            if (driver.isActivityAvailable()) {
                runCatching(driver::preloadAfterInterstitial)
                    .onFailure { throwable ->
                        logW(TAG_PRELOAD, "preload failed for ${AdPreloadStage.InterstitialClosed.name}", throwable)
                    }
            }
            finish()
        }
        val started = runCatching {
            driver.showInterstitial(areaKey, closed)
        }.onFailure { throwable ->
            logW(TAG_INTERSTITIAL, "show interstitial failed for $areaKey", throwable)
        }.getOrDefault(false)
        if (!started) finish()
    }

    private fun canShow(request: AdRequest): Boolean {
        if (request.areaKey.isNullOrBlank()) return false
        val state = stateProvider()
        if (state.permissionFlowActive && request.scene !is AdScene.PermissionRejected) return false
        if (externalActivityReturning && request.scene !is AdScene.PermissionRejected) return false
        if (state.featureOperationActive) return false
        return true
    }

    private fun complete(request: AdRequest) {
        var nextRequest: AdRequest? = null
        var becameIdle = false
        val completed = synchronized(lock) {
            if (activeRequest !== request) {
                false
            } else {
                knownRequestIds.remove(request.requestId)
                nextRequest = queuedRequests.pollFirst()
                activeRequest = nextRequest
                becameIdle = nextRequest == null
                true
            }
        }
        if (!completed) return
        if (becameIdle) onInterstitialStateChanged(false)
        try {
            request.onContinue()
        } finally {
            nextRequest?.let(::execute)
        }
    }

    private fun showStartupConsent(onContinue: () -> Unit) {
        if (!driver.isActivityAvailable()) {
            logW(TAG_STARTUP, "skip startup consent: activity unavailable")
            onContinue()
            return
        }
        var initTimeout: AdRuntimeCancellation? = null
        var showTimeout: AdRuntimeCancellation? = null
        val finish = once {
            initTimeout?.cancel()
            showTimeout?.cancel()
            onContinue()
        }
        val showConsent = once {
            initTimeout?.cancel()
            if (!driver.isActivityAvailable()) {
                logW(TAG_STARTUP, "skip startup consent show: activity unavailable")
                finish()
                return@once
            }
            logD(TAG_STARTUP, "show startup consent")
            showTimeout = scheduler.schedule(STARTUP_CONSENT_SHOW_TIMEOUT_MS) {
                logW(TAG_STARTUP, "startup consent show timeout")
                finish()
            }
            runCatching {
                driver.showSplashConsent {
                    logD(TAG_STARTUP, "startup consent finished")
                    finish()
                }
            }.onFailure { throwable ->
                logW(TAG_STARTUP, "show startup consent failed", throwable)
                finish()
            }
        }

        initTimeout = scheduler.schedule(STARTUP_CONSENT_INIT_TIMEOUT_MS) { showConsent() }
        runCatching {
            logD(TAG_STARTUP, "init startup consent")
            val hasCachedConsentState = driver.initConsent { success ->
                logD(TAG_STARTUP, "startup consent init callback success=$success")
                showConsent()
            }
            if (hasCachedConsentState) {
                logD(TAG_STARTUP, "startup consent has cached state")
                showConsent()
            }
        }.onFailure { throwable ->
            logW(TAG_STARTUP, "init startup consent failed", throwable)
            finish()
        }
    }

    private fun showOpenAd(
        onOpenAdStateChanged: (Boolean) -> Unit,
        onContinue: () -> Unit,
    ) {
        if (!driver.isActivityAvailable()) {
            logW(TAG_STARTUP, "skip cold start open ad: activity unavailable")
            onOpenAdStateChanged(false)
            onContinue()
            return
        }
        val loaded = AtomicBoolean(false)
        val completed = AtomicBoolean(false)
        var startTimeout: AdRuntimeCancellation? = null
        var totalTimeout: AdRuntimeCancellation? = null
        var closeSettle: AdRuntimeCancellation? = null

        fun continueOnce() {
            if (!completed.compareAndSet(false, true)) return
            startTimeout?.cancel()
            totalTimeout?.cancel()
            closeSettle?.cancel()
            onOpenAdStateChanged(false)
            onContinue()
        }

        startTimeout = scheduler.schedule(OPEN_AD_START_TIMEOUT_MS) {
            if (!loaded.get()) {
                logW(TAG_STARTUP, "cold start open ad start timeout")
                continueOnce()
            }
        }
        runCatching {
            driver.showOpenAd(
                areaKey = AdAreaKeys.Open.OPEN_PAGE,
                onClosed = {
                    logD(TAG_STARTUP, "cold start open ad closed")
                    val delay = if (loaded.get()) OPEN_AD_CLOSE_SETTLE_MS else 0L
                    closeSettle = scheduler.schedule(delay, ::continueOnce)
                },
                onLoaded = {
                    if (!completed.get() && loaded.compareAndSet(false, true)) {
                        startTimeout?.cancel()
                        totalTimeout = scheduler.schedule(OPEN_AD_TOTAL_TIMEOUT_MS) {
                            logW(TAG_STARTUP, "cold start open ad total timeout")
                            continueOnce()
                        }
                        onOpenAdStateChanged(true)
                        logD(TAG_STARTUP, "cold start open ad loaded")
                    }
                },
            )
        }.onFailure { throwable ->
            logW(TAG_STARTUP, "show cold start open ad failed", throwable)
            continueOnce()
        }
    }

    private fun restoreExternalActivityAppOpen() {
        externalActivityRelease?.invoke()
        externalActivityRelease = null
        driver.suppressNextAppOpen(false)
    }

    private data class AdRequest(
        val scene: AdScene,
        val requestId: String,
        val areaKey: String?,
        val onContinue: () -> Unit,
    )

    private companion object {
        const val TAG_INTERSTITIAL = "QuickCleanInterAd"
        const val TAG_PRELOAD = "QuickCleanAdPreload"
        const val TAG_STARTUP = "QuickCleanStartup"
        const val STARTUP_CONSENT_INIT_TIMEOUT_MS = 6_500L
        const val STARTUP_CONSENT_SHOW_TIMEOUT_MS = 6_500L
        const val OPEN_AD_START_TIMEOUT_MS = 6_500L
        const val OPEN_AD_TOTAL_TIMEOUT_MS = 30_000L
        const val OPEN_AD_CLOSE_SETTLE_MS = 800L
        const val EXTERNAL_ACTIVITY_RETURN_COOLDOWN_MS = 1_200L
    }
}

internal interface AdRuntimeDriver {
    fun isActivityAvailable(): Boolean
    fun initConsent(onResult: (Boolean) -> Unit): Boolean
    fun showSplashConsent(onFinished: () -> Unit)
    fun showOpenAd(
        areaKey: String,
        onClosed: () -> Unit,
        onLoaded: () -> Unit,
    )
    fun showInterstitial(areaKey: String, onClosed: () -> Unit): Boolean
    fun preloadStartup(context: Context)
    fun preloadMainPage(context: Context)
    fun preloadAfterInterstitial()
    fun isAppOpenEnabled(): Boolean
    fun setAppOpenEnabled(enabled: Boolean)
    fun suppressNextAppOpen(suppress: Boolean)
}

private class AdvertiseAdRuntimeDriver(
    private val activityProvider: () -> Activity?,
) : AdRuntimeDriver {
    override fun isActivityAvailable(): Boolean = availableActivity() != null

    override fun initConsent(onResult: (Boolean) -> Unit): Boolean =
        availableActivity()?.let { AdvertiseSdkAdapter.initConsent(it, onResult) } ?: false

    override fun showSplashConsent(onFinished: () -> Unit) {
        availableActivity()?.let { AdvertiseSdkAdapter.showSplashConsent(it, onFinished) } ?: onFinished()
    }

    override fun showOpenAd(
        areaKey: String,
        onClosed: () -> Unit,
        onLoaded: () -> Unit,
    ) {
        val activity = availableActivity() ?: run {
            onClosed()
            return
        }
        AdvertiseSdkAdapter.showOpenAd(activity, areaKey, onClosed, onLoaded)
    }

    override fun showInterstitial(areaKey: String, onClosed: () -> Unit): Boolean {
        val activity = availableActivity() ?: return false
        AdvertiseSdkAdapter.showInterstitial(activity, areaKey, onClosed)
        return true
    }

    override fun preloadStartup(context: Context) = AdvertiseSdkAdapter.preloadStartup(context)
    override fun preloadMainPage(context: Context) = AdvertiseSdkAdapter.preloadMainPage(context)
    override fun preloadAfterInterstitial() {
        availableActivity()?.let(AdvertiseSdkAdapter::preloadAfterPlayFinish)
    }
    override fun isAppOpenEnabled(): Boolean = AdvertiseSdkAdapter.isAppOpenEnabled()
    override fun setAppOpenEnabled(enabled: Boolean) = AdvertiseSdkAdapter.setAppOpenEnabled(enabled)
    override fun suppressNextAppOpen(suppress: Boolean) = AdvertiseSdkAdapter.suppressNextAppOpen(suppress)

    private fun availableActivity(): Activity? =
        activityProvider()?.takeUnless { it.isFinishing || it.isDestroyed }
}

internal fun interface AdRuntimeCancellation {
    fun cancel()
}

internal interface AdRuntimeScheduler {
    fun schedule(delayMillis: Long, block: () -> Unit): AdRuntimeCancellation
}

private class HandlerAdRuntimeScheduler(
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : AdRuntimeScheduler {
    override fun schedule(delayMillis: Long, block: () -> Unit): AdRuntimeCancellation {
        val runnable = Runnable(block)
        handler.postDelayed(runnable, delayMillis.coerceAtLeast(0L))
        return AdRuntimeCancellation { handler.removeCallbacks(runnable) }
    }
}

private class AppOpenGuard(
    private val driver: AdRuntimeDriver,
) {
    private val lock = Any()
    private var activeCount = 0
    private var restoreEnabled: Boolean? = null

    fun acquire(reason: AppOpenReason): () -> Unit {
        synchronized(lock) {
            if (activeCount == 0) {
                restoreEnabled = runCatching(driver::isAppOpenEnabled).getOrDefault(true)
                runCatching { driver.setAppOpenEnabled(false) }
            }
            activeCount += 1
        }
        val released = AtomicBoolean(false)
        return {
            if (released.compareAndSet(false, true)) release(reason)
        }
    }

    private fun release(reason: AppOpenReason) {
        var shouldRestore = false
        var enabledToRestore = true
        synchronized(lock) {
            if (activeCount <= 0) return
            activeCount -= 1
            if (activeCount == 0) {
                shouldRestore = true
                enabledToRestore = restoreEnabled ?: true
                restoreEnabled = null
            }
        }
        if (shouldRestore) runCatching { driver.setAppOpenEnabled(enabledToRestore) }
    }
}

private enum class AppOpenReason {
    Startup,
    ExternalActivity,
}

private fun logD(tag: String, message: String) {
    runCatching { Log.d(tag, message) }
}

private fun logW(tag: String, message: String, throwable: Throwable? = null) {
    runCatching {
        if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
    }
}
