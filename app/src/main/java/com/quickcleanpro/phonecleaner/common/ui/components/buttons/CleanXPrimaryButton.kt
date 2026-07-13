package com.quickcleanpro.phonecleaner.common.ui.components.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.quickcleanpro.phonecleaner.common.ui.components.cleanXPressFeedback
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXBlue
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXButtonHeight
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXButtonHorizontalPadding
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXButtonShape
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXButtonVerticalPadding
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXLineSubtitle
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXTextTitle

@Composable
fun CleanXPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(CleanXButtonHeight)
            .cleanXPressFeedback(
                interactionSource = interactionSource,
                enabled = enabled,
                pressedAlpha = 0.88f,
                pressedScale = 0.98f,
            ),
        shape = CleanXButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CleanXBlue,
            contentColor = Color.White,
            disabledContainerColor = CleanXBlue.copy(alpha = 0.45f),
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = CleanXButtonHorizontalPadding, vertical = CleanXButtonVerticalPadding)
    ) {
        Text(
            text = text,
            fontSize = CleanXTextTitle,
            lineHeight = CleanXLineSubtitle,
            fontWeight = FontWeight.W500
        )
    }
}
