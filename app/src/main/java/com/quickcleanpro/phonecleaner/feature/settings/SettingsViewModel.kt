package com.quickcleanpro.phonecleaner.feature.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val temperatureUnit: String,
)

sealed interface SettingsAction {
    data class TemperatureUnitChanged(val unit: String) : SettingsAction
    data object Back : SettingsAction
    data object OpenTerms : SettingsAction
    data object OpenPrivacy : SettingsAction
    data object OpenAdPrivacy : SettingsAction
    data object RateApp : SettingsAction
}

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(repository.readTemperatureUnit().normalizeTemperatureUnit()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.TemperatureUnitChanged -> setTemperatureUnit(action.unit)
            SettingsAction.Back,
            SettingsAction.OpenTerms,
            SettingsAction.OpenPrivacy,
            SettingsAction.OpenAdPrivacy,
            SettingsAction.RateApp -> Unit
        }
    }

    fun setTemperatureUnit(unit: String) {
        val normalized = unit.normalizeTemperatureUnit()
        repository.saveTemperatureUnit(normalized)
        _uiState.value = SettingsUiState(normalized)
    }
}

fun String.normalizeTemperatureUnit(): String =
    if (equals("F", ignoreCase = true)) "F" else "C"
