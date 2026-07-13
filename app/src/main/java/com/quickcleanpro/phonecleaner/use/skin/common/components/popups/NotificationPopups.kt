package com.quickcleanpro.phonecleaner.use.skin.common.components.popups

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R

@Composable
internal fun NotificationBlockingTurnedOffDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    CleanXDecisionDialog(
        title = stringResource(R.string.notification_blocking_off_message),
        message = stringResource(R.string.notification_blocking_off_detail),
        onDismissRequest = onCancel,
        dismissText = stringResource(R.string.cancel),
        onDismissAction = onCancel,
        confirmText = stringResource(R.string.ok),
        onConfirmAction = onConfirm,
    )
}
