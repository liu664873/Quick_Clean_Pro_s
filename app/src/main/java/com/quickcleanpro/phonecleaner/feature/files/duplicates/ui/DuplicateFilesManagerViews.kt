package com.quickcleanpro.phonecleaner.feature.files.duplicates.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*


import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBlue
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFileEntry
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateGroupItem
import com.quickcleanpro.phonecleaner.feature.files.duplicates.duplicateFileKey
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerDivider
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerListBottomPadding
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerMutedNavy
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerNavy
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.SelectionCircle
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter

@Composable
internal fun DuplicateFilesGroupListView(
    groups: List<DuplicateGroupItem>,
    selectedFileKeys: Set<String>,
    allSelected: Boolean,
    showWarning: Boolean,
    scrollState: ScrollState,
    onToggleAll: () -> Unit,
    onOpenGroup: (DuplicateGroupItem) -> Unit,
    onToggleGroupSelection: (DuplicateGroupItem) -> Unit,
    onAcceptWarning: () -> Unit,
) {
    val duplicateFileCount = remember(groups) { groups.sumOf { it.files.size } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = FileManagerListBottomPadding)
    ) {
        if (showWarning) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.file_duplicate_warning),
                        color = Color(0xFF7D8EA8),
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onAcceptWarning()
                        },
                        modifier = Modifier
                            .width(72.dp)
                            .height(36.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanXBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(stringResource(R.string.file_got_it), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(duplicateFileCount.toString(), color = FileManagerNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(5.dp))
                Text(stringResource(R.string.nav_duplicate_files), color = FileManagerMutedNavy, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(stringResource(R.string.file_select_all), color = FileManagerNavy, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                SelectionCircle(selected = allSelected, modifier = Modifier.clickable { onToggleAll() })
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            groups.chunked(2).forEach { rowGroups ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowGroups.forEach { group ->
                        DuplicateGroupCard(
                            group = group,
                            selected = group.files.any { duplicateFileKey(it) in selectedFileKeys },
                            onOpen = { onOpenGroup(group) },
                            onToggleSelection = { onToggleGroupSelection(group) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(2 - rowGroups.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun DuplicateGroupCard(
    group: DuplicateGroupItem,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onOpen() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(154.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEAF0F7))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF9AA7BA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.duplicateCount.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            SelectionCircle(
                selected = selected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clickable { onToggleSelection() }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                DuplicateFileIcon(modifier = Modifier.size(44.dp))
            }
            Text(
                text = group.sizeLabel,
                color = FileManagerNavy,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = group.name,
            color = FileManagerNavy,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = pluralStringResource(
                R.plurals.duplicate_files_count,
                group.duplicateCount,
                group.duplicateCount
            ),
            color = FileManagerMutedNavy,
            fontSize = 10.sp,
            lineHeight = 12.sp,
        )
    }
}

@Composable
internal fun DuplicateFilesGroupDetailView(
    group: DuplicateGroupItem,
    selectedFileKeys: Set<String>,
    scrollState: ScrollState,
    onToggleFile: (DuplicateFileEntry) -> Unit,
    onAutoSelect: () -> Unit,
    onToggleGroupSelection: () -> Unit,
) {
    val selectedGroupSize = group.files
        .filter { duplicateFileKey(it) in selectedFileKeys }
        .sumOf { it.realFile?.sizeBytes ?: 0L }
    val selectedGroupSizeLabel = remember(selectedGroupSize) { FileSizeFormatter.format(selectedGroupSize) }
    val groupKeys = group.files.map(::duplicateFileKey).toSet()
    val recommendedKeys = group.files.drop(1).map(::duplicateFileKey).toSet()
    val selectedGroupKeys = selectedFileKeys.intersect(groupKeys)
    val autoSelectedExactly = recommendedKeys.isNotEmpty() && selectedGroupKeys == recommendedKeys

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = FileManagerListBottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(42.dp))
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEAF0F7)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                DuplicateFileIcon(modifier = Modifier.size(44.dp))
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            group.name,
            color = FileManagerNavy,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.files.size.toString(), color = FileManagerNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.nav_duplicate_files),
                        color = FileManagerMutedNavy,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedGroupSizeLabel, color = FileManagerMutedNavy, fontSize = 10.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    DuplicateOutlineButton(
                        text = stringResource(if (autoSelectedExactly) R.string.file_unselect else R.string.file_auto_select),
                        onClick = if (autoSelectedExactly) onToggleGroupSelection else onAutoSelect,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                group.files.forEachIndexed { index, file ->
                    val fileKey = duplicateFileKey(file)
                    DuplicateFileRow(
                        file = file,
                        selected = fileKey in selectedFileKeys,
                        onToggleSelection = { onToggleFile(file) }
                    )
                    if (index != group.files.lastIndex) {
                        FileManagerDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun DuplicateFileRow(
    file: DuplicateFileEntry,
    selected: Boolean,
    onToggleSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DuplicateFileIcon(modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.file_path_label, file.path),
                color = FileManagerNavy,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${file.date} ${file.sizeLabel}", color = FileManagerMutedNavy, fontSize = 14.sp, lineHeight = 17.sp)
            if (file.note != null) {
                Text(
                    localizedDuplicateFileNote(file.note),
                    color = CleanXBlue,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            }
        }
        SelectionCircle(
            selected = selected,
            modifier = Modifier.clickable { onToggleSelection() }
        )
    }
}

@Composable
private fun DuplicateOutlineButton(
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    ) {
        Text(text, color = CleanXBlue, fontSize = 12.sp)
    }
}

@Composable
private fun localizedDuplicateFileNote(note: String): String =
    when (note) {
        "Removal not recommended" -> stringResource(R.string.file_removal_not_recommended)
        else -> note
    }

@Composable
private fun DuplicateFileIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_file_blue),
        contentDescription = null,
        modifier = modifier
    )
}
