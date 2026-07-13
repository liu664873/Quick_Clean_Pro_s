package com.quickcleanpro.phonecleaner.feature.toolbox.shared.device

import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.BatteryStatusInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.DeviceHardwareInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.BatteryInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.MemoryInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.StorageInfo

interface DeviceInfoRepository {
    fun batteryInfo(): BatteryInfo

    fun batteryStatusInfo(): BatteryStatusInfo

    fun memoryInfo(): MemoryInfo

    fun internalStorageInfo(): StorageInfo

    fun hardwareInfo(): DeviceHardwareInfo

    fun cpuTemperatureC(): Float?

    fun batteryCurrentNowMa(): Float?

    fun batteryCurrentAverageMa(): Float?
}
