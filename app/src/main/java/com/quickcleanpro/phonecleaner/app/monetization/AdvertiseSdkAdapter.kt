package com.quickcleanpro.phonecleaner.app.monetization

import android.app.Activity
import android.app.Application
import android.content.Context
import com.pdffox.adv.AdvertiseSdk
import com.pdffox.adv.notification.NotificationManager as AdvertiseNotificationManager

object AdvertiseSdkAdapter {
    suspend fun initialize(
        context: Application,
        isTest: Boolean,
    ) {
        AdvertiseSdk.init(
            context = context,
            isTest = isTest,
            sdkConfig = AdvertiseConfigFactory.create(context),
        )
    }

    fun initConsent(
        activity: Activity,
        onResult: (Boolean) -> Unit,
    ): Boolean = AdvertiseSdk.initConsent(activity, onResult)

    fun showSplashConsent(
        activity: Activity,
        onFinished: () -> Unit,
    ) {
        AdvertiseSdk.showSplashConsent(activity, onFinished)
    }

    fun showOpenAd(
        activity: Activity,
        areaKey: String,
        onClosed: () -> Unit,
        onLoaded: () -> Unit,
    ) {
        AdvertiseSdk.showOpenAd(
            activity = activity,
            areaKey = areaKey,
            onCloseListener = AdvertiseSdk.OpenAdCloseListener(onClosed),
            onLoadedListener = AdvertiseSdk.OpenAdLoadedListener(onLoaded),
            onPaidListener = AdvertiseSdk.OpenAdPaidListener { _ -> },
        )
    }

    fun showInterstitial(
        activity: Activity,
        areaKey: String,
        onClosed: () -> Unit,
    ) {
        AdvertiseSdk.showInterstitialAd(activity, areaKey, onClosed)
    }

    fun preloadStartup(context: Context) {
        preload(context, AdvertiseSdk.LOAD_TIME_OPEN_APP, includeOpen = true)
    }

    fun preloadMainPage(context: Context) {
        preload(context, AdvertiseSdk.LOAD_TIME_ENTER_FEATURE, includeOpen = false)
    }

    fun preloadAfterPlayFinish(context: Context) {
        preload(context, AdvertiseSdk.LOAD_TIME_PLAY_FINISH, includeOpen = false)
    }

    fun isPrivacyOptionsRequired(): Boolean = AdvertiseSdk.isPrivacyOptionsRequired

    fun showPrivacyOptions(activity: Activity) {
        AdvertiseSdk.showPrivacyOptions(activity)
    }

    fun ensurePersistentNotificationServiceRunning(context: Context) {
        AdvertiseSdk.ensurePersistentNotificationServiceRunning(context)
    }

    fun updateNotificationContent(content: String) {
        AdvertiseNotificationManager.updateNotificationContent(content)
    }

    fun isAppOpenEnabled(): Boolean = AdvertiseSdk.isAppOpenAdEnabled

    fun setAppOpenEnabled(enabled: Boolean) {
        AdvertiseSdk.isAppOpenAdEnabled = enabled
    }

    fun suppressNextAppOpen(suppress: Boolean) {
        AdvertiseSdk.suppressNextAppOpenAd = suppress
    }

    private fun preload(
        context: Context,
        loadTimeKey: String,
        includeOpen: Boolean,
    ) {
        val appContext = context.applicationContext
        if (includeOpen && AdvertiseSdk.canPreloadOpen(loadTimeKey)) {
            AdvertiseSdk.preloadOpen(appContext, loadTimeKey)
        }
        if (AdvertiseSdk.canPreloadInterstitial(loadTimeKey)) {
            AdvertiseSdk.preloadInterstitial(appContext, loadTimeKey)
        }
    }
}
