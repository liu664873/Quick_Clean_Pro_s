package com.quickcleanpro.phonecleaner.feature.junkclean.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.common.ui.components.popups.StopScanDialog
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanPhase
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanUiAction
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanUiState

@Composable
fun JunkCleanScreen(
    state: JunkCleanUiState,
    onAction: (JunkCleanUiAction) -> Unit,
) {
    BackHandler { onAction(JunkCleanUiAction.Back) }

    CleanXScaffoldPage(
        title = stringResource(R.string.junk_removal),
        onBack = { onAction(JunkCleanUiAction.Back) },
        scrollEnabled = false,
        contentPadding = PaddingValues(0.dp),
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Color(0xFFE3ECFD), Color(0xFFDFEBF5)),
        ),
        bottomBar = {
            if (state.phase == JunkCleanPhase.Preview) {
                JunkScanResultBottomBar(
                    selectedSummary = state.selectedSummary,
                    onClean = { onAction(JunkCleanUiAction.CleanSelected) },
                )
            }
        },
    ) {
        JunkCleanContentView(
            uiState = state,
            onToggleCategorySelection = { onAction(JunkCleanUiAction.ToggleCategories(it)) },
            onToggleItem = { onAction(JunkCleanUiAction.ToggleItem(it.junkFile.id)) },
            onContinueFromResult = { onAction(JunkCleanUiAction.ContinueFromResult) },
        )
    }

    if (state.showStopDialog) {
        StopScanDialog(
            onQuit = { onAction(JunkCleanUiAction.QuitStoppedOperation) },
            onResume = { onAction(JunkCleanUiAction.ResumeStoppedOperation) },
        )
    }
}
