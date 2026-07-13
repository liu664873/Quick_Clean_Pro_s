package com.quickcleanpro.phonecleaner.feature.junkclean.ui

import com.quickcleanpro.phonecleaner.feature.junkclean.*

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.feature.junkclean.model.JunkCategory
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanScanState
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanUiState
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXScanProgress

private val Navy = Color(0xFF2D3748)
private val MutedText = Color(0xFF8190A5)

@Composable
internal fun JunkScanningView(uiState: JunkCleanUiState) {
    val categoryLabel = uiState.currentCategory?.let { stringResource(it.titleRes) }
    val scanningText =
        categoryLabel?.let { stringResource(R.string.scan_scanning_category, it) }
            ?: stringResource(R.string.scan_loading_fallback)

    CleanXScanProgress(
        captionText =
            if (uiState.scanState == JunkCleanScanState.Error) {
                uiState.errorMessage ?: uiState.errorMessageRes?.let { stringResource(it) }
                    ?: stringResource(R.string.scan_failed)
            } else {
                scanningText
            },
        captionColor = if (uiState.scanState == JunkCleanScanState.Error) MutedText else Navy,
    ) {
        Text(
            text = uiState.formattedFoundSize.ifBlank { "0 B" },
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF22A9E8),
            textAlign = TextAlign.Center,
        )
    }
}

private val JunkCategory.titleRes: Int
    @StringRes
    get() =
        when (this) {
            JunkCategory.CACHE -> R.string.category_cache
            JunkCategory.TEMP_FILE -> R.string.category_temp
            JunkCategory.RESIDUAL -> R.string.category_residual
            JunkCategory.APK -> R.string.category_apk
            JunkCategory.DUPLICATE -> R.string.category_duplicate
            JunkCategory.LARGE_FILE -> R.string.category_large
        }

