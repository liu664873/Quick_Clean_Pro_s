package com.quickcleanpro.phonecleaner.advertise

import android.content.Context
import com.quickcleanpro.phonecleaner.common.ads.AdNavigationPolicy
import com.quickcleanpro.phonecleaner.common.ads.AdRuntime
import com.quickcleanpro.phonecleaner.common.ads.AdRuntimeCancellation
import com.quickcleanpro.phonecleaner.common.ads.AdRuntimeDriver
import com.quickcleanpro.phonecleaner.common.ads.AdRuntimeScheduler
import com.quickcleanpro.phonecleaner.common.ads.AdRuntimeState

internal fun fakeAdRuntime(
    driver: FakeAdRuntimeDriver = FakeAdRuntimeDriver(),
    scheduler: FakeAdRuntimeScheduler = FakeAdRuntimeScheduler(),
    stateProvider: () -> AdRuntimeState = { AdRuntimeState() },
    onInterstitialStateChanged: (Boolean) -> Unit = {},
    navigationPolicy: AdNavigationPolicy = AdNavigationPolicy(),
): AdRuntime =
    AdRuntime(
        stateProvider = stateProvider,
        onInterstitialStateChanged = onInterstitialStateChanged,
        driver = driver,
        scheduler = scheduler,
        navigationPolicy = navigationPolicy,
    )

internal class FakeAdRuntimeDriver : AdRuntimeDriver {
    var activityAvailable = true
    var interstitialStarted = true
    var interstitialFailure: Throwable? = null
    var appOpenEnabledState = true
    var suppressNextAppOpen = false
    var consentCached = true
    var preloadStartupCount = 0
    var preloadMainPageCount = 0
    var preloadAfterInterstitialCount = 0
    val interstitialAreaKeys = mutableListOf<String>()
    val appOpenEnabledChanges = mutableListOf<Boolean>()

    private var consentInitCallback: ((Boolean) -> Unit)? = null
    private var consentFinished: (() -> Unit)? = null
    private var openClosed: (() -> Unit)? = null
    private var openLoaded: (() -> Unit)? = null
    private val interstitialClosed = mutableListOf<() -> Unit>()

    override fun isActivityAvailable(): Boolean = activityAvailable

    override fun initConsent(onResult: (Boolean) -> Unit): Boolean {
        consentInitCallback = onResult
        return consentCached
    }

    override fun showSplashConsent(onFinished: () -> Unit) {
        consentFinished = onFinished
    }

    override fun showOpenAd(
        areaKey: String,
        onClosed: () -> Unit,
        onLoaded: () -> Unit,
    ) {
        openClosed = onClosed
        openLoaded = onLoaded
    }

    override fun showInterstitial(areaKey: String, onClosed: () -> Unit): Boolean {
        interstitialFailure?.let { throw it }
        if (!activityAvailable || !interstitialStarted) return false
        interstitialAreaKeys += areaKey
        interstitialClosed += onClosed
        return true
    }

    override fun preloadStartup(context: Context) {
        preloadStartupCount += 1
    }

    override fun preloadMainPage(context: Context) {
        preloadMainPageCount += 1
    }

    override fun preloadAfterInterstitial() {
        preloadAfterInterstitialCount += 1
    }

    override fun isAppOpenEnabled(): Boolean = appOpenEnabledState

    override fun setAppOpenEnabled(enabled: Boolean) {
        appOpenEnabledState = enabled
        appOpenEnabledChanges += enabled
    }

    override fun suppressNextAppOpen(suppress: Boolean) {
        suppressNextAppOpen = suppress
    }

    fun completeInterstitial(index: Int = interstitialClosed.lastIndex) = interstitialClosed[index].invoke()
    fun completeConsentInitialization(success: Boolean = true) = consentInitCallback?.invoke(success) ?: Unit
    fun completeConsent() = consentFinished?.invoke() ?: Unit
    fun loadOpenAd() = openLoaded?.invoke() ?: Unit
    fun closeOpenAd() = openClosed?.invoke() ?: Unit
}

internal class FakeAdRuntimeScheduler : AdRuntimeScheduler {
    private data class Task(
        val runAt: Long,
        val block: () -> Unit,
        var cancelled: Boolean = false,
    )

    private val tasks = mutableListOf<Task>()
    private var now = 0L

    override fun schedule(delayMillis: Long, block: () -> Unit): AdRuntimeCancellation {
        val task = Task(now + delayMillis.coerceAtLeast(0L), block)
        tasks += task
        return AdRuntimeCancellation { task.cancelled = true }
    }

    fun advanceBy(millis: Long) {
        val target = now + millis
        while (true) {
            val task = tasks
                .filter { !it.cancelled && it.runAt <= target }
                .minByOrNull(Task::runAt)
                ?: break
            task.cancelled = true
            now = task.runAt
            task.block()
        }
        now = target
    }
}
