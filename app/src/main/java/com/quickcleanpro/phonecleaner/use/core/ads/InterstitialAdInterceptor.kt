package com.quickcleanpro.phonecleaner.use.core.ads

import com.quickcleanpro.phonecleaner.core.monetization.ads.AdCoordinator
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdGateway
import com.quickcleanpro.phonecleaner.core.monetization.ads.AdOpportunity

interface InterstitialAdInterceptor {
    fun interceptRouteEntry(
        fromRoute: String?,
        targetRoute: String?,
        onContinue: () -> Unit,
    )

    fun interceptFeatureOperation(
        scene: AdScene?,
        requestId: String,
        onContinue: () -> Unit,
    )
}

class DefaultInterstitialAdInterceptor(
    private val coordinator: AdCoordinator,
    private val policy: AdNavigationPolicy = AdNavigationPolicy(),
) : InterstitialAdInterceptor {
    constructor(
        showInterstitial: (AdScene, String, () -> Unit) -> Unit,
        policy: AdNavigationPolicy = AdNavigationPolicy(),
    ) : this(
        coordinator = AdCoordinator(LegacyInterstitialAdGateway(showInterstitial)),
        policy = policy,
    )

    override fun interceptRouteEntry(
        fromRoute: String?,
        targetRoute: String?,
        onContinue: () -> Unit,
    ) {
        val decision =
            policy.entryAdDecision(
                fromRoute = fromRoute,
                targetRoute = targetRoute,
            )
        if (decision == null) {
            onContinue()
            return
        }

        val scene = AdScene.EnterFeature(decision.feature, decision.route)
        coordinator.show(
            opportunity =
                AdOpportunity(
                    scene = scene,
                    requestId = "route_enter_${decision.feature.name}_${decision.route}",
                ),
            onContinue = onContinue,
        )
    }

    override fun interceptFeatureOperation(
        scene: AdScene?,
        requestId: String,
        onContinue: () -> Unit,
    ) {
        if (scene == null) {
            onContinue()
            return
        }
        coordinator.show(
            opportunity = AdOpportunity(scene = scene, requestId = requestId),
            onContinue = onContinue,
        )
    }
}

private class LegacyInterstitialAdGateway(
    private val showInterstitial: (AdScene, String, () -> Unit) -> Unit,
) : AdGateway {
    override fun showInterstitial(
        opportunity: AdOpportunity,
        onClosed: () -> Unit,
    ): Boolean {
        showInterstitial(opportunity.scene, opportunity.requestId, onClosed)
        return true
    }
}
