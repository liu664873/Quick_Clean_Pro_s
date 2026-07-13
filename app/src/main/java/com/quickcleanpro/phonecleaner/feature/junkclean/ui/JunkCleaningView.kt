package com.quickcleanpro.phonecleaner.feature.junkclean.ui

import com.quickcleanpro.phonecleaner.feature.junkclean.*

import com.quickcleanpro.phonecleaner.feature.junkclean.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXDeleteCompleteAnimation
import com.quickcleanpro.phonecleaner.common.ui.components.animations.CleanXDeleteCompleteStage

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
