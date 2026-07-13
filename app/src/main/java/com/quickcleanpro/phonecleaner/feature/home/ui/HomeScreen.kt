package com.quickcleanpro.phonecleaner.feature.home.ui

import com.quickcleanpro.phonecleaner.feature.home.*

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.home.HomeSummaryUiState
import com.quickcleanpro.phonecleaner.feature.home.HomeViewModel
import com.quickcleanpro.phonecleaner.feature.home.ui.ToolNotificationSpec
import com.quickcleanpro.phonecleaner.common.ui.theme.LocalAppThemeTokens
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXIconButtonSize
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXPagePadding
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXPillShape
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXSegmentTabs
import com.quickcleanpro.phonecleaner.common.ui.components.cleanXDebouncedClick
import com.quickcleanpro.phonecleaner.feature.settings.ui.SettingsRateDialog
import com.quickcleanpro.phonecleaner.feature.settings.ui.openGooglePlayRatePage
import com.quickcleanpro.phonecleaner.common.ui.components.stableNavigationBarsPadding
import com.quickcleanpro.phonecleaner.common.ui.components.stableStatusBarsPadding
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXBlue
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXText
import kotlinx.coroutines.launch
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.app.navigation.feature.destinationOrNull

private val PrimaryText: Color @Composable @ReadOnlyComposable get() = LocalAppThemeTokens.current.colors.textPrimary

private data class HomeTab(
    val label: String,
    val iconRes: Int,
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    initialTabIndex: Int = 0,
) {
    val summaryState = state.summary
    val exitPromptSpec = state.exitPromptRoute?.let(::toolNotificationSpec)
    val showAutoRateDialog = state.showAutoRateDialog

    fun openFeature(feature: FeatureKey) {
        feature.destinationOrNull()?.let(onNavigate)
    }

    BackHandler(
        enabled = exitPromptSpec == null && !state.externalBlockingPromptActive && !showAutoRateDialog,
        onBack = { onAction(HomeAction.RequestExitPrompt) },
    )

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

    Box(modifier = modifier.fillMaxSize()) {
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
                    onSettingsClick = { onNavigate(AppDestination.Settings) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                CleanXSegmentTabs(
                    tabs = tabs.map { it.label },
                    selectedIndex = pagerState.currentPage,
                    onSelected = { index ->
                        val fromIndex = pagerState.currentPage
                        onAction(HomeAction.TabInteracted)
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
                                onFeatureClick = ::openFeature,
                            )
                        1 ->
                            FilesManagerTabContent(
                                onFeatureClick = ::openFeature,
                            )
                        2 ->
                            ToolBoxTabContent(
                                summaryState = summaryState,
                                onFeatureClick = ::openFeature,
                            )
                    }
                }
            }
        }
    }

    if (exitPromptSpec != null) {
        HomeExitPromptDialog(
            spec = exitPromptSpec,
            onExit = {
                onAction(HomeAction.DismissExitPrompt)
                onAction(HomeAction.ExitApp)
            },
            onViewNow = {
                state.exitPromptRoute
                    ?.let(AppDestination::forRoute)
                    ?.let { destination ->
                        onAction(HomeAction.DismissExitPrompt)
                        onNavigate(destination)
                    }
            },
        )
    }

    if (showAutoRateDialog && !state.externalBlockingPromptActive && exitPromptSpec == null) {
        SettingsRateDialog(
            onDismiss = { onAction(HomeAction.DismissAutoRateDialog) },
            onRate = { onAction(HomeAction.RateApp) },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
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
