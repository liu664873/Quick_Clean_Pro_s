package com.quickcleanpro.phonecleaner.feature.toolbox.battery.model

data class BatteryHistorySample(
    val timestampMillis: Long,
    val currentMa: Float?,
    val temperatureC: Float,
)
