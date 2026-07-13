package com.quickcleanpro.phonecleaner.feature.antivirus

enum class VirusScanMode(
    val minDurationMillis: Long,
    val stepCount: Int,
    val displayUpdateIntervalMillis: Long,
) {
    Quick(minDurationMillis = 7_000L, stepCount = 3, displayUpdateIntervalMillis = 80L),
    Deep(minDurationMillis = 15_000L, stepCount = 4, displayUpdateIntervalMillis = 40L);

    fun circleStartThreshold(circleIndex: Int): Float {
        val circleDiameter = 60f
        val connectorWidth = 30f
        val totalWidth = stepCount * circleDiameter + (stepCount - 1) * connectorWidth
        val circleLeft = circleIndex * (circleDiameter + connectorWidth)
        return (circleLeft / totalWidth).coerceIn(0f, 1f)
    }

    fun circleFillThreshold(circleIndex: Int): Float {
        val circleDiameter = 60f
        val connectorWidth = 30f
        val totalWidth = stepCount * circleDiameter + (stepCount - 1) * connectorWidth
        val circleRight = circleIndex * (circleDiameter + connectorWidth) + circleDiameter
        return (circleRight / totalWidth).coerceIn(0f, 1f)
    }
}
