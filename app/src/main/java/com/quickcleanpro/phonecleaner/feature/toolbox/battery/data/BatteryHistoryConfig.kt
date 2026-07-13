package com.quickcleanpro.phonecleaner.feature.toolbox.battery.data

object BatteryHistoryConfig {
    const val SAMPLE_INTERVAL_MILLIS = 2_000L
    const val WINDOW_MILLIS = 24L * 60L * 60L * 1000L
    const val MAX_SAMPLES = (WINDOW_MILLIS / SAMPLE_INTERVAL_MILLIS).toInt() + 1
    const val FUTURE_TOLERANCE_MILLIS = 60_000L
}
