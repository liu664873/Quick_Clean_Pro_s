package com.quickcleanpro.phonecleaner.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.model.StorageInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.DeviceInfoRepository
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class OnboardingUiState(
    val deviceModel: String = "",
    val androidVersion: String = "",
    val screenSize: String = "",
    val batteryHealth: String = "",
    val batteryStatusText: String = "",
    val storageInfo: StorageInfo = StorageInfo(0, 0, 0),
    val completedStep: Int = 0,
) {
    val complete: Boolean get() = completedStep > ONBOARDING_SCAN_ROW_COUNT
}

sealed interface OnboardingAction {
    data object Entered : OnboardingAction
    data object Refresh : OnboardingAction
    data object Skip : OnboardingAction
    data object GetStarted : OnboardingAction
}

sealed interface OnboardingEffect {
    data object ScanFinished : OnboardingEffect
    data class Complete(val skipped: Boolean) : OnboardingEffect
}

class OnboardingScanViewModel(
    private val repository: DeviceInfoRepository,
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<OnboardingEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<OnboardingEffect> = _effects.asSharedFlow()
    private var entered = false
    private var completionSubmitted = false

    init {
        refresh()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.Entered -> startScanProgress()
            OnboardingAction.Refresh -> refresh()
            OnboardingAction.Skip -> completeOnboarding(skipped = true)
            OnboardingAction.GetStarted -> completeOnboarding(skipped = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val hardware = repository.hardwareInfo()
            val battery = repository.batteryInfo()
            val batteryStatus = repository.batteryStatusInfo()
            val storage = repository.internalStorageInfo()
            _uiState.value =
                _uiState.value.copy(
                    deviceModel = hardware.model,
                    androidVersion = hardware.androidVersion,
                    screenSize = hardware.screenSize,
                    batteryHealth = battery.health,
                    batteryStatusText = batteryStatus.statusText,
                    storageInfo = storage,
                )
        }
    }

    private fun startScanProgress() {
        if (entered) return
        entered = true
        viewModelScope.launch {
            for (step in 1..ONBOARDING_SCAN_ROW_COUNT) {
                delay(ONBOARDING_STEP_DELAY_MILLIS)
                _uiState.value = _uiState.value.copy(completedStep = step)
            }
            delay(ONBOARDING_STEP_DELAY_MILLIS)
            _uiState.value = _uiState.value.copy(completedStep = ONBOARDING_SCAN_ROW_COUNT + 1)
            _effects.emit(OnboardingEffect.ScanFinished)
        }
    }

    private fun completeOnboarding(skipped: Boolean) {
        if (completionSubmitted || !_uiState.value.complete) return
        completionSubmitted = true
        onboardingPreferences.markScanCompleted()
        _effects.tryEmit(OnboardingEffect.Complete(skipped))
    }
}

private const val ONBOARDING_SCAN_ROW_COUNT = 6
private const val ONBOARDING_STEP_DELAY_MILLIS = 680L
