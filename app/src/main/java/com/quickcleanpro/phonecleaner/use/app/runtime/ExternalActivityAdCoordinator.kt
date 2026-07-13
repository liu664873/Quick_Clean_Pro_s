package com.quickcleanpro.phonecleaner.use.app.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkAdapter
import com.quickcleanpro.phonecleaner.use.core.ads.AppOpenAdSuppression
import com.quickcleanpro.phonecleaner.use.core.ads.AppOpenSuppressionReason

class ExternalActivityAdCoordinator {
    var launchPending by mutableStateOf(false)
        private set

    var returning by mutableStateOf(false)
        private set

    var returnGeneration by mutableIntStateOf(0)
        private set

    private var appOpenRelease: (() -> Unit)? = null

    fun markLaunch() {
        launchPending = true
        returning = false
        if (appOpenRelease == null) {
            appOpenRelease = AppOpenAdSuppression.acquire(AppOpenSuppressionReason.ExternalActivity)
        }
        AdvertiseSdkAdapter.suppressNextAppOpen(true)
    }

    fun cancelLaunch() {
        launchPending = false
        returning = false
        restoreAppOpen()
    }

    fun markReturn() {
        if (!launchPending) return
        launchPending = false
        returning = true
        returnGeneration += 1
    }

    fun finishReturnCooldown(generation: Int) {
        if (returnGeneration != generation || launchPending) return
        returning = false
        restoreAppOpen()
    }

    fun dispose() {
        launchPending = false
        returning = false
        restoreAppOpen()
    }

    private fun restoreAppOpen() {
        appOpenRelease?.invoke()
        appOpenRelease = null
        AdvertiseSdkAdapter.suppressNextAppOpen(false)
    }
}
