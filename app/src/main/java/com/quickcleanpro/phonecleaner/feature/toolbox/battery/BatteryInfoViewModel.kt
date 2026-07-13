package com.quickcleanpro.phonecleaner.feature.toolbox.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.feature.settings.SettingsRepository
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.data.BatteryHistoryRepository
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.data.BatteryHistorySampler
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.BatteryCurrentRange
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.DeviceInfoMode
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.DeviceInfoStateController
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.DeviceInfoUiState
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.ProcCpuUsageReader
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.DeviceInfoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

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
    private val _effects = MutableSharedFlow<BatteryInfoEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<BatteryInfoEffect> = _effects.asSharedFlow()

    fun onAction(action: BatteryInfoAction) {
        when (action) {
            BatteryInfoAction.Resumed -> {
                controller.load(DeviceInfoMode.Battery)
                controller.startBatterySampling()
            }
            BatteryInfoAction.Paused -> controller.stopBatterySampling()
            BatteryInfoAction.Back -> {
                if (uiState.value.isScanning) controller.showStopDialog() else exit()
            }
            BatteryInfoAction.QuitScan -> {
                controller.dismissStopDialog()
                exit()
            }
            BatteryInfoAction.ResumeScan -> controller.resumeScan()
            is BatteryInfoAction.RangeSelected -> controller.selectCurrentRange(action.range)
        }
    }

    private fun exit() {
        _effects.tryEmit(BatteryInfoEffect.Exit)
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}

sealed interface BatteryInfoAction {
    data object Resumed : BatteryInfoAction
    data object Paused : BatteryInfoAction
    data object Back : BatteryInfoAction
    data object QuitScan : BatteryInfoAction
    data object ResumeScan : BatteryInfoAction
    data class RangeSelected(val range: BatteryCurrentRange) : BatteryInfoAction
}

sealed interface BatteryInfoEffect {
    data object Exit : BatteryInfoEffect
}
