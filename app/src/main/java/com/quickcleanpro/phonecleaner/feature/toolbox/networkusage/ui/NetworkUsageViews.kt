package com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.ui

import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.*


import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXPrimaryTabs
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXTabItem
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.common.ui.components.PackageAppIcon
import com.quickcleanpro.phonecleaner.common.ui.components.RoundedProgressBar
import com.quickcleanpro.phonecleaner.common.ui.components.popups.StopScanDialog
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXBlue
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.ui.ToolboxScanningContent
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.NetworkUsageDisplayItem
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.NetworkUsageTab
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.NetworkUsageUiState
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val CardBg = Color.White
private val Navy = Color(0xFF2D3748)
private val TitleNavy = Color(0xFF2D3748)
private val NavyMuted = Color(0xFF8190A5)
private val ValueMuted = Color(0xFF8190A5)
private val CardRadius = 10.dp

@Composable
internal fun NetworkUsageScreenState(
    uiState: NetworkUsageUiState,
    showScanning: Boolean,
    showStopDialog: Boolean,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onQuitScan: () -> Unit,
    onResumeScan: () -> Unit,
) {
    val pagerState =
        rememberPagerState(
            initialPage = uiState.selectedIndex,
            pageCount = { NetworkUsageTab.entries.size },
        )
    val scope = rememberCoroutineScope()
    val visualSelectedPage = pagerState.currentPage.coerceIn(0, NetworkUsageTab.entries.lastIndex)
    LaunchedEffect(pagerState, onTabSelected) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != uiState.selectedIndex) onTabSelected(page)
            }
    }

    CleanXScaffoldPage(
        title = stringResource(R.string.network_usage),
        onBack = onBack,
        contentPadding =
            if (uiState.hasAccess && showScanning) {
                PaddingValues(0.dp)
            } else {
                PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            },
        scrollEnabled = !(uiState.hasAccess && showScanning),
    ) {
        if (uiState.hasAccess && showScanning) {
            ToolboxScanningContent(
                centerIconRes = R.drawable.ic_scan_network_usage,
                captionText = stringResource(R.string.network_usage),
            )
        } else if (uiState.hasAccess) {
            NetworkUsageTabs(
                uiState = uiState,
                selectedIndex = visualSelectedPage,
                onSelected = { index ->
                    if (index != visualSelectedPage) {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                NetworkUsagePageContent(
                    uiState = uiState,
                    tab = NetworkUsageTab.entries[page],
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showStopDialog) {
        StopScanDialog(
            onQuit = onQuitScan,
            onResume = onResumeScan,
        )
    }
}

@Composable
private fun NetworkUsagePageContent(
    uiState: NetworkUsageUiState,
    tab: NetworkUsageTab,
) {
    when {
        uiState.isLoading && uiState.usage == null -> Unit
        uiState.totalBytes(tab) <= 0L -> EmptyNetworkState()
        else -> {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                NetworkSummaryCard(uiState = uiState, tab = tab)
                Spacer(modifier = Modifier.height(16.dp))
                NetworkAppList(items = uiState.displayItems(tab))
            }
        }
    }
}

@Composable
private fun NetworkUsageTabs(
    uiState: NetworkUsageUiState,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    CleanXPrimaryTabs(
        items =
            listOf(
                CleanXTabItem(
                    title = stringResource(R.string.cellular_today),
                    value = formatNetworkBytes(uiState.cellularTotalBytes),
                ),
                CleanXTabItem(
                    title = stringResource(R.string.wifi_today),
                    value = formatNetworkBytes(uiState.wifiTotalBytes),
                ),
            ),
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        containerColor = Color.Transparent,
    )
}

@Composable
private fun EmptyNetworkState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 74.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.blank),
            contentDescription = null,
            modifier = Modifier.size(230.dp),
            contentScale = ContentScale.Fit,
        )
    }
}


@Composable
private fun NetworkSummaryCard(
    uiState: NetworkUsageUiState,
    tab: NetworkUsageTab,
) {
    val usage = uiState.usage
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(CardRadius),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(86.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryColumn(
                value = formatNetworkBytes(uiState.totalBytes(tab)),
                label =
                    stringResource(
                        if (usage?.isToday == false) {
                            R.string.total_usage_since_boot
                        } else {
                            R.string.total_usage_today
                        },
                    ).replace('\n', ' '),
            )
            SummaryDivider()
            SummaryColumn(
                value = formatNetworkBytes(uiState.rxBytes(tab)),
                label =
                    stringResource(
                        if (usage?.isToday == false) {
                            R.string.downloads_since_boot
                        } else {
                            R.string.downloads_today
                        },
                    ).replace('\n', ' '),
            )
            SummaryDivider()
            SummaryColumn(
                value = formatNetworkBytes(uiState.txBytes(tab)),
                label =
                    stringResource(
                        if (usage?.isToday == false) {
                            R.string.uploads_since_boot
                        } else {
                            R.string.uploads_today
                        },
                    ).replace('\n', ' '),
            )
        }
    }
}

@Composable
private fun SummaryDivider() {
    Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color(0xFFD8DEE7)))
}

@Composable
private fun SummaryColumn(
    value: String,
    label: String,
) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = value,
            color = Navy,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            color = NavyMuted,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NetworkAppList(items: List<NetworkUsageDisplayItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(CardRadius),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val maxUsage = items.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
            items.forEachIndexed { index, item ->
                NetworkAppRow(
                    item = item,
                    progress = (item.totalBytes.toFloat() / maxUsage).coerceIn(0.08f, 1f),
                )
            }
        }
    }
}

@Composable
private fun NetworkAppRow(
    item: NetworkUsageDisplayItem,
    progress: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PackageAppIcon(
            packageName = item.packageName,
            fallbackText = if (item.isAggregate) "S" else item.appName.take(1).ifBlank { "A" },
            color = CleanXBlue,
            isAggregate = item.isAggregate,
            modifier = Modifier.size(44.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text =
                        if (item.isAggregate) {
                            stringResource(R.string.system_unknown_traffic)
                        } else {
                            item.appName
                        },
                    color = TitleNavy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatNetworkBytes(item.totalBytes, compact = true),
                    color = ValueMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
            }
            RoundedProgressBar(
                progress = progress,
                width = 209.dp,
                height = 4.dp,
                trackColor = Navy.copy(alpha = 0.15f),
                fillColor = CleanXBlue,
            )
        }
    }
}

private fun formatNetworkBytes(
    bytes: Long,
    compact: Boolean = false,
): String {
    val value = FileSizeFormatter.format(bytes.coerceAtLeast(0L))
    return if (compact) value.replace(" ", "") else value
}
