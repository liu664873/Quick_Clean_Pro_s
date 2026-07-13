package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkspeed.views

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedPhase
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedUiState

@Composable
internal fun NetworkSpeedContentView(
    uiState: NetworkSpeedUiState,
    onNavigateTool: (com.quickcleanpro.phonecleaner.app.navigation.AppDestination) -> Unit,
) {
    when (uiState.phase) {
        NetworkSpeedPhase.Idle -> NetworkSpeedIdleView(uiState = uiState)
        NetworkSpeedPhase.Testing -> NetworkSpeedTestingView(uiState = uiState)
        NetworkSpeedPhase.Completing -> NetworkSpeedTestingView(uiState = uiState)
        NetworkSpeedPhase.Result -> NetworkSpeedResultView(uiState = uiState, onNavigateTool = onNavigateTool)
        NetworkSpeedPhase.Error -> NetworkSpeedErrorView(uiState = uiState)
    }
}
