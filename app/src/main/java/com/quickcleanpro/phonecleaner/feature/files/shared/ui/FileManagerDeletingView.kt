package com.quickcleanpro.phonecleaner.feature.files.shared.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXDeleteCompleteAnimation
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXDeleteCompleteStage

@Composable
internal fun FileManagerDeletingView(
    fallbackText: String? = null,
    stage: CleanXDeleteCompleteStage = CleanXDeleteCompleteStage.Deleting,
) {
    CleanXDeleteCompleteAnimation(
        stage = stage,
        modifier = Modifier.fillMaxSize(),
        fallbackText = fallbackText,
        drawBackground = false,
    )
}
