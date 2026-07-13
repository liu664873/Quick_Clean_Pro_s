package com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.device

import androidx.lifecycle.ViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DeviceViewModelCompositionTest {
    @Test
    fun `battery view model composes shared controller instead of inheriting device view model`() {
        assertFalse(DeviceInfoViewModel::class.java.isAssignableFrom(BatteryInfoViewModel::class.java))
        assertEquals(ViewModel::class.java, BatteryInfoViewModel::class.java.superclass)
    }
}
