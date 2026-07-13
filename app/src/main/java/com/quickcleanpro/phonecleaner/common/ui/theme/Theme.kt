package com.quickcleanpro.phonecleaner.common.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import com.quickcleanpro.phonecleaner.BuildConfig

private fun lightSchemeFor(colors: AppColors) =
    lightColorScheme(
        primary = colors.primary,
        onPrimary = colors.textOnPrimary,
        primaryContainer = colors.primarySoft,
        secondary = Teal700,
        onSecondary = Gray50,
        secondaryContainer = Teal200,
        background = colors.gradientBackgroundTop,
        onBackground = colors.textPrimary,
        surface = colors.surfaceBackground,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.subtlePanelBackground,
        onSurfaceVariant = colors.textMuted,
        error = Red500,
        onError = Gray50,
    )

private fun darkSchemeFor(colors: AppColors) =
    darkColorScheme(
        primary = colors.primarySoft,
        onPrimary = Gray900,
        primaryContainer = colors.primary,
        secondary = Teal200,
        onSecondary = Gray900,
        secondaryContainer = Teal700,
        background = Gray900,
        onBackground = Gray50,
        surface = Gray900,
        onSurface = Gray50,
        surfaceVariant = Gray800,
        onSurfaceVariant = Gray400,
        error = Red500,
        onError = Gray50,
    )

private fun selectedAppThemeTokens(): AppThemeTokens =
    when (BuildConfig.PRODUCT_THEME_KEY) {
        "quickclean_pro" -> QuickCleanProThemeTokens
        else -> DefaultAppThemeTokens
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuickCleanProAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appThemeTokens = selectedAppThemeTokens()
    val colorScheme =
        if (darkTheme) {
            darkSchemeFor(appThemeTokens.colors)
        } else {
            lightSchemeFor(appThemeTokens.colors)
        }

    CompositionLocalProvider(LocalAppThemeTokens provides appThemeTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
        ) {
            CompositionLocalProvider(
                LocalIndication provides NoRippleIndication,
                LocalRippleConfiguration provides null,
                content = content,
            )
        }
    }
}

private object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = NoRippleNode()

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = other === this

    private class NoRippleNode : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            drawContent()
        }
    }
}
