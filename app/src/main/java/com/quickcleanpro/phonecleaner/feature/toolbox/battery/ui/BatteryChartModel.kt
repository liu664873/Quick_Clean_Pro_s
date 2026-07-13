package com.quickcleanpro.phonecleaner.feature.toolbox.battery.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.BatteryCurrentRange
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.BatteryCurrentSample
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

internal data class BatteryChartPoint(
    val timestampMillis: Long,
    val value: Float,
)

internal data class BatteryChartAxisBounds(
    val min: Float,
    val max: Float,
    val yLabels: List<String>,
) {
    val range: Float
        get() = (max - min).takeIf { abs(it) > 0.01f } ?: 1f
}

internal fun batteryCurrentAxisLabels(range: BatteryCurrentRange): List<String> =
    when (range) {
        BatteryCurrentRange.OneMinute -> listOf("60s", "50s", "40s", "30s", "20s", "10s", "0s")
        BatteryCurrentRange.OneHour -> listOf("60m", "50m", "40m", "30m", "20m", "10m", "0m")
        BatteryCurrentRange.TwentyFourHours -> listOf("24h", "20h", "16h", "12h", "8h", "4h", "0h")
    }

internal fun batteryCurrentChartMaxPoints(range: BatteryCurrentRange): Int? =
    when (range) {
        BatteryCurrentRange.OneMinute -> null
        BatteryCurrentRange.OneHour -> ONE_HOUR_BUCKET_COUNT
        BatteryCurrentRange.TwentyFourHours -> TWENTY_FOUR_HOUR_BUCKET_COUNT
    }

internal fun batteryCurrentChartPoints(
    samples: List<BatteryCurrentSample>,
    selectedRange: BatteryCurrentRange,
    windowEndMillis: Long,
): List<BatteryChartPoint> {
    if (windowEndMillis <= 0L) return emptyList()
    val windowStartMillis = windowEndMillis - selectedRange.durationMillis
    val visibleSamples =
        samples
            .asSequence()
            .filter { it.timestampMillis in windowStartMillis..windowEndMillis }
            .sortedBy { it.timestampMillis }
            .toList()

    val bucketMillis = batteryCurrentChartBucketMillis(selectedRange)
    if (bucketMillis == null) {
        return visibleSamples.map { BatteryChartPoint(it.timestampMillis, abs(it.currentMa)) }
    }

    val bucketCount = batteryCurrentChartBucketCount(selectedRange)
    return visibleSamples
        .groupBy { sample ->
            val elapsedMillis = sample.timestampMillis - windowStartMillis
            if (elapsedMillis <= 0L) {
                0
            } else {
                ((elapsedMillis - 1L) / bucketMillis).toInt().coerceIn(0, bucketCount - 1)
            }
        }.toSortedMap()
        .map { (bucketIndex, bucketSamples) ->
            BatteryChartPoint(
                timestampMillis = windowStartMillis + (bucketIndex + 1L) * bucketMillis,
                value = bucketSamples.map { abs(it.currentMa) }.average().toFloat(),
            )
        }
}

internal fun batteryCurrentChartWindowEndMillis(
    samples: List<BatteryCurrentSample>,
    selectedRange: BatteryCurrentRange,
    latestSampleTimestampMillis: Long,
): Long {
    val latestTimestamp =
        latestSampleTimestampMillis
            .takeIf { it > 0L }
            ?: samples.maxOfOrNull { it.timestampMillis }
            ?: return 0L
    val bucketMillis = batteryCurrentChartBucketMillis(selectedRange) ?: return latestTimestamp
    return alignBatteryChartBucket(latestTimestamp, bucketMillis)
}

internal fun batteryOneMinuteWindowEnd(
    points: List<BatteryChartPoint>,
    latestSampleTimestampMillis: Long,
): Long =
    latestSampleTimestampMillis
        .takeIf { it > 0L }
        ?: points.maxOfOrNull { it.timestampMillis }
        ?: 0L

