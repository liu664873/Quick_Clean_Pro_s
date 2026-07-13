package com.quickcleanpro.phonecleaner.use.skin.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.feature.home.presentation.HomeSummaryUiState
import com.quickcleanpro.phonecleaner.use.feature.notification.presentation.ToolNotificationSpec
import com.quickcleanpro.phonecleaner.use.skin.common.theme.LocalAppThemeTokens
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXIconButtonSize
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXPagePadding
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXPillShape
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXSegmentTabs
import com.quickcleanpro.phonecleaner.use.skin.common.components.cleanXDebouncedClick
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.SettingsRateDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.stableNavigationBarsPadding
import com.quickcleanpro.phonecleaner.use.skin.common.components.stableStatusBarsPadding
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXBlue
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXText
import kotlinx.coroutines.launch

private val PrimaryText: Color @Composable @ReadOnlyComposable get() = LocalAppThemeTokens.current.colors.textPrimary

private data class HomeTab(
    val label: String,
    val iconRes: Int,
)

@Composable
fun HomeScreen(
    summaryState: HomeSummaryUiState,
    exitPromptSpec: ToolNotificationSpec?,
    showAutoRateDialog: Boolean,
    externalBlockingPromptActive: Boolean = false,
    initialTabIndex: Int = 0,
    onExit: () -> Unit,
    onViewExitPromptFeature: () -> Unit,
    onSettingsClick: () -> Unit,
    onFeatureClick: (FeatureKey) -> Unit,
    onTabInteraction: () -> Unit,
    onDismissAutoRateDialog: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tabs =
        listOf(
            HomeTab(stringResource(R.string.home_tab_home), R.drawable.ic_home),
            HomeTab(stringResource(R.string.home_tab_file_manager), R.drawable.ic_file_manager),
            HomeTab(stringResource(R.string.home_tab_toolbox), R.drawable.ic_toolbox),
        )
    val pagerState =
        rememberPagerState(
            initialPage = initialTabIndex.coerceIn(0, tabs.lastIndex),
            pageCount = { tabs.size },
        )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.home_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .stableStatusBarsPadding()
                        .stableNavigationBarsPadding()
                        .padding(horizontal = CleanXPagePadding),
            ) {
                Header(
                    onSettingsClick = onSettingsClick,
                )
                Spacer(modifier = Modifier.height(16.dp))
                CleanXSegmentTabs(
                    tabs = tabs.map { it.label },
                    selectedIndex = pagerState.currentPage,
                    onSelected = { index ->
                        val fromIndex = pagerState.currentPage
                        onTabInteraction()
                        if (fromIndex != index) {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    },
                    containerColor = Color.Transparent,
                )
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 ->
                            HomeTabContent(
                                summaryState = summaryState,
                                onFeatureClick = onFeatureClick,
                            )
                        1 ->
                            FilesManagerTabContent(
                                onFeatureClick = onFeatureClick,
                            )
                        2 ->
                            ToolBoxTabContent(
                                summaryState = summaryState,
                                onFeatureClick = onFeatureClick,
                            )
                    }
                }
            }
        }
    }

    if (exitPromptSpec != null) {
        HomeExitPromptDialog(
            spec = exitPromptSpec,
            onExit = onExit,
            onViewNow = onViewExitPromptFeature,
        )
    }

    if (showAutoRateDialog && !externalBlockingPromptActive && exitPromptSpec == null) {
        SettingsRateDialog(onDismiss = onDismissAutoRateDialog)
    }
}

@Composable
private fun Header(onSettingsClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = PrimaryText,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .size(CleanXIconButtonSize)
                    .clip(CleanXPillShape)
                    .cleanXDebouncedClick(onClick = onSettingsClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = PrimaryText,
                modifier = Modifier.size(23.dp),
            )
        }
    }
}

@Composable
private fun HomeExitPromptDialog(
    spec: ToolNotificationSpec,
    onExit: () -> Unit,
    onViewNow: () -> Unit,
) {
    if (spec.route == AppDestination.NotificationCleaner.route) {
        NotificationCleanerExitPromptDialog(
            onEnableNow = onViewNow,
            onClose = onExit,
        )
    } else {
        ToolExitPromptDialog(
            spec = spec,
            onExit = onExit,
            onViewNow = onViewNow,
        )
    }
}

@Composable
private fun ToolExitPromptDialog(
    spec: ToolNotificationSpec,
    onExit: () -> Unit,
    onViewNow: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 343.dp),
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(spec.iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(toolExitPromptMessageRes(spec.route)),
                    color = CleanXText,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { onExit() }
                                .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.exit),
                            color = LocalAppThemeTokens.current.colors.textSecondary,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onViewNow,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, CleanXBlue),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = CleanXBlue,
                            ),
                        contentPadding = PaddingValues(horizontal = 19.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.view_now),
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCleanerExitPromptDialog(
    onEnableNow: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 343.dp),
            color = Color.White,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.exit_prompt_notification_title),
                    color = CleanXText,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))
                NotificationPromptTip(text = stringResource(R.string.exit_prompt_notification_tip_block))
                Spacer(modifier = Modifier.height(13.dp))
                NotificationPromptTip(text = stringResource(R.string.exit_prompt_notification_tip_matters))
                Spacer(modifier = Modifier.height(13.dp))
                NotificationPromptTip(text = stringResource(R.string.exit_prompt_notification_tip_focus))
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onEnableNow,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = CleanXBlue,
                            contentColor = Color.White,
                        ),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    Text(
                        text = stringResource(R.string.enable_now),
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(13.dp))
                OutlinedButton(
                    onClick = onClose,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, CleanXBlue),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = CleanXBlue,
                        ),
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPromptTip(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(19.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CleanXBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = text,
            color = LocalAppThemeTokens.current.colors.textSecondary,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@StringRes
private fun toolExitPromptMessageRes(route: String): Int =
    when (route) {
        AppDestination.DeviceInfo.route -> R.string.exit_prompt_device_info
        AppDestination.JunkClean.route -> R.string.exit_prompt_junk_removal
        AppDestination.BatteryInfo.route -> R.string.exit_prompt_battery_info
        AppDestination.NetworkScan.route -> R.string.exit_prompt_network_scan
        AppDestination.NetworkUsage.route -> R.string.exit_prompt_network_usage
        else -> R.string.exit_prompt_device_info
    }
