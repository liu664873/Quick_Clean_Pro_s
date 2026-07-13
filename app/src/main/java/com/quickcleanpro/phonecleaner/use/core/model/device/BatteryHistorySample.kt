package com.quickcleanpro.phonecleaner.use.core.model.device

data class BatteryHistorySample(
    val timestampMillis: Long,
    val currentMa: Float?,
    val temperatureC: Float,
)
