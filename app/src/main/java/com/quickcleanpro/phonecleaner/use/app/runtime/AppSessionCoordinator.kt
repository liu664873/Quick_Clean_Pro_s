package com.quickcleanpro.phonecleaner.use.app.runtime

import androidx.compose.runtime.mutableStateMapOf

enum class RuntimeBusyReason {
    Permission,
    FeatureOperation,
    Interstitial,
}

class AppSessionCoordinator {
    private val activeReasons = mutableStateMapOf<RuntimeBusyReason, Boolean>()

    fun set(reason: RuntimeBusyReason, active: Boolean) {
        if (active) activeReasons[reason] = true else activeReasons.remove(reason)
    }

    fun isActive(reason: RuntimeBusyReason): Boolean = activeReasons[reason] == true
}
