package com.quickcleanpro.phonecleaner.feature.files.shared.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBlue
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXPrimaryTabs
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXTabItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileImageDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileListDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.FileListDisplayStyle
import com.quickcleanpro.phonecleaner.feature.files.shared.FileListIconKind
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerTabDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.shared.filterMediaGridItems
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerRealImage

@Composable
internal fun FileManagerTopAction(
    actionText: String?,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
) {
    if (actionText == null) return

    Text(
        text = actionText,
        color = CleanXBlue.copy(alpha = if (actionEnabled) 1f else 0.45f),
        fontSize = 16.sp,
        modifier = Modifier.clickable(enabled = actionEnabled) { onAction() },
    )
}

@Composable
internal fun FileManagerMediaGridView(
    tabs: List<FileManagerTabDisplayItem>,
    items: List<FileImageDisplayItem>,
    selectedIds: Set<Int>,
    scrollState: ScrollState,
    onToggleVisibleItems: (Set<Int>) -> Unit,
    onSelect: (Int) -> Unit,
    onOpenDetail: (FileImageDisplayItem) -> Unit,
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    showPlayBadge: Boolean = true,
) {
    val visibleItems = remember(items, selectedTabIndex) {
        filterMediaGridItems(tabs.getOrNull(selectedTabIndex)?.title.orEmpty(), items)
    }
    val visibleIds = remember(visibleItems) { visibleItems.map { it.id }.toSet() }
    val allSelected = visibleItems.isNotEmpty() && selectedIds.containsAll(visibleItems.map { it.id })
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = FileManagerListBottomPadding)
    ) {
        CleanXPrimaryTabs(
            items = tabs.map { tab ->
                CleanXTabItem(
                    title = localizedFileManagerTabTitle(tab.title),
                    value = tab.sizeLabel,
                )
            },
            selectedIndex = selectedTabIndex,
            onSelected = onTabSelected,
            containerColor = Color.Transparent,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FileManagerCardColor,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FileManagerListCardHeader(
                    count = visibleItems.size,
                    selected = allSelected,
                    onToggleAll = { onToggleVisibleItems(visibleIds) },
                    title = stringResource(R.string.file_items),
                )
                visibleItems.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            MediaGridTile(
                                item = item,
                                selected = item.id in selectedIds,
                                onOpen = { onOpenDetail(item) },
                                onToggleSelection = { onSelect(item.id) },
                                showPlayBadge = showPlayBadge,
                                modifier = Modifier
                                    .weight(1f)
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FileManagerListView(
    tabs: List<FileManagerTabDisplayItem>,
    items: List<FileListDisplayItem>,
    selectedTabIndex: Int,
    selectedIds: Set<Int>,
    allSelected: Boolean,
    scrollState: ScrollState,
    style: FileListDisplayStyle,
    onTabSelected: (Int) -> Unit,
    onToggleAll: () -> Unit,
    onSelect: (Int) -> Unit,
    onOpenDetail: (FileListDisplayItem) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = FileManagerListBottomPadding)
    ) {
        CleanXPrimaryTabs(
            items = tabs.map { tab ->
                CleanXTabItem(
                    title = localizedFileManagerTabTitle(tab.title),
                    value = tab.sizeLabel,
                )
            },
            selectedIndex = selectedTabIndex,
            onSelected = onTabSelected,
            containerColor = Color.Transparent,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FileManagerCardColor,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FileManagerListCardHeader(
                    count = items.size,
                    selected = allSelected,
                    onToggleAll = onToggleAll,
                    title = stringResource(R.string.file_items),
                )

                Spacer(modifier = Modifier.height(14.dp))

                items.forEachIndexed { index, item ->
                    FileManagerListRow(
                        item = item,
                        selected = item.id in selectedIds,
                        style = style,
                        onOpen = { onOpenDetail(item) },
                        onToggleSelection = { onSelect(item.id) }
                    )
                    if (index != items.lastIndex) {
                        FileManagerDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun FileManagerAudioListView(
    tabs: List<FileManagerTabDisplayItem>,
    items: List<FileImageDisplayItem>,
    selectedIds: Set<Int>,
    scrollState: ScrollState,
    onToggleVisibleItems: (Set<Int>) -> Unit,
    onSelect: (Int) -> Unit,
    onOpenDetail: (FileImageDisplayItem) -> Unit,
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    val visibleItems = remember(items, selectedTabIndex) {
        filterMediaGridItems(tabs.getOrNull(selectedTabIndex)?.title.orEmpty(), items)
    }
    val visibleIds = remember(visibleItems) { visibleItems.map { it.id }.toSet() }
    val allSelected = visibleItems.isNotEmpty() && selectedIds.containsAll(visibleIds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = FileManagerListBottomPadding)
    ) {
        CleanXPrimaryTabs(
            items = tabs.map { tab ->
                CleanXTabItem(
                    title = localizedFileManagerTabTitle(tab.title),
                    value = tab.sizeLabel,
                )
            },
            selectedIndex = selectedTabIndex,
            onSelected = onTabSelected,
            containerColor = Color.Transparent,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FileManagerCardColor,
            shape = RoundedCornerShape(10.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FileManagerListCardHeader(
                    count = visibleItems.size,
                    selected = allSelected,
                    onToggleAll = { onToggleVisibleItems(visibleIds) },
                    title = stringResource(R.string.file_items),
                )

                Spacer(modifier = Modifier.height(14.dp))

                visibleItems.forEachIndexed { index, item ->
                    FileManagerMediaFileRow(
                        item = item,
                        selected = item.id in selectedIds,
                        iconResId = R.drawable.ic_audio_yellow,
                        onOpen = { onOpenDetail(item) },
                        onToggleSelection = { onSelect(item.id) },
                    )
                    if (index != visibleItems.lastIndex) {
                        FileManagerDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun FileManagerListRow(
    item: FileListDisplayItem,
    selected: Boolean,
    style: FileListDisplayStyle,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val isDocumentsStyle = style == FileListDisplayStyle.Documents
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onOpen() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDocumentsStyle) {
            Image(
                painter = painterResource(id = R.drawable.ic_file_yellow),
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
        } else {
            FileManagerItemTypeIcon(kind = item.iconKind, modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.width(if (isDocumentsStyle) 14.dp else 12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.name,
                color = FileManagerNavy,
                fontSize = if (isDocumentsStyle) 17.sp else 18.sp,
                lineHeight = if (isDocumentsStyle) 21.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.meta,
                color = if (isDocumentsStyle) Color(0xFF7F91AA) else FileManagerMutedNavy,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SelectionCircle(
            selected = selected,
            modifier = Modifier.clickable { onToggleSelection() }
        )
    }
}

@Composable
internal fun FileManagerItemTypeIcon(
    kind: FileListIconKind,
    modifier: Modifier = Modifier
) {
    val colors = when (kind) {
        FileListIconKind.LargeVideo -> listOf(Color(0xFFD92BFF), Color(0xFF921CF0))
        FileListIconKind.Document -> listOf(Color(0xFFFF943F), Color(0xFFFF7A21))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (kind) {
                FileListIconKind.LargeVideo -> Icons.Default.PlayArrow
                FileListIconKind.Document -> Icons.AutoMirrored.Filled.InsertDriveFile
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun FileManagerListCardHeader(
    count: Int,
    selected: Boolean,
    onToggleAll: () -> Unit,
    title: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = count.toString(),
            color = FileManagerNavy,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            color = FileManagerMutedNavy,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.file_select_all),
            color = FileManagerNavy,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        SelectionCircle(selected = selected, modifier = Modifier.clickable { onToggleAll() })
    }
}

@Composable
internal fun FileManagerDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FileManagerDividerColor)
    )
}

@Composable
private fun MediaGridTile(
    item: FileImageDisplayItem,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    showPlayBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(item.colors))
            .clickable { onOpen() }
    ) {
        FileManagerRealImage(item = item)
        if (showPlayBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(31.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        SelectionCircle(
            selected = selected,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clickable { onToggleSelection() },
        )
    }
}

@Composable
private fun FileManagerMediaFileRow(
    item: FileImageDisplayItem,
    selected: Boolean,
    iconResId: Int,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onOpen() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.name.ifBlank { item.sizeLabel },
                color = FileManagerNavy,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.meta.ifBlank { item.sizeLabel },
                color = FileManagerMutedNavy,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SelectionCircle(
            selected = selected,
            modifier = Modifier.clickable { onToggleSelection() },
        )
    }
}

