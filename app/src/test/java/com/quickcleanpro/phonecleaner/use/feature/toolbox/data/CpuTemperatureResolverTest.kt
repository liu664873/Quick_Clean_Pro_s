package com.quickcleanpro.phonecleaner.feature.toolbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CpuTemperatureResolverTest {
    @Test
    fun cpuThermalNodesWinOverExcludedNodes() {
        val temperature =
            CpuTemperatureResolver.resolveThermalZones(
                listOf(
                    ThermalZoneReading(type = "battery", rawTemperature = "47000"),
                    ThermalZoneReading(type = "skin-therm", rawTemperature = "45000"),
                    ThermalZoneReading(type = "gpu-thermal", rawTemperature = "50000"),
                    ThermalZoneReading(type = "cpu-little", rawTemperature = "44000"),
                    ThermalZoneReading(type = "cpu-big", rawTemperature = "52000"),
                ),
            )

        assertEquals(52f, temperature ?: -1f, 0.001f)
    }

    @Test
    fun socAndTsensNodesFallbackWhenCpuNodesAreAbsent() {
        val temperature =
            CpuTemperatureResolver.resolveThermalZones(
                listOf(
                    ThermalZoneReading(type = "battery", rawTemperature = "47000"),
                    ThermalZoneReading(type = "soc_thermal", rawTemperature = "41000"),
                    ThermalZoneReading(type = "tsens_tz_sensor4", rawTemperature = "43000"),
                    ThermalZoneReading(type = "package-id0", rawTemperature = "420"),
                ),
            )

        assertEquals(43f, temperature ?: -1f, 0.001f)
    }

    @Test
    fun excludedOnlyNodesReturnNull() {
        val temperature =
            CpuTemperatureResolver.resolveThermalZones(
                listOf(
                    ThermalZoneReading(type = "battery", rawTemperature = "47000"),
                    ThermalZoneReading(type = "skin-therm", rawTemperature = "45000"),
                    ThermalZoneReading(type = "gpu-thermal", rawTemperature = "50000"),
                    ThermalZoneReading(type = "wifi-therm", rawTemperature = "42000"),
                ),
            )

        assertNull(temperature)
    }

    @Test
    fun rawTemperatureUnitsAreNormalized() {
        assertEquals(42f, CpuTemperatureResolver.normalizeTemperatureC("42000") ?: -1f, 0.001f)
        assertEquals(42f, CpuTemperatureResolver.normalizeTemperatureC("420") ?: -1f, 0.001f)
        assertEquals(42f, CpuTemperatureResolver.normalizeTemperatureC("42.0") ?: -1f, 0.001f)
    }

    @Test
    fun invalidTemperaturesAreFiltered() {
        assertNull(CpuTemperatureResolver.normalizeTemperatureC("not-a-number"))
        assertNull(CpuTemperatureResolver.normalizeTemperatureC(Float.NaN))
        assertNull(CpuTemperatureResolver.normalizeTemperatureC(Float.POSITIVE_INFINITY))
        assertNull(CpuTemperatureResolver.normalizeTemperatureC("0"))
        assertNull(CpuTemperatureResolver.normalizeTemperatureC("126000"))
    }

    @Test
    fun rawHardwareTemperaturesUseHighestValidValue() {
        val temperature =
            CpuTemperatureResolver.resolveRawTemperatures(
                listOf(
                    Float.NaN,
                    -Float.MAX_VALUE,
                    36.5f,
                    46f,
                    126f,
                ),
            )

        assertEquals(46f, temperature ?: -1f, 0.001f)
    }
}
