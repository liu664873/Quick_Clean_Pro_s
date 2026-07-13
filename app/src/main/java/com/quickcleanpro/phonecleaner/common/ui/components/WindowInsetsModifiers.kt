package com.quickcleanpro.phonecleaner.common.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.stableStatusBarsPadding(): Modifier =
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.stableNavigationBarsPadding(): Modifier =
    windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
