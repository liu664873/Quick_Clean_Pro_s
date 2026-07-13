package com.quickcleanpro.phonecleaner.feature.toolbox.shared.device

import java.util.Locale

internal data class ThermalZoneReading(
    val type: String,
    val rawTemperature: String,
)

internal object CpuTemperatureResolver {
    fun resolveThermalZones(readings: List<ThermalZoneReading>): Float? {
        val candidates =
            readings
                .asSequence()
                .mapNotNull { reading ->
                    val normalizedType = reading.type.lowercase(Locale.US)
                    val priority = normalizedType.cpuThermalPriority() ?: return@mapNotNull null
                    val temperature =
                        normalizeTemperatureC(reading.rawTemperature)
                            ?: return@mapNotNull null
                    CpuTemperatureCandidate(priority, temperature)
                }.toList()

        return candidates
            .filter { it.priority == CPU_THERMAL_PRIMARY_PRIORITY }
            .maxOfOrNull { it.temperatureC }
            ?: candidates
                .filter { it.priority == CPU_THERMAL_FALLBACK_PRIORITY }
                .maxOfOrNull { it.temperatureC }
    }

    fun resolveRawTemperatures(rawTemperatures: Iterable<Float>): Float? =
        rawTemperatures
            .mapNotNull(::normalizeTemperatureC)
            .maxOrNull()

    fun normalizeTemperatureC(rawTemperature: String): Float? =
        rawTemperature.trim().toFloatOrNull()?.let(::normalizeTemperatureC)

    fun normalizeTemperatureC(rawTemperature: Float): Float? {
        if (!rawTemperature.isFinite()) return null
        val normalized =
            when {
                rawTemperature > 1000f -> rawTemperature / 1000f
                rawTemperature > 150f -> rawTemperature / 10f
                else -> rawTemperature
            }
        return normalized.takeIf {
            it.isFinite() &&
                it > MIN_REASONABLE_ACTIVE_TEMPERATURE_C &&
                it in MIN_REASONABLE_CPU_TEMPERATURE_C..MAX_REASONABLE_CPU_TEMPERATURE_C
        }
    }

    private fun String.cpuThermalPriority(): Int? {
        if (CPU_THERMAL_EXCLUDED_KEYWORDS.any { contains(it) }) return null
        return when {
            CPU_THERMAL_PRIMARY_KEYWORDS.any { contains(it) } -> CPU_THERMAL_PRIMARY_PRIORITY
            CPU_THERMAL_FALLBACK_KEYWORDS.any { contains(it) } -> CPU_THERMAL_FALLBACK_PRIORITY
            else -> null
        }
    }

    private data class CpuTemperatureCandidate(
        val priority: Int,
        val temperatureC: Float,
    )

    private const val CPU_THERMAL_PRIMARY_PRIORITY = 0
    private const val CPU_THERMAL_FALLBACK_PRIORITY = 1
    private const val MIN_REASONABLE_CPU_TEMPERATURE_C = -20f
    private const val MAX_REASONABLE_CPU_TEMPERATURE_C = 125f
    private const val MIN_REASONABLE_ACTIVE_TEMPERATURE_C = 1f

    private val CPU_THERMAL_PRIMARY_KEYWORDS =
        listOf(
            "cpu",
            "cpuss",
            "cluster",
            "big",
            "little",
            "gold",
            "silver",
            "prime",
            "core",
            "mtktscpu",
        )

    private val CPU_THERMAL_FALLBACK_KEYWORDS =
        listOf(
            "soc",
            "ap",
            "package",
            "pkg",
            "tsens",
            "aoss",
            "sys-therm",
            "tz_shell",
        )

    private val CPU_THERMAL_EXCLUDED_KEYWORDS =
        listOf(
            "battery",
            "bat",
            "vbat",
            "charger",
            "chg",
            "usb",
            "skin",
            "gpu",
            "camera",
            "video",
            "modem",
            "wifi",
            "wlan",
            "ddr",
            "display",
        )
}
