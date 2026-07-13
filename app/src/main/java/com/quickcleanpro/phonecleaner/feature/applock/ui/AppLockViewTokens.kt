package com.quickcleanpro.phonecleaner.feature.applock.ui

import com.quickcleanpro.phonecleaner.feature.applock.*

import com.quickcleanpro.phonecleaner.feature.applock.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.common.ui.theme.LocalAppThemeTokens

internal const val PIN_LENGTH = 4

private val c @Composable @ReadOnlyComposable get() = LocalAppThemeTokens.current.colors

internal val AppLockBackground: Color @Composable @ReadOnlyComposable get() = c.pageBackground
internal val AppLockCardColor: Color @Composable @ReadOnlyComposable get() = c.cardBackground
internal val AppLockNavy: Color @Composable @ReadOnlyComposable get() = c.textPrimary
internal val AppLockSecondaryText: Color @Composable @ReadOnlyComposable get() = c.textMuted
internal val AppLockPlaceholderText: Color @Composable @ReadOnlyComposable get() = c.textMuted
internal val AppLockDividerColor: Color @Composable @ReadOnlyComposable get() = c.divider
internal val AppLockCardRadius: Dp @Composable @ReadOnlyComposable get() = c.let { 10.dp }
internal val PinSelectedColor: Color @Composable @ReadOnlyComposable get() = c.textPrimary
internal val PinUnselectedBorderColor: Color @Composable @ReadOnlyComposable get() = Color(0xA68190A5)
internal val PinKeyBackground: Color @Composable @ReadOnlyComposable get() = Color(0x408190A5)
internal val PinErrorColor: Color @Composable @ReadOnlyComposable get() = c.pinError
