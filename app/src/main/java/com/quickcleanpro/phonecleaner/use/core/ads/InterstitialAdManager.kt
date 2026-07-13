package com.quickcleanpro.phonecleaner.use.core.ads

import com.quickcleanpro.phonecleaner.core.monetization.ads.AdRuntimeState

import android.app.Activity
import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkAdGateway
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdCoordinator
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdOpportunity
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdPolicy

data class InterstitialRequest(
    val scene: AdScene,
    val requestId: String,
)

class InterstitialAdManager(
    activityProvider: () -> Activity?,
    stateProvider: () -> AdRuntimeState = { AdRuntimeState() },
    onInFlightChanged: (Boolean) -> Unit = {},
) {
    val coordinator =
        AdCoordinator(
            gateway = AdvertiseSdkAdGateway(activityProvider),
            policy = AdPolicy(stateProvider),
            onInFlightChanged = onInFlightChanged,
        )

    fun show(
        scene: AdScene,
        requestId: String,
        onContinue: () -> Unit,
    ) {
        run(
            request =
                InterstitialRequest(
                    scene = scene,
                    requestId = requestId,
                ),
            onContinue = onContinue,
        )
    }

    fun run(request: InterstitialRequest, onContinue: () -> Unit) {
        coordinator.show(
            opportunity = AdOpportunity(scene = request.scene, requestId = request.requestId),
            onContinue = onContinue,
        )
    }
}
