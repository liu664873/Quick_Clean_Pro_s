package com.quickcleanpro.phonecleaner.use.core.ads

import android.content.Context
import android.util.Log
import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkAdapter

object AdvertisePreloader {
    private const val TAG = "QuickCleanAdPreload"

    fun preloadStartupAds(context: Context) {
        runPreload("startup") { AdvertiseSdkAdapter.preloadStartup(context) }
    }

    fun preloadMainPageAds(context: Context) {
        runPreload("main_page") { AdvertiseSdkAdapter.preloadMainPage(context) }
    }

    fun preloadAfterPlayFinish(context: Context) {
        runPreload("play_finish") { AdvertiseSdkAdapter.preloadAfterPlayFinish(context) }
    }

    private inline fun runPreload(label: String, block: () -> Unit) {
        runCatching(block)
            .onFailure { throwable -> Log.w(TAG, "preload failed for $label", throwable) }
    }
}