internal fun batteryTemperatureAxisBounds(unit: String): BatteryChartAxisBounds =
    if (unit.equals("F", ignoreCase = true)) {
        BatteryChartAxisBounds(32f, 122f, listOf("122", "104", "86", "68", "50", "32"))
    } else {
        BatteryChartAxisBounds(0f, 50f, listOf("50", "40", "30", "20", "10", "0"))
    }

internal fun batteryCurrentAxisBounds(values: List<Float>): BatteryChartAxisBounds {
    if (values.isEmpty()) {
        return BatteryChartAxisBounds(0f, 1000f, listOf("1000", "800", "600", "400", "200", "0"))
    }
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 0f
    val padding = ((maxValue - minValue) * 0.16f).takeIf { it > 1f } ?: 120f
    val min = floor(max(0f, minValue - padding) / 120f) * 120f
    val max = ceil((maxValue + padding) / 120f) * 120f
    val resolvedMax = if (max <= min) min + 600f else max
    val step = (resolvedMax - min) / 5f
    return BatteryChartAxisBounds(
        min = min,
        max = resolvedMax,
        yLabels = (0..5).map { index -> (resolvedMax - step * index).toInt().toString() },
    )
}

internal fun downsampleBatteryChartPoints(
    points: List<BatteryChartPoint>,
    maxPoints: Int,
): List<BatteryChartPoint> {
    if (points.size <= maxPoints) return points
    if (maxPoints <= 2) return listOf(points.first(), points.last())

    val middleMaxPoints = maxPoints - 2
    val middle = points.subList(1, points.lastIndex)
    if (middle.isEmpty()) return listOf(points.first(), points.last())

    val sampledMiddle =
        (0 until middleMaxPoints).mapNotNull { bucketIndex ->
            val start = floor(bucketIndex * middle.size / middleMaxPoints.toFloat()).toInt()
            val end =
                floor((bucketIndex + 1) * middle.size / middleMaxPoints.toFloat())
                    .toInt()
                    .coerceAtLeast(start + 1)
                    .coerceAtMost(middle.size)
            val bucket = middle.subList(start, end).takeIf(List<BatteryChartPoint>::isNotEmpty)
                ?: return@mapNotNull null
            bucket.maxByOrNull { abs(it.value - bucket.first().value) } ?: bucket.first()
        }
    return listOf(points.first()) + sampledMiddle.take(middleMaxPoints) + points.last()
}

private fun batteryCurrentChartBucketMillis(range: BatteryCurrentRange): Long? =
    when (range) {
        BatteryCurrentRange.OneMinute -> null
        BatteryCurrentRange.OneHour -> ONE_HOUR_BUCKET_MILLIS
        BatteryCurrentRange.TwentyFourHours -> TWENTY_FOUR_HOUR_BUCKET_MILLIS
    }

private fun batteryCurrentChartBucketCount(range: BatteryCurrentRange): Int =
    when (range) {
        BatteryCurrentRange.OneMinute -> 0
        BatteryCurrentRange.OneHour -> ONE_HOUR_BUCKET_COUNT
        BatteryCurrentRange.TwentyFourHours -> TWENTY_FOUR_HOUR_BUCKET_COUNT
    }

private fun alignBatteryChartBucket(timestampMillis: Long, bucketMillis: Long): Long {
    if (timestampMillis <= 0L || bucketMillis <= 0L) return timestampMillis
    val remainder = timestampMillis % bucketMillis
    return if (remainder == 0L) timestampMillis else timestampMillis + bucketMillis - remainder
}

private const val ONE_HOUR_BUCKET_COUNT = 30
private const val TWENTY_FOUR_HOUR_BUCKET_COUNT = 24
private const val ONE_HOUR_BUCKET_MILLIS = 2L * 60L * 1000L
private const val TWENTY_FOUR_HOUR_BUCKET_MILLIS = 60L * 60L * 1000L
