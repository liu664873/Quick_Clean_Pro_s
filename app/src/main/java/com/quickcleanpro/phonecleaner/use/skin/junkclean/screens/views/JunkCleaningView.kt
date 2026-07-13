package com.quickcleanpro.phonecleaner.use.skin.junkclean.screens.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.CleanXDeleteCompleteAnimation
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.CleanXDeleteCompleteStage

@Composable
internal fun JunkCleaningView(
    stage: CleanXDeleteCompleteStage = CleanXDeleteCompleteStage.Deleting,
) {
    CleanXDeleteCompleteAnimation(
        stage = stage,
        fallbackText = stringResource(R.string.cleaning_selected_files),
        drawBackground = false,
    )
}
