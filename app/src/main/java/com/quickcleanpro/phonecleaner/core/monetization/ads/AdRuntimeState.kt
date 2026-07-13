package com.quickcleanpro.phonecleaner.core.monetization.ads

data class AdRuntimeState(
    val permissionFlowActive: Boolean = false,
    val externalActivityReturning: Boolean = false,
    val scanningOrCleaning: Boolean = false,
    val fullScreenCoolingDown: Boolean = false,
    val forceDisableAds: Boolean = false,
)
