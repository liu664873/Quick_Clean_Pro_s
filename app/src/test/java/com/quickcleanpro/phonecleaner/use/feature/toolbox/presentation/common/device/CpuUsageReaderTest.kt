package com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo

import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CpuUsageReaderTest {
    @Test
    fun `parse proc stat includes io wait in idle and all fields in total`() {
        val stat = parseCpuStat("cpu  100 20 30 400 50 6 7 8")

        assertEquals(CpuStat(idle = 450L, total = 621L), stat)
    }

    @Test
    fun `calculate usage uses deltas between samples`() {
        val first = CpuStat(idle = 500L, total = 1_000L)
        val second = CpuStat(idle = 550L, total = 1_200L)

        assertEquals(75, calculateCpuUsagePercent(first, second))
    }

    @Test
    fun `calculate usage rejects inconsistent samples`() {
        assertNull(
            calculateCpuUsagePercent(
                first = CpuStat(idle = 500L, total = 1_000L),
                second = CpuStat(idle = 700L, total = 1_100L),
            ),
        )
    }

    @Test
    fun `parse proc stat rejects unrelated lines`() {
        assertNull(parseCpuStat("intr 1 2 3 4"))
    }
}
