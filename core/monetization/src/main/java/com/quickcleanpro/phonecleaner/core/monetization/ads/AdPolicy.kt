package com.quickcleanpro.phonecleaner.core.monetization.ads

import com.quickcleanpro.phonecleaner.use.core.ads.AdScene

class AdPolicy(
    private val stateProvider: () -> AdRuntimeState = { AdRuntimeState() },
) {
    fun canShow(opportunity: AdOpportunity): Boolean {
        if (opportunity.areaKey.isNullOrBlank()) return false

        val state = stateProvider()
        if (state.forceDisableAds) return false
        if (state.permissionFlowActive && opportunity.scene !is AdScene.PermissionRejected) return false
        if (state.externalActivityReturning && opportunity.scene !is AdScene.PermissionRejected) return false
        if (state.scanningOrCleaning) return false
        if (state.fullScreenCoolingDown) return false
        return true
    }
}
