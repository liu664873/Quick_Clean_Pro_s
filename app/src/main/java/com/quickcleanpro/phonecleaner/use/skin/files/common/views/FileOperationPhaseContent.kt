package com.quickcleanpro.phonecleaner.use.skin.files.common.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXEmptyScanResult
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.FileOperationPhase
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.CleanXDeleteCompleteStage

@Composable
internal fun FileOperationPhaseContent(
    phase: FileOperationPhase,
    scanningText: String,
    deletingText: String? = null,
    resultAmount: String,
    resultUnit: String,
    resultCaption: String,
    onNavigateTool: (AppDestination) -> Unit,
    onContinue: () -> Unit,
    browsingContent: @Composable () -> Unit,
) {
    when (phase) {
        FileOperationPhase.Scanning -> FileManagerScanningView(text = scanningText)
        FileOperationPhase.Browsing,
        FileOperationPhase.ConfirmDelete -> browsingContent()
        FileOperationPhase.Deleting,
        FileOperationPhase.CompleteAnimation -> FileManagerDeletingView(
            fallbackText = deletingText ?: stringResource(R.string.file_deleting_files),
            stage =
                if (phase == FileOperationPhase.CompleteAnimation) {
                    CleanXDeleteCompleteStage.Complete
                } else {
                    CleanXDeleteCompleteStage.Deleting
                },
        )
        FileOperationPhase.Result -> FileManagerResultView(
            amount = resultAmount,
            unit = resultUnit,
            caption = resultCaption,
            onNavigateTool = onNavigateTool,
            onContinue = onContinue,
        )
        FileOperationPhase.NoResults -> CleanXEmptyScanResult(
            message = stringResource(R.string.file_scan_completed_no_results),
        )
    }
}
