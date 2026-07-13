package com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.device

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.common.coroutines.runSuspendCatching
import com.quickcleanpro.phonecleaner.use.core.model.device.BatteryHistorySample
import com.quickcleanpro.phonecleaner.use.core.model.device.BatteryStatusInfo
import com.quickcleanpro.phonecleaner.use.core.model.device.DeviceCpuInfo
import com.quickcleanpro.phonecleaner.use.core.model.device.DeviceHardwareInfo
import com.quickcleanpro.phonecleaner.use.core.model.device.DeviceSensorInfo
import com.quickcleanpro.phonecleaner.use.core.model.device.BatteryInfo
import com.quickcleanpro.phonecleaner.use.core.model.device.MemoryInfo
import com.quickcleanpro.phonecleaner.use.core.model.device.StorageInfo
import com.quickcleanpro.phonecleaner.use.core.repository.BatteryHistoryRepository
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.DeviceInfoRepository
import com.quickcleanpro.phonecleaner.use.core.repository.SettingsRepository
import com.quickcleanpro.phonecleaner.use.core.common.format.FileSizeFormatter
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistoryConfig
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistoryOwner
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistorySampler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DeviceInfoMode {
    Device,
    Battery,
}

enum class BatteryCurrentRange(
    @StringRes val labelRes: Int,
    val durationMillis: Long,
) {
    OneMinute(R.string.battery_range_1_min, 60_000L),
    OneHour(R.string.battery_range_1_hour, 60L * 60L * 1000L),
    TwentyFourHours(R.string.battery_range_24_hours, 24L * 60L * 60L * 1000L),
}

data class BatteryCurrentSample(
    val timestampMillis: Long,
    val currentMa: Float,
)

data class BatteryTemperatureSample(
    val timestampMillis: Long,
    val temperatureC: Float,
)

data class DeviceInfoUiState(
    val mode: DeviceInfoMode = DeviceInfoMode.Device,
    val battery: BatteryInfo = EMPTY_BATTERY_INFO,
    val batteryStatus: BatteryStatusInfo = EMPTY_BATTERY_STATUS,
    val memory: MemoryInfo = EMPTY_MEMORY_INFO,
    val storage: StorageInfo = EMPTY_STORAGE_INFO,
    val hardware: DeviceHardwareInfo = EMPTY_HARDWARE_INFO,
    val cpuUsagePercent: Int? = null,
    val cpuTemperatureC: Float? = null,
    val currentNow: Float? = null,
    val currentAverage: Float? = null,
    val selectedCurrentRange: BatteryCurrentRange = BatteryCurrentRange.OneMinute,
    val currentSamples: List<BatteryCurrentSample> = emptyList(),
    val temperatureSamples: List<BatteryTemperatureSample> = emptyList(),
    val latestSampleTimestampMillis: Long = 0L,
    val temperatureUnit: String = TEMPERATURE_UNIT_CELSIUS,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val batteryCapacityText: String
        get() = battery.levelPercent.takeIf { it >= 0 }?.let { "$it%" } ?: "--"

    val batteryLifeText: String
        get() = battery.availableTime.takeIf { it.isNotBlank() && it != UNKNOWN } ?: "--"

    val selectedCurrentSamples: List<BatteryCurrentSample>
        get() =
            filterCurrentSamples(
                samples = currentSamples,
                selectedRange = selectedCurrentRange,
                timestampMillis = latestSampleTimestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            )

    val selectedCurrentAverage: Float?
        get() = averageCurrent(selectedCurrentSamples, currentAverage)

    val selectedTemperatureSamples: List<BatteryTemperatureSample>
        get() =
            filterTemperatureSamples(
                samples = temperatureSamples,
                timestampMillis = latestSampleTimestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            )

    val selectedTemperatureAverageC: Float?
        get() = averageTemperature(selectedTemperatureSamples, battery.temperature)

    val displayCurrentNow: Float?
        get() = selectedCurrentSamples.lastOrNull()?.currentMa ?: currentNow

    val displayTemperatureC: Float
        get() = selectedTemperatureSamples.lastOrNull()?.temperatureC ?: battery.temperature
}

