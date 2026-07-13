package com.quickcleanpro.phonecleaner.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.SdkInitializationCoordinator
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val onboardingPreferences: OnboardingPreferences,
    private val sdkInitialization: SdkInitializationCoordinator,
) : ViewModel() {
    private var machineState = SplashMachineState()
    private val _uiState = MutableStateFlow(machineState.toUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<SplashEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            sdkInitialization.awaitAdvertiseReady()
            onAction(SplashAction.SdkBarrierFinished)
        }
    }

    fun onAction(action: SplashAction) {
        val transition =
            reduceSplashState(
                state = machineState,
                action = action,
                normalDestination = startupDestination(),
            )
        if (transition.state != machineState) {
            machineState = transition.state
            _uiState.value = machineState.toUiState()
        }
        transition.effects.forEach { effect -> effectsChannel.trySend(effect) }
    }

    private fun startupDestination(): AppDestination =
        if (onboardingPreferences.isScanCompleted()) AppDestination.Home else AppDestination.OnboardingScan
}
