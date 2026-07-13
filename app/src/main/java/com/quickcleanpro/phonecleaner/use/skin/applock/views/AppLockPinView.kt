package com.quickcleanpro.phonecleaner.use.skin.applock.views

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.use.feature.applock.presentation.AppLockUiState
import com.quickcleanpro.phonecleaner.use.skin.common.components.stableNavigationBarsPadding

@Composable
internal fun AppLockPinView(
    uiState: AppLockUiState,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val error = uiState.pinErrorRes
    val errorOffset = remember { Animatable(0f) }

    LaunchedEffect(error) {
        if (error != null) {
            errorOffset.snapTo(0f)
            listOf(20f, -20f, 20f, -20f, 0f).forEach { target ->
                errorOffset.animateTo(target, tween(durationMillis = 80))
            }
        } else {
            errorOffset.snapTo(0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .stableNavigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Text(
            text = stringResource(uiState.pinStep.hintRes),
            color = AppLockNavy,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        PinDots(length = uiState.pinInput.length)

        Spacer(modifier = Modifier.height(15.dp))
        Box(
            modifier = Modifier.height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (error != null) {
                Text(
                    text = stringResource(error),
                    color = PinErrorColor,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationX = errorOffset.value }
                )
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
        NumberPad(
            onDigit = { digit ->
                if (uiState.vibrationEnabled && uiState.pinInput.length != PIN_LENGTH) {
                    performPinVibration(context)
                }
                onDigit(digit)
            },
            onDelete = onDelete
        )
    }
}

@Composable
private fun PinDots(length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { index ->
            val selected = index < length
            Surface(
                modifier = Modifier.size(20.dp),
                color = if (selected) PinSelectedColor else Color.Transparent,
                shape = RoundedCornerShape(50),
                border = if (selected) null else BorderStroke(1.dp, PinUnselectedBorderColor)
            ) {}
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                row.forEach { digit ->
                    NumberButton(label = digit.toString(), onClick = { onDigit(digit) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.size(70.dp))
            NumberButton(label = "0", onClick = { onDigit('0') })
            NumberButton(label = "\u00D7", onClick = onDelete)
        }
    }
}

@Composable
private fun NumberButton(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .size(70.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = PinKeyBackground,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private fun performPinVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30L)
        }
    }
}
