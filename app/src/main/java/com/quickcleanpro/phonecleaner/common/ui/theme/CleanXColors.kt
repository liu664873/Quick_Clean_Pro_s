package com.quickcleanpro.phonecleaner.common.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.quickcleanpro.phonecleaner.common.ui.theme.LocalAppThemeTokens

@get:Composable
@get:ReadOnlyComposable
val Colors: com.quickcleanpro.phonecleaner.common.ui.theme.AppColors
    get() = LocalAppThemeTokens.current.colors

val CleanXBackground: Color @Composable @ReadOnlyComposable get() = Colors.pageBackground
val CleanXText: Color @Composable @ReadOnlyComposable get() = Colors.textPrimary
val CleanXMutedText: Color @Composable @ReadOnlyComposable get() = Colors.textMuted
val CleanXBlue: Color @Composable @ReadOnlyComposable get() = Colors.primary
val CleanXBlueDark: Color @Composable @ReadOnlyComposable get() = Colors.primaryDeep
val CleanXBlueSoft: Color @Composable @ReadOnlyComposable get() = Colors.primarySoft
val CleanXCardColor: Color @Composable @ReadOnlyComposable get() = Colors.cardBackground
val CleanXSoftPanel: Color @Composable @ReadOnlyComposable get() = Colors.softPanelBackground
val CleanXSubtlePanel: Color @Composable @ReadOnlyComposable get() = Colors.subtlePanelBackground
val CleanXDivider: Color @Composable @ReadOnlyComposable get() = Colors.divider
val CleanXSuccess: Color @Composable @ReadOnlyComposable get() = Colors.success
val CleanXWarning: Color @Composable @ReadOnlyComposable get() = Colors.warning
val CleanXDanger: Color @Composable @ReadOnlyComposable get() = Colors.danger

// 閸忕厧顔愰弮褏澧?VirusUiTheme 瀵洜鏁ら敍鍫熷閺堝鈧ジ鍏樻禒搴濆瘜妫版ü鑵戦懢宄板絿閿?
val VirusBackgroundTop: Color @Composable @ReadOnlyComposable get() = Colors.gradientBackgroundTop
val VirusBackgroundBottom: Color @Composable @ReadOnlyComposable get() = Colors.gradientBackgroundBottom
val VirusBackgroundCard: Color @Composable @ReadOnlyComposable get() = Colors.virusBackgroundCard
val VirusTitle: Color @Composable @ReadOnlyComposable get() = Colors.navy
val VirusBody: Color @Composable @ReadOnlyComposable get() = Colors.textPrimary
val VirusSecondary: Color @Composable @ReadOnlyComposable get() = Colors.navyText
val VirusBlue: Color @Composable @ReadOnlyComposable get() = Colors.primary
val VirusBlueDeep: Color @Composable @ReadOnlyComposable get() = Colors.primaryDeep
val VirusTrackInactive: Color @Composable @ReadOnlyComposable get() = Colors.virusTrackInactive
val VirusTrackInactiveLine: Color @Composable @ReadOnlyComposable get() = Colors.virusTrackLine
val VirusDanger: Color @Composable @ReadOnlyComposable get() = Colors.dangerSoft
val VirusDivider: Color @Composable @ReadOnlyComposable get() = Colors.dividerNavy
val VirusOrange: Color @Composable @ReadOnlyComposable get() = Colors.orange
val VirusHigh: Color @Composable @ReadOnlyComposable get() = Colors.orangeHot
