package com.quickcleanpro.phonecleaner.feature.antivirus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusResultAction
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanUiState

@Composable
fun ScanVirusResultScreen(
    state: VirusScanUiState,
    onAction: (VirusResultAction) -> Unit,
) {
    var fileToDelete by remember { mutableStateOf<String?>(null) }

    fileToDelete?.let { path ->
        DeleteVirusFileDialog(
            onConfirm = {
                fileToDelete = null
                onAction(VirusResultAction.DeleteFileThreat(path))
            },
            onDismiss = { fileToDelete = null },
        )
    }

    VirusThreatResultView(
        uiState = state,
        onSolveAdbRisk = { onAction(VirusResultAction.SolveAdbRisk) },
        onSolveThreat = { threat ->
            if (threat.isFile) {
                threat.apkPath?.let { fileToDelete = it }
            } else {
                threat.packageName?.let { onAction(VirusResultAction.SolveAppThreat(it)) }
            }
        },
        onBack = { onAction(VirusResultAction.Back) },
    )
    BackHandler { onAction(VirusResultAction.Back) }
}

@Composable
fun NoVirusResultScreen(
    onAction: (VirusResultAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
) {
    BackHandler { onAction(VirusResultAction.Back) }
    CleanXScaffoldPage(
        title = stringResource(R.string.anti_virus),
        onBack = { onAction(VirusResultAction.Back) },
        scrollEnabled = false,
        contentPadding = PaddingValues(0.dp),
    ) {
        NoVirusResultView(
            onNavigateTool = onNavigate,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
