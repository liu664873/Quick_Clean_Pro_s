package com.quickcleanpro.phonecleaner.use.skin.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.skin.common.theme.LocalAppThemeTokens
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXHeaderBottomPadding
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXHeaderIconSize
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXHeaderTopPadding
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXIconButtonSize
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXPillShape

private val CleanXHeaderText: Color @Composable @ReadOnlyComposable get() = LocalAppThemeTokens.current.colors.textPrimary

@Composable
fun CleanXTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    titleFontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    showBack: Boolean = true,
    onBack: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backAction: () -> Unit = {
        if (onBack != null) onBack() else backDispatcher?.onBackPressed()
        Unit
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .stableStatusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = CleanXHeaderTopPadding, bottom = CleanXHeaderBottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Box(
                modifier =
                    Modifier
                        .size(CleanXIconButtonSize)
                        .clip(CleanXPillShape)
                        .cleanXDebouncedClick { backAction() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = CleanXHeaderText,
                    modifier = Modifier.size(CleanXHeaderIconSize),
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text = title,
            color = CleanXHeaderText,
            fontSize = titleFontSize,
            lineHeight = 27.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions?.invoke(this)
    }
}
