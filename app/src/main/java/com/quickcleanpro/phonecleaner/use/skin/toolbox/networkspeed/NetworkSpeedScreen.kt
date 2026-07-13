package com.quickcleanpro.phonecleaner.use.skin.toolbox.networkspeed

import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedPhase
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.networkspeed.NetworkSpeedUiState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.common.components.stableNavigationBarsPadding
import com.quickcleanpro.phonecleaner.use.skin.common.components.buttons.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.use.skin.toolbox.networkspeed.views.NetworkSpeedContentView

@Composable
fun NetworkSpeedScreen(
    uiState: NetworkSpeedUiState,
    onBack: () -> Unit,
    onRunSpeedTest: () -> Unit,
    onStopSpeedTest: () -> Unit,
    onNavigateTool: (AppDestination) -> Unit,
) {
    CleanXScaffoldPage(
        title = stringResource(R.string.network_speed),
        titleFontSize = 20.sp,
        onBack = onBack,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        bottomBar = {
            if (uiState.phase != NetworkSpeedPhase.Result && uiState.phase != NetworkSpeedPhase.Completing) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .stableNavigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    CleanXPrimaryButton(
                        text =
                            if (uiState.phase == NetworkSpeedPhase.Testing) {
                                stringResource(R.string.stop)
                            } else {
                                stringResource(R.string.run_speed_test)
                            },
                        onClick =
                            if (uiState.phase == NetworkSpeedPhase.Testing) {
                                onStopSpeedTest
                            } else {
                                onRunSpeedTest
                            },
                        enabled = uiState.hasNetwork || uiState.phase == NetworkSpeedPhase.Testing,
                    )
                }
            }
        },
    ) {
        NetworkSpeedContentView(
            uiState = uiState,
            onNavigateTool = onNavigateTool,
        )
    }
}
