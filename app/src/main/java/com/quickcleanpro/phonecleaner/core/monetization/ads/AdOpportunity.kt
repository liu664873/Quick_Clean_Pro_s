package com.quickcleanpro.phonecleaner.core.monetization.ads

import com.quickcleanpro.phonecleaner.use.core.ads.AdPlacementRegistry
import com.quickcleanpro.phonecleaner.use.core.ads.AdScene

data class AdOpportunity(
    val scene: AdScene,
    val requestId: String,
    val areaKey: String? = AdPlacementRegistry.interstitialArea(scene),
)