internal class DeviceInfoStateController(
    private val repository: DeviceInfoRepository,
    private val batteryHistoryRepository: BatteryHistoryRepository,
    private val batteryHistorySampler: BatteryHistorySampler,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val cpuUsageReader: CpuUsageReader,
    initialMode: DeviceInfoMode,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val maxSampleCount: Int = BatteryHistoryConfig.MAX_SAMPLES,
) {
    private val _uiState = MutableStateFlow(DeviceInfoUiState())
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

    private var historyCollectionJob: Job? = null

    init {
        load(initialMode)
    }

    fun load(mode: DeviceInfoMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                isLoading = true,
                cpuUsagePercent = null,
                errorMessage = null,
            )
        }
        if (mode == DeviceInfoMode.Battery) {
            startHistoryCollection()
        } else {
            stopBatterySampling()
        }
        scope.launch(ioDispatcher) {
            val timestamp = nowMillis()
            val previousState = _uiState.value
            runSuspendCatching {
                buildState(
                    mode = mode,
                    previousState = previousState,
                    timestampMillis = timestamp,
                )
            }.onSuccess { loaded ->
                _uiState.value = loaded
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun startBatterySampling() {
        startHistoryCollection()
        scope.launch(ioDispatcher) {
            val timestamp = nowMillis()
            applyHistorySamples(batteryHistoryRepository.loadRecent(timestamp), timestamp)
            val forceSample = shouldForceBatterySample(timestamp)
            sampleBatterySnapshot(forceSample = forceSample)
            batteryHistorySampler.start(BatteryHistoryOwner.BatteryPage)
        }
    }

    fun stopBatterySampling() {
        batteryHistorySampler.stop(BatteryHistoryOwner.BatteryPage)
        historyCollectionJob?.cancel()
        historyCollectionJob = null
    }

    fun selectCurrentRange(range: BatteryCurrentRange) {
        _uiState.update { it.copy(selectedCurrentRange = range) }
    }

    fun close() {
        stopBatterySampling()
    }

    private suspend fun buildState(
        mode: DeviceInfoMode,
        previousState: DeviceInfoUiState,
        timestampMillis: Long,
    ): DeviceInfoUiState {
        val battery = repository.batteryInfo()
        val batteryStatus = repository.batteryStatusInfo()
        val memory = repository.memoryInfo()
        val storage = repository.internalStorageInfo()
        val hardware = repository.hardwareInfo()
        val cpuTemperatureC = repository.cpuTemperatureC()
        val currentNow = repository.batteryCurrentNowMa().normalizedBatteryCurrent()
        val currentAverage = repository.batteryCurrentAverageMa().normalizedBatteryCurrent()
        val historySamples =
            if (mode == DeviceInfoMode.Battery) {
                batteryHistoryRepository.loadRecent(timestampMillis)
            } else {
                batteryHistoryRepository.samples.value
            }

        return DeviceInfoUiState(
            mode = mode,
            battery = battery,
            batteryStatus = batteryStatus,
            memory = memory,
            storage = storage,
            hardware = hardware,
            cpuUsagePercent = cpuUsageReader.readUsagePercent(previousState.cpuUsagePercent),
            cpuTemperatureC = cpuTemperatureC ?: previousState.cpuTemperatureC,
            currentNow = currentNow,
            currentAverage = currentAverage,
            selectedCurrentRange = previousState.selectedCurrentRange,
            currentSamples = historySamples.toCurrentSamples(timestampMillis, maxSampleCount),
            temperatureSamples = historySamples.toTemperatureSamples(timestampMillis),
            latestSampleTimestampMillis = timestampMillis,
            temperatureUnit = readTemperatureUnit(),
            isLoading = false,
            errorMessage = null,
        ).withHistorySamples(historySamples, timestampMillis, maxSampleCount)
    }

    private suspend fun sampleBatterySnapshot(forceSample: Boolean) {
        if (forceSample) {
            batteryHistorySampler.sampleOnce(force = true)
        }
        val timestamp = nowMillis()
        val battery = runSuspendCatching { repository.batteryInfo() }.getOrNull()
        val batteryStatus = runSuspendCatching { repository.batteryStatusInfo() }.getOrNull()
        val currentNow = runSuspendCatching { repository.batteryCurrentNowMa().normalizedBatteryCurrent() }.getOrNull()
        val currentAverage = runSuspendCatching { repository.batteryCurrentAverageMa().normalizedBatteryCurrent() }.getOrNull()
        val samples = batteryHistoryRepository.loadRecent(timestamp)

        _uiState.update { state ->
            state
                .copy(
                    battery = battery ?: state.battery,
                    batteryStatus = batteryStatus ?: state.batteryStatus,
                    currentNow = currentNow ?: state.currentNow,
                    currentAverage = currentAverage ?: state.currentAverage,
                    temperatureUnit = readTemperatureUnit(),
                    isLoading = false,
                ).withHistorySamples(samples, timestamp, maxSampleCount)
        }
    }

    private fun startHistoryCollection() {
        if (historyCollectionJob?.isActive == true) return
        historyCollectionJob =
            scope.launch(ioDispatcher) {
                batteryHistoryRepository.samples.collect { samples ->
                    applyHistorySamples(samples, nowMillis())
                }
            }
    }

    private fun applyHistorySamples(
        samples: List<BatteryHistorySample>,
        timestampMillis: Long,
    ) {
        _uiState.update { state ->
            state
                .copy(temperatureUnit = readTemperatureUnit())
                .withHistorySamples(samples, timestampMillis, maxSampleCount)
        }
    }

    private fun readTemperatureUnit(): String = settingsRepository.readTemperatureUnit().normalizeTemperatureUnit()

    private fun shouldForceBatterySample(timestampMillis: Long): Boolean {
        val latestSample =
            batteryHistoryRepository.samples.value
                .lastOrNull()
                ?: batteryHistoryRepository.loadRecent(timestampMillis).lastOrNull()
                ?: return true
        return timestampMillis - latestSample.timestampMillis !in 0L..BATTERY_HISTORY_FRESH_SAMPLE_MILLIS
    }
}

class DeviceInfoViewModel(
    repository: DeviceInfoRepository,
    batteryHistoryRepository: BatteryHistoryRepository,
    batteryHistorySampler: BatteryHistorySampler,
    settingsRepository: SettingsRepository,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val controller =
        DeviceInfoStateController(
            repository = repository,
            batteryHistoryRepository = batteryHistoryRepository,
            batteryHistorySampler = batteryHistorySampler,
            settingsRepository = settingsRepository,
            scope = viewModelScope,
            ioDispatcher = ioDispatcher,
            cpuUsageReader = ProcCpuUsageReader(ioDispatcher),
            initialMode = DeviceInfoMode.Device,
        )

    val uiState: StateFlow<DeviceInfoUiState> = controller.uiState

    fun load(mode: DeviceInfoMode) = controller.load(mode)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}

class BatteryInfoViewModel(
    repository: DeviceInfoRepository,
    batteryHistoryRepository: BatteryHistoryRepository,
    batteryHistorySampler: BatteryHistorySampler,
    settingsRepository: SettingsRepository,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val controller =
        DeviceInfoStateController(
            repository = repository,
            batteryHistoryRepository = batteryHistoryRepository,
            batteryHistorySampler = batteryHistorySampler,
            settingsRepository = settingsRepository,
            scope = viewModelScope,
            ioDispatcher = ioDispatcher,
            cpuUsageReader = ProcCpuUsageReader(ioDispatcher),
            initialMode = DeviceInfoMode.Battery,
        )

    val uiState: StateFlow<DeviceInfoUiState> = controller.uiState

    fun load(mode: DeviceInfoMode) = controller.load(mode)

    fun startBatterySampling() = controller.startBatterySampling()

    fun stopBatterySampling() = controller.stopBatterySampling()

    fun selectCurrentRange(range: BatteryCurrentRange) = controller.selectCurrentRange(range)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}

internal fun formatBatteryTemperature(
    tempC: Float?,
    unit: String = TEMPERATURE_UNIT_CELSIUS,
    includeSpace: Boolean = true,
): String {
    val temperature = tempC?.takeIf { it.isFiniteValue() } ?: return "--"
    val normalizedUnit = unit.normalizeTemperatureUnit()
    val displayTemperature = if (normalizedUnit == TEMPERATURE_UNIT_FAHRENHEIT) {
        celsiusToFahrenheit(temperature)
    } else {
        temperature
    }
    val separator = if (includeSpace) " " else ""
    return String.format(Locale.US, "%.1f%s\u00B0%s", displayTemperature, separator, normalizedUnit)
}

internal fun displayBatteryTemperatureValue(
    tempC: Float,
    unit: String,
): Float =
    if (unit.normalizeTemperatureUnit() == TEMPERATURE_UNIT_FAHRENHEIT) {
        celsiusToFahrenheit(tempC)
    } else {
        tempC
    }

internal fun formatBatteryVoltage(voltageMilliVolts: Int): String =
    if (voltageMilliVolts > 0) {
        String.format(Locale.US, "%.1f V", voltageMilliVolts / 1000f)
    } else {
        "--"
    }

internal fun formatBatteryCurrent(currentMa: Float?): String =
    currentMa
        ?.takeIf { it.isFiniteValue() }
        ?.let { String.format(Locale.US, "%.2f mA", abs(it)) }
        ?: "--"

internal fun formatBatteryCapacityMah(capacityMah: Int?): String =
    capacityMah
        ?.takeIf { it > 0 }
        ?.let { "$it mAh" }
        ?: "--"

internal fun currentBatteryCapacityMah(battery: BatteryInfo): Int? {
    val totalCapacity = battery.capacity.takeIf { it > 0 } ?: return null
    val level = battery.levelPercent.takeIf { it >= 0 } ?: return null
    return (totalCapacity * level / 100f).roundToInt()
}

internal fun formatBytesOrPlaceholder(bytes: Long): String =
    if (bytes > 0L) {
        FileSizeFormatter.format(bytes)
    } else {
        "--"
    }

internal fun String.orPlaceholder(): String = takeIf { isNotBlank() && this != UNKNOWN } ?: "--"

private fun DeviceInfoUiState.withHistorySamples(
    samples: List<BatteryHistorySample>,
    timestampMillis: Long,
    maxSampleCount: Int,
): DeviceInfoUiState {
    val recentSamples = samples.recentBatteryHistory(timestampMillis)
    val latestSample = recentSamples.lastOrNull()
    val freshLatestSample =
        latestSample?.takeIf { sample ->
            timestampMillis - sample.timestampMillis in 0L..BATTERY_HISTORY_FRESH_SAMPLE_MILLIS
        }
    return copy(
        battery =
            freshLatestSample?.let { latest ->
                battery.copy(temperature = latest.temperatureC)
            } ?: battery,
        currentNow = freshLatestSample?.currentMa?.normalizedBatteryCurrent() ?: currentNow,
        currentSamples = recentSamples.toCurrentSamples(timestampMillis, maxSampleCount),
        temperatureSamples = recentSamples.toTemperatureSamples(timestampMillis),
        latestSampleTimestampMillis = latestSample?.timestampMillis ?: timestampMillis,
    )
}

private fun List<BatteryHistorySample>.toCurrentSamples(
    timestampMillis: Long,
    maxSampleCount: Int,
): List<BatteryCurrentSample> {
    val currentSamples =
        recentBatteryHistory(timestampMillis)
            .asSequence()
            .mapNotNull { sample ->
                sample.currentMa
                    ?.takeIf { it.isFiniteValue() }
                    ?.let { BatteryCurrentSample(sample.timestampMillis, abs(it)) }
            }.toList()
    return currentSamples.takeLast(maxSampleCount)
}

private fun List<BatteryHistorySample>.toTemperatureSamples(timestampMillis: Long): List<BatteryTemperatureSample> =
    recentBatteryHistory(timestampMillis)
        .asSequence()
        .filter { it.timestampMillis >= timestampMillis - TEMPERATURE_WINDOW_MILLIS }
        .filter { it.temperatureC.isFiniteValue() }
        .map { BatteryTemperatureSample(it.timestampMillis, it.temperatureC) }
        .toList()

private fun List<BatteryHistorySample>.recentBatteryHistory(timestampMillis: Long): List<BatteryHistorySample> {
    val cutoff = timestampMillis - BatteryCurrentRange.TwentyFourHours.durationMillis
    val latestAllowed = timestampMillis + BATTERY_HISTORY_FUTURE_TOLERANCE_MILLIS
    return asSequence()
        .filter { it.timestampMillis in cutoff..latestAllowed }
        .sortedBy { it.timestampMillis }
        .toList()
}

private fun filterCurrentSamples(
    samples: List<BatteryCurrentSample>,
    selectedRange: BatteryCurrentRange,
    timestampMillis: Long,
): List<BatteryCurrentSample> {
    val cutoff = timestampMillis - selectedRange.durationMillis
    return samples
        .asSequence()
        .filter { it.timestampMillis in cutoff..timestampMillis }
        .sortedBy { it.timestampMillis }
        .toList()
}

private fun filterTemperatureSamples(
    samples: List<BatteryTemperatureSample>,
    timestampMillis: Long,
): List<BatteryTemperatureSample> {
    val cutoff = timestampMillis - TEMPERATURE_WINDOW_MILLIS
    return samples
        .asSequence()
        .filter { it.timestampMillis in cutoff..timestampMillis }
        .sortedBy { it.timestampMillis }
        .toList()
}

private fun averageCurrent(
    samples: List<BatteryCurrentSample>,
    fallback: Float?,
): Float? =
    samples
        .takeIf { it.isNotEmpty() }
        ?.map { abs(it.currentMa) }
        ?.average()
        ?.toFloat()
        ?: fallback.normalizedBatteryCurrent()

private fun averageTemperature(
    samples: List<BatteryTemperatureSample>,
    fallback: Float?,
): Float? =
    samples
        .takeIf { it.isNotEmpty() }
        ?.map { it.temperatureC }
        ?.average()
        ?.toFloat()
        ?: fallback

private fun Float.isFiniteValue(): Boolean = !isNaN() && !isInfinite()

private fun Float?.normalizedBatteryCurrent(): Float? = this?.takeIf { it.isFiniteValue() }?.let { abs(it) }

private fun String.normalizeTemperatureUnit(): String =
    if (equals(TEMPERATURE_UNIT_FAHRENHEIT, ignoreCase = true)) {
        TEMPERATURE_UNIT_FAHRENHEIT
    } else {
        TEMPERATURE_UNIT_CELSIUS
    }

private fun celsiusToFahrenheit(value: Float): Float = value * 9f / 5f + 32f

private const val UNKNOWN = "Unknown"
private const val TEMPERATURE_UNIT_CELSIUS = "C"
private const val TEMPERATURE_UNIT_FAHRENHEIT = "F"
private const val TEMPERATURE_WINDOW_MILLIS = 60_000L
private const val BATTERY_HISTORY_FUTURE_TOLERANCE_MILLIS = 60_000L
private const val BATTERY_HISTORY_FRESH_SAMPLE_MILLIS = BatteryHistoryConfig.SAMPLE_INTERVAL_MILLIS + 5_000L
private val EMPTY_BATTERY_INFO = BatteryInfo(-1, UNKNOWN, 0f, -1, UNKNOWN, 0, UNKNOWN)
private val EMPTY_BATTERY_STATUS = BatteryStatusInfo(statusText = UNKNOWN, isCharging = false)
private val EMPTY_MEMORY_INFO = MemoryInfo(0L, 0L, 0L, 0, false)
private val EMPTY_STORAGE_INFO = StorageInfo(0L, 0L, 0L)
private val EMPTY_HARDWARE_INFO =
    DeviceHardwareInfo(
        model = UNKNOWN,
        androidVersion = "Android --",
        screenSize = "--",
        screenDensity = "--",
        multiTouchSupported = false,
        sensors =
            DeviceSensorInfo(
                accelerometer = false,
                magneticField = false,
                orientation = false,
                gyroscope = false,
                light = false,
                proximity = false,
                ambientTemperature = false,
            ),
        cpu =
            DeviceCpuInfo(
                hardware = UNKNOWN,
                model = UNKNOWN,
                cores = 0,
                maxFrequency = UNKNOWN,
            ),
    )
