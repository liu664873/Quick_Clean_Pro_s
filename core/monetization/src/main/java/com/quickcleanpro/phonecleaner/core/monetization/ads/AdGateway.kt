package com.quickcleanpro.phonecleaner.core.monetization.ads

interface AdGateway {
    fun showInterstitial(
        opportunity: AdOpportunity,
        onClosed: () -> Unit,
    ): Boolean
}
