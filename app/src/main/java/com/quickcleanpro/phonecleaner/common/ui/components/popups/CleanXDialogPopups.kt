package com.quickcleanpro.phonecleaner.common.ui.components.popups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBlue
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXMutedText
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXText
import com.quickcleanpro.phonecleaner.common.ui.components.cleanXPressFeedback

@Composable
internal fun CleanXDecisionDialog(
    title: String,
    onDismissRequest: () -> Unit,
    dismissText: String,
    onDismissAction: () -> Unit,
    confirmText: String,
    onConfirmAction: () -> Unit,
    message: String? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val body = message?.takeIf { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        shape = RoundedCornerShape(10.dp),
        title = {
            Text(
                text = title,
                color = CleanXText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = body?.let {
            {
                Text(
                    text = it,
                    color = CleanXMutedText,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dismissInteractionSource = remember { MutableInteractionSource() }
                    val confirmInteractionSource = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = onDismissAction,
                        modifier = Modifier.cleanXPressFeedback(
                            interactionSource = dismissInteractionSource,
                            pressedAlpha = 0.78f,
                            pressedScale = 1f,
                        ),
                        interactionSource = dismissInteractionSource,
                    ) {
                        Text(
                            text = dismissText,
                            color = Color(0xFFB3BDCB),
                            fontSize = 16.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onConfirmAction,
                        modifier = Modifier.cleanXPressFeedback(
                            interactionSource = confirmInteractionSource,
                            pressedAlpha = 0.78f,
                            pressedScale = 1f,
                        ),
                        shape = RoundedCornerShape(50),
                        interactionSource = confirmInteractionSource,
                        border = BorderStroke(width = 1.dp, color = CleanXBlue)
                    ) {
                        Text(
                            text = confirmText,
                            color = CleanXBlue,
                            fontSize = 16.sp,
                        )
                    }
                }
                extraContent?.invoke()
            }
        },
    )
}

@Composable
internal fun CleanXSingleActionDialog(
    title: String,
    actionText: String,
    onAction: () -> Unit,
    onDismissRequest: () -> Unit = onAction,
    message: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        shape = RoundedCornerShape(10.dp),
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    color = CleanXText,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                message?.takeIf { it.isNotBlank() }?.let { body ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = body,
                        color = CleanXMutedText,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                CleanXPrimaryButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.padding(horizontal = 0.dp),
                )
            }
        },
        confirmButton = {},
    )
}
