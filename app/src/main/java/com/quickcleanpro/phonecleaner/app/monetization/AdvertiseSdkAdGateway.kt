package com.quickcleanpro.phonecleaner.app.monetization

import android.app.Activity
import android.util.Log
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdGateway
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdOpportunity
import com.quickcleanpro.phonecleaner.use.core.ads.AdvertisePreloader
import java.util.concurrent.atomic.AtomicBoolean

class AdvertiseSdkAdGateway(
    private val activityProvider: () -> Activity?,
) : AdGateway {
    override fun showInterstitial(
        opportunity: AdOpportunity,
        onClosed: () -> Unit,
    ): Boolean {
        val areaKey = opportunity.areaKey?.takeIf(String::isNotBlank) ?: return false
        val activity = activityProvider()?.takeUnless { it.isUnavailable() } ?: return false
        val closed =
            once {
                Log.d(
                    TAG,
                    "interstitial closed scene=${opportunity.scene} areaKey=$areaKey requestId=${opportunity.requestId}",
                )
                if (!activity.isUnavailable()) {
                    AdvertisePreloader.preloadAfterPlayFinish(activity)
                }
                onClosed()
            }

        return try {
            AdvertiseSdkAdapter.showInterstitial(activity, areaKey, closed)
            true
        } catch (throwable: Throwable) {
            Log.w(TAG, "show interstitial failed for $areaKey", throwable)
            false
        }
    }

    private fun Activity.isUnavailable(): Boolean = isFinishing || isDestroyed

    private companion object {
        const val TAG = "QuickCleanInterAd"
    }
}

private fun once(block: () -> Unit): () -> Unit {
    val called = AtomicBoolean(false)
    return {
        if (called.compareAndSet(false, true)) block()
    }
}
