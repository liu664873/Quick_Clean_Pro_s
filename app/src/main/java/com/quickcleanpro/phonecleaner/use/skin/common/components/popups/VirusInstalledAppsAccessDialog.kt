package com.quickcleanpro.phonecleaner.use.skin.common.components.popups

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quickcleanpro.phonecleaner.R
import androidx.compose.foundation.BorderStroke

private val VirusInstalledAppsDialogShape = RoundedCornerShape(12.dp)
private val VirusInstalledAppsButtonShape = RoundedCornerShape(28.dp)
private val VirusInstalledAppsPrimary = Color(0xFF22A9E8)
private val VirusInstalledAppsTitle = Color(0xFF1D2959)
private val VirusInstalledAppsBody = Color(0xA61D2959)

@Composable
internal fun VirusInstalledAppsAccessDialog(
    title: String,
    message: String,
    primaryText: String,
    onPrimaryAction: () -> Unit,
    secondaryText: String,
    onSecondaryAction: () -> Unit,
    onDismissRequest: () -> Unit = onSecondaryAction,
    showHeroImage: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 343.dp),
                color = Color.White,
                shape = VirusInstalledAppsDialogShape,
            ) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showHeroImage) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(137.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0DEFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.virus_app_list_permission),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(width = 186.dp, height = 111.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = title,
                    color = VirusInstalledAppsTitle,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    letterSpacing = 0.03.em,
                    fontWeight = FontWeight.Medium,
                    textAlign = if (showHeroImage) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = VirusInstalledAppsBody,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.03.em,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                VirusInstalledAppsFilledButton(
                    text = primaryText,
                    onClick = onPrimaryAction,
                )
                Spacer(modifier = Modifier.height(16.dp))
                VirusInstalledAppsOutlinedButton(
                    text = secondaryText,
                    onClick = onSecondaryAction,
                )
            }
            }
        }
    }
}

@Composable
private fun VirusInstalledAppsFilledButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        shape = VirusInstalledAppsButtonShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = VirusInstalledAppsPrimary,
                contentColor = Color.White,
            ),
    ) {
        VirusInstalledAppsButtonText(
            text = text,
            color = Color.White,
        )
    }
}

@Composable
private fun VirusInstalledAppsOutlinedButton(
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        shape = VirusInstalledAppsButtonShape,
        border = BorderStroke(1.35.dp, VirusInstalledAppsPrimary),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = VirusInstalledAppsPrimary,
            ),
    ) {
        VirusInstalledAppsButtonText(
            text = text,
            color = VirusInstalledAppsPrimary,
        )
    }
}

@Composable
private fun VirusInstalledAppsButtonText(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        color = color,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
