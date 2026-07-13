package com.quickcleanpro.phonecleaner.use.skin.files.common.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.CleanXDeleteCompleteAnimation
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.CleanXDeleteCompleteStage

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
