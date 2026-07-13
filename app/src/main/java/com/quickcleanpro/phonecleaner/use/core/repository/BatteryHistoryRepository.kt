package com.quickcleanpro.phonecleaner.use.core.repository

import com.quickcleanpro.phonecleaner.use.core.model.device.BatteryHistorySample
import kotlinx.coroutines.flow.StateFlow

interface BatteryHistoryRepository {
    val samples: StateFlow<List<BatteryHistorySample>>

    fun loadRecent(nowMillis: Long = System.currentTimeMillis()): List<BatteryHistorySample>

    fun append(
        sample: BatteryHistorySample,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<BatteryHistorySample>
}
