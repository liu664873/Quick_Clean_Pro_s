package com.quickcleanpro.phonecleaner.feature.settings.ui

import com.quickcleanpro.phonecleaner.feature.settings.*

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBlue
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXMutedText
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXText
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXCardColor
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXCardShape

@Composable
internal fun TemperatureUnitDialog(
    selected: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CleanXCardColor,
        shape = CleanXCardShape,
        title = {
            Text(
                text = stringResource(R.string.settings_temperature_unit),
                color = CleanXText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TemperatureOption(unit = "F", selected = selected == "F", onClick = { onSelected("F") })
                TemperatureOption(unit = "C", selected = selected == "C", onClick = { onSelected("C") })
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun TemperatureOption(
    unit: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = unit,
            color = CleanXText,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CleanXBlue,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
internal fun SettingsRateDialog(
    onDismiss: () -> Unit,
    onRate: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(0) }
    var showFeedback by remember { mutableStateOf(false) }
    var tracked by remember { mutableStateOf(false) }

    fun close(ifOk: Boolean) {
        if (!tracked) {
            tracked = true
            AnalyticsTracker.trackRatePopup(ifOk)
        }
        onDismiss()
    }

    Dialog(onDismissRequest = { close(false) }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showFeedback) {
                    RateFeedbackContent(onDismiss = { close(false) })
                } else {
                    RateSelectionContent(
                        rating = rating,
                        onRatingChange = { rating = it },
                        onSubmit = {
                            if (rating >= 4) {
                                close(true)
                                onRate()
                            } else {
                                showFeedback = true
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RateSelectionContent(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    Text(
        text = stringResource(R.string.rate_title),
        color = CleanXText,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < rating) Color(0xFFFFC52E) else Color(0xFFD3D6DC),
                modifier =
                    Modifier
                        .size(38.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onRatingChange(index + 1) },
                        ),
            )
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    CleanXPrimaryButton(
        text = stringResource(R.string.submit),
        onClick = onSubmit,
        enabled = rating > 0,
    )
}

@Composable
private fun RateFeedbackContent(onDismiss: () -> Unit) {
    Text(
        text = stringResource(R.string.rate_feedback_title),
        color = CleanXText,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.rate_feedback_message),
        color = CleanXMutedText,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(20.dp))
    CleanXPrimaryButton(
        text = stringResource(R.string.ok),
        onClick = onDismiss,
    )
}

internal fun openGooglePlayRatePage(context: Context) {
    val packageName = context.packageName
    val marketIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val webIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching {
        context.startActivity(marketIntent)
    }.recoverCatching {
        context.startActivity(webIntent)
    }
}
