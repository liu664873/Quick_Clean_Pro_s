package com.quickcleanpro.phonecleaner.feature.toolbox.shared.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.HardwarePropertiesManager
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.DeviceInfoDataSource
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.StorageDataSource
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.BatteryStatusInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.DeviceCpuInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.DeviceHardwareInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.DeviceSensorInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.BatteryInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.MemoryInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.StorageInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.DeviceInfoRepository
import java.io.File
import java.util.Locale

class DeviceInfoRepositoryImpl(
    context: Context,
) : DeviceInfoRepository {
    private val appContext = context.applicationContext

    override fun batteryInfo(): BatteryInfo = DeviceInfoDataSource.getBatteryInfo(appContext)


    override fun batteryStatusInfo(): BatteryStatusInfo {
        val status = readBatteryStatus()
        return BatteryStatusInfo(
            statusText = status.toBatteryStatusText(),
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING,
        )
    }


    override fun memoryInfo(): MemoryInfo = DeviceInfoDataSource.getMemoryInfo(appContext)


    override fun internalStorageInfo(): StorageInfo = StorageDataSource.getInternalStorageInfo()

    override fun hardwareInfo(): DeviceHardwareInfo {
        val metrics = appContext.resources.displayMetrics
        val packageManager = appContext.packageManager
        return DeviceHardwareInfo(
            model = Build.MODEL.takeIf { it.isNotBlank() } ?: UNKNOWN,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            screenSize = "${metrics.widthPixels}x${metrics.heightPixels}",
            screenDensity = "${metrics.densityDpi} DPI",
            multiTouchSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH),
            sensors = readSensorInfo(),
            cpu =
                DeviceCpuInfo(
                    hardware = Build.HARDWARE.takeIf { it.isNotBlank() } ?: UNKNOWN,
                    model =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL.isNotBlank()) {
                            Build.SOC_MODEL
                        } else {
                            UNKNOWN
                        },
                    cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
                    maxFrequency = readCpuMaxFrequency(),
                ),
        )
    }

    override fun batteryCurrentNowMa(): Float? = readBatteryCurrentMa(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

    override fun batteryCurrentAverageMa(): Float? = readBatteryCurrentMa(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)

    override fun cpuTemperatureC(): Float? =
        readCpuTemperatureFromHardwareProperties()
            ?: readCpuTemperatureFromThermalZones()

    private fun readBatteryStatus(): Int? =
        try {
            val intent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    appContext.registerReceiver(
                        null,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                        Context.RECEIVER_NOT_EXPORTED,
                    )
                } else {
                    appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                }
            intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        } catch (_: Exception) {
            null
        }

    private fun readSensorInfo(): DeviceSensorInfo {
        val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

        fun supported(type: Int): Boolean = sensorManager?.getDefaultSensor(type) != null
        return DeviceSensorInfo(
            accelerometer = supported(Sensor.TYPE_ACCELEROMETER),
            magneticField = supported(Sensor.TYPE_MAGNETIC_FIELD),
            orientation = supported(Sensor.TYPE_ORIENTATION),
            gyroscope = supported(Sensor.TYPE_GYROSCOPE),
            light = supported(Sensor.TYPE_LIGHT),
            proximity = supported(Sensor.TYPE_PROXIMITY),
            ambientTemperature = supported(Sensor.TYPE_AMBIENT_TEMPERATURE),
        )
    }

    private fun readBatteryCurrentMa(property: Int): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null
        return try {
            val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val microAmps = batteryManager.getIntProperty(property)
            if (microAmps == Int.MIN_VALUE) null else microAmps / 1000f
        } catch (_: Exception) {
            null
        }
    }

    private fun readCpuMaxFrequency(): String {
        return try {
            CPU_FREQUENCY_PATHS.forEach { path ->
                val freq = File(path).readText().trim().toLongOrNull()
                if (freq != null && freq > 0) {
                    val ghz = freq / 1_000_000f
                    return if (ghz >= 1f) {
                        String.format(Locale.US, "%.1f GHz", ghz)
                    } else {
                        "${freq / 1000} MHz"
                    }
                }
            }
            UNKNOWN
        } catch (_: Exception) {
            UNKNOWN
        }
    }

    private fun readCpuTemperatureFromHardwareProperties(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return runCatching {
            val hardwarePropertiesManager =
                appContext.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
                    ?: return@runCatching null
            CpuTemperatureResolver.resolveRawTemperatures(
                hardwarePropertiesManager
                    .getDeviceTemperatures(
                        HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                        HardwarePropertiesManager.TEMPERATURE_CURRENT,
                    ).asIterable(),
            )
        }.getOrNull()
    }

    private fun readCpuTemperatureFromThermalZones(): Float? =
        CpuTemperatureResolver.resolveThermalZones(
            thermalZoneDirectories()
                .mapNotNull { zone ->
                    val type = File(zone, THERMAL_TYPE_FILE).readTrimmedText()
                    val rawTemperature = File(zone, THERMAL_TEMP_FILE).readTrimmedText()
                    if (type.isBlank() || rawTemperature.isBlank()) {
                        null
                    } else {
                        ThermalZoneReading(type = type, rawTemperature = rawTemperature)
                    }
                },
        )

    private fun thermalZoneDirectories(): List<File> {
        val thermalRoot = File(THERMAL_PATH)
        val listedZones =
            thermalRoot
                .listFiles { file -> file.name.startsWith(THERMAL_ZONE_PREFIX) }
                .orEmpty()
                .sortedBy { zone ->
                    zone.name.removePrefix(THERMAL_ZONE_PREFIX).toIntOrNull() ?: Int.MAX_VALUE
                }
        if (listedZones.isNotEmpty()) return listedZones

        return (0..MAX_THERMAL_ZONE_INDEX)
            .map { index -> File(thermalRoot, "$THERMAL_ZONE_PREFIX$index") }
    }

    private companion object {
        private const val UNKNOWN = "Unknown"
        private const val THERMAL_PATH = "/sys/class/thermal"
        private const val THERMAL_ZONE_PREFIX = "thermal_zone"
        private const val THERMAL_TYPE_FILE = "type"
        private const val THERMAL_TEMP_FILE = "temp"
        private const val MAX_THERMAL_ZONE_INDEX = 128

        // CPU 閺堚偓婢堆囶暥閻滃洨娈戦崣顖濆厴 sysfs 鐠侯垰绶?
        private val CPU_FREQUENCY_PATHS =
            listOf(
                "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq",
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq",
            )
    }
}

private fun File.readTrimmedText(): String =
    runCatching { readText().trim() }.getOrDefault("")

private fun Int?.toBatteryStatusText(): String =
    when (this) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }
