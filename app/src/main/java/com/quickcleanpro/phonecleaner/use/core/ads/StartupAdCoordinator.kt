package com.quickcleanpro.phonecleaner.use.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkAdapter
import java.util.concurrent.atomic.AtomicBoolean

object StartupAdCoordinator {
    fun runColdStart(
        activity: Activity?,
        context: Context,
        onFinished: () -> Unit,
        onOpenAdShowing: () -> Unit = {},
        onOpenAdFinished: () -> Unit = {},
    ) {
        val releaseAppOpenAdSuppression = AppOpenAdSuppression.acquire(AppOpenSuppressionReason.Startup)
        val finish =
            once {
                releaseAppOpenAdSuppression()
                onFinished()
            }
        showStartupConsent(activity) {
            AdvertisePreloader.preloadStartupAds(context)
            showOpenAdAndWaitForSdk(
                activity = activity,
                areaKey = AdAreaKeys.Open.OPEN_PAGE,
                label = "cold start",
                onOpenAdShowing = onOpenAdShowing,
                onOpenAdFinished = onOpenAdFinished,
                onContinue = finish,
            )
        }
    }

    fun run(
        activity: Activity?,
        context: Context,
        onFinished: () -> Unit,
    ) = runColdStart(activity, context, onFinished)

    private fun showStartupConsent(
        activity: Activity?,
        onContinue: () -> Unit,
    ) {
        if (activity == null || activity.isUnavailable()) {
            Log.w(TAG, "skip startup consent: activity unavailable")
            onContinue()
            return
        }

        val handler = Handler(Looper.getMainLooper())
        val continueOnce =
            once {
                handler.removeCallbacksAndMessages(null)
                onContinue()
            }
        var continued = false
        val finish =
            once {
                continued = true
                continueOnce()
            }
        val showConsent =
            once {
                handler.removeCallbacksAndMessages(null)
                if (continued || activity.isUnavailable()) {
                    Log.w(TAG, "skip startup consent show: continued=$continued unavailable=${activity.isUnavailable()}")
                    finish()
                    return@once
                }
                runCatching {
                    Log.d(TAG, "show startup consent")
                    handler.postDelayed(
                        {
                            Log.w(TAG, "startup consent show timeout")
                            finish()
                        },
                        STARTUP_CONSENT_SHOW_TIMEOUT_MS,
                    )
                    AdvertiseSdkAdapter.showSplashConsent(activity) {
                        Log.d(TAG, "startup consent finished")
                        finish()
                    }
                }.onFailure { throwable ->
                    Log.w(TAG, "show startup consent failed", throwable)
                    finish()
                }
            }

        handler.postDelayed({ showConsent() }, STARTUP_CONSENT_INIT_TIMEOUT_MS)
        runCatching {
            Log.d(TAG, "init startup consent")
            val hasCachedConsentState =
                AdvertiseSdkAdapter.initConsent(activity) { success ->
                    Log.d(TAG, "startup consent init callback success=$success")
                    showConsent()
                }
            if (hasCachedConsentState) {
                Log.d(TAG, "startup consent has cached state")
                showConsent()
            }
        }.onFailure { throwable ->
            Log.w(TAG, "init startup consent failed", throwable)
            finish()
        }
    }

    private fun showOpenAdAndWaitForSdk(
        activity: Activity?,
        areaKey: String,
        label: String,
        onOpenAdShowing: () -> Unit,
        onOpenAdFinished: () -> Unit,
        onContinue: () -> Unit,
    ) {
        val finishOpenAd = once(onOpenAdFinished)
        val showOpenAd = once(onOpenAdShowing)
        val handler = Handler(Looper.getMainLooper())
        val loaded = AtomicBoolean(false)
        val completed = AtomicBoolean(false)
        if (activity == null || activity.isUnavailable()) {
            Log.w(TAG, "skip $label open ad: activity unavailable")
            finishOpenAd()
            onContinue()
            return
        }

        fun continueOnce() {
            if (completed.compareAndSet(false, true)) {
                handler.removeCallbacksAndMessages(null)
                finishOpenAd()
                onContinue()
            }
        }

        val startTimeout =
            Runnable {
                if (!loaded.get()) {
                    Log.w(TAG, "$label open ad start timeout")
                    continueOnce()
                }
            }
        val totalTimeout =
            Runnable {
                Log.w(TAG, "$label open ad total timeout")
                continueOnce()
            }
        fun continueAfterSdkClose() {
            val delayMillis = if (loaded.get()) OPEN_AD_CLOSE_SETTLE_MS else 0L
            if (delayMillis > 0L) {
                handler.postDelayed({ continueOnce() }, delayMillis)
            } else {
                continueOnce()
            }
        }

        runCatching {
            handler.postDelayed(startTimeout, OPEN_AD_START_TIMEOUT_MS)
            AdvertiseSdkAdapter.showOpenAd(
                activity = activity,
                areaKey = areaKey,
                onClosed = {
                        Log.d(TAG, "$label open ad closed")
                        continueAfterSdkClose()
                    },
                onLoaded = {
                        if (!completed.get()) {
                            loaded.set(true)
                            handler.removeCallbacks(startTimeout)
                            handler.postDelayed(totalTimeout, OPEN_AD_TOTAL_TIMEOUT_MS)
                            showOpenAd()
                            Log.d(TAG, "$label open ad loaded")
                        }
                    },
            )
        }.onFailure { throwable ->
            Log.w(TAG, "show $label open ad failed", throwable)
            continueOnce()
        }
    }

    private fun Activity.isUnavailable(): Boolean = isFinishing || isDestroyed

    private const val TAG = "QuickCleanStartup"
    private const val STARTUP_CONSENT_INIT_TIMEOUT_MS = 6_500L
    private const val STARTUP_CONSENT_SHOW_TIMEOUT_MS = 6_500L
    private const val OPEN_AD_START_TIMEOUT_MS = 6_500L
    private const val OPEN_AD_TOTAL_TIMEOUT_MS = 30_000L
    private const val OPEN_AD_CLOSE_SETTLE_MS = 800L
}
