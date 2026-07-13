package com.quickcleanpro.phonecleaner.use.feature.toolbox.battery.data.service

import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistoryOwner
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistorySampler

internal class BatterySamplingCoordinator(
    private val sampler: BatteryHistorySampler,
) {
    fun start() {
        runCatching { sampler.start(BatteryHistoryOwner.Service) }
    }

    fun stop() {
        runCatching { sampler.stop(BatteryHistoryOwner.Service) }
    }
}
