package com.quickcleanpro.phonecleaner.use.feature.toolbox.domain

import com.quickcleanpro.phonecleaner.use.core.model.device.BatteryHistorySample

enum class BatteryHistoryOwner {
    Service,
    BatteryPage,
}

interface BatteryHistorySampler {
    fun start(owner: BatteryHistoryOwner)

    fun stop(owner: BatteryHistoryOwner)

    fun sampleOnce(force: Boolean = false): BatteryHistorySample?
}
