package com.quickcleanpro.phonecleaner.common.ui.components.popups

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R

@Composable
internal fun StopScanDialog(onQuit: () -> Unit, onResume: () -> Unit) {
    CleanXDecisionDialog(
        title = stringResource(R.string.file_stop_scan_title),
        onDismissRequest = onResume,
        dismissText = stringResource(R.string.file_quit),
        onDismissAction = onQuit,
        confirmText = stringResource(R.string.file_resume),
        onConfirmAction = onResume,
    )
}
