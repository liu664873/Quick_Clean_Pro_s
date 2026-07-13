package com.quickcleanpro.phonecleaner.battery

import com.quickcleanpro.phonecleaner.feature.toolbox.battery.ui.batteryCurrentAxisBounds
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.ui.batteryCurrentChartPoints
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.ui.downsampleBatteryChartPoints
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.BatteryCurrentRange
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.BatteryCurrentSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryChartModelTest {
    @Test
    fun oneMinuteRangeKeepsOrderedSamplesAndUsesAbsoluteCurrent() {
        val samples =
            listOf(
                BatteryCurrentSample(1_000L, -200f),
                BatteryCurrentSample(2_000L, 400f),
            )

        val points = batteryCurrentChartPoints(samples, BatteryCurrentRange.OneMinute, 60_000L)

        assertEquals(listOf(1_000L, 2_000L), points.map { it.timestampMillis })
        assertEquals(listOf(200f, 400f), points.map { it.value })
    }

    @Test
    fun downsamplingKeepsFirstAndLastPoint() {
        val points = (0 until 10).map { index ->
            com.quickcleanpro.phonecleaner.feature.toolbox.battery.ui.BatteryChartPoint(
                timestampMillis = index.toLong(),
                value = index.toFloat(),
            )
        }

        val result = downsampleBatteryChartPoints(points, 4)

        assertEquals(4, result.size)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun emptyCurrentAxisHasStableBounds() {
        val bounds = batteryCurrentAxisBounds(emptyList())

        assertEquals(0f, bounds.min)
        assertEquals(1000f, bounds.max)
        assertTrue(bounds.yLabels.isNotEmpty())
    }
}
