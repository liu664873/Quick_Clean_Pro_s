package com.quickcleanpro.phonecleaner.use.skin.junkclean.screens.views

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.model.clean.CategoryCleanGroup
import com.quickcleanpro.phonecleaner.use.core.model.clean.CleanItem
import com.quickcleanpro.phonecleaner.use.core.model.clean.JunkCategory
import com.quickcleanpro.phonecleaner.use.core.model.clean.JunkFile
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXBlue
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.use.skin.common.components.stableNavigationBarsPadding
import com.quickcleanpro.phonecleaner.use.feature.junkclean.presentation.SelectionSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val CleanXText = Color(0xFF2D3748)
private val CleanXMutedText = Color(0xFF8190A5)
private val ResultCardBg = Color.White
private val ResultNavy = Color(0xFF2D3748)
private val ResultOrange = Color(0xFFFC7941)
private val ResultDivider = Color(0xFFE2EAF3)
private val ResultIconBg = Color(0xFFEAF7FE)

@Composable
internal fun JunkScanResultBottomBar(
    selectedSummary: SelectionSummary,
    onClean: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .stableNavigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val onlyZeroByteSelection =
            selectedSummary.checkedCount == 0 && selectedSummary.checkedEmptyCategoryCount > 0
        val buttonText =
            when {
                selectedSummary.checkedCount > 0 ->
                    stringResource(R.string.remove_size, compactSizeLabel(selectedSummary.checkedSize))
                onlyZeroByteSelection -> stringResource(R.string.result_zero_byte_selection_button)
                else -> stringResource(R.string.result_select_items_button)
            }
        if (onlyZeroByteSelection) {
            Text(
                text = stringResource(R.string.result_zero_byte_selection_hint),
                color = CleanXMutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
            )
        }
        CleanXPrimaryButton(
            text = buttonText,
            onClick = onClean,
            enabled = selectedSummary.checkedCount > 0,
            height = 50.dp,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun JunkScanResultView(
    groups: List<CategoryCleanGroup>,
    checkedEmptyCategories: Set<JunkCategory>,
    selectedSummary: SelectionSummary,
    onToggleCategorySelection: (List<JunkCategory>) -> Unit,
    onToggleItem: (CleanItem) -> Unit,
) {
    val rows = displayGroups(groups)
    var expandedIndex by remember { mutableStateOf(-1) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SummaryCard(totalSize = selectedSummary.checkedSize)
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ResultCardBg,
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                    rows.forEachIndexed { rowIndex, row ->
                        val items = row.group.items
                        CategoryGroupSection(
                            row = row,
                            checked =
                                row.group.items.takeIf { it.isNotEmpty() }?.all { it.isChecked }
                                    ?: (row.group.category in checkedEmptyCategories),
                            expanded = expandedIndex == rowIndex && items.isNotEmpty(),
                            onToggleExpanded = {
                                expandedIndex = if (expandedIndex == rowIndex) -1 else rowIndex
                            },
                            onToggleCategorySelection = {
                                onToggleCategorySelection(row.categories)
                            },
                            onToggleItem = onToggleItem,
                        )
                        if (rowIndex < rows.lastIndex) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(ResultDivider),
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
internal fun AwaitingAuthorizationView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.authorization_required),
            color = CleanXText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = CleanXMutedText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
internal fun JunkCleanErrorView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.error),
            color = CleanXText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = CleanXMutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun SummaryCard(totalSize: Long) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(size = 12.dp))
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            .background(Color(0xFFFFFFFF))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().background(Color(0xFFFFFFFF)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_junk_clean_scan_result_tran),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(1.33333.dp)
                    .width(62.dp)
                    .height(62.dp)
            )

            Spacer(modifier = Modifier.width(26.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayMainSize(totalSize),
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 33.85.sp,
                            fontWeight = FontWeight(700),
                            color = Color(0xFFFF9529),
                            textAlign = TextAlign.Right,
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayUnit(totalSize),
                        style = TextStyle(
                            fontSize = 18.46.sp,
                            fontWeight = FontWeight(700),
                            color = Color(0xFF2D3748),
                            textAlign = TextAlign.Center,
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.occupying),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight(400),
                        color = Color(0xFF8190A5),
                    )
                )
            }
        }
    }
}
@Composable
private fun CategoryGroupSection(
    row: ResultDisplayRow,
    checked: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleCategorySelection: () -> Unit,
    onToggleItem: (CleanItem) -> Unit,
) {
    val group = row.group
    val items = group.items

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(row.titleRes),
                    color = ResultNavy,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier.widthIn(min = 120.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.formattedTotalSize,
                    color = ResultNavy,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ResultNavy,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable(enabled = items.isNotEmpty()) { onToggleExpanded() },
                )
                Spacer(modifier = Modifier.width(8.dp))
                RoundCheckButton(
                    checked = checked,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable { onToggleCategorySelection() },
                )
            }
        }

        if (expanded && items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items.forEach { item ->
                    JunkItemCard(
                        item = item,
                        modifier = Modifier.width(55.dp),
                        onClick = { onToggleItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun JunkItemCard(
    item: CleanItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ResultIconBg),
                contentAlignment = Alignment.Center,
            ) {
                JunkItemIcon(item = item)
            }
            RoundCheckButton(
                checked = item.isChecked,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-5).dp)
                        .size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.fileName.ifBlank { stringResource(R.string.unnamed_file) },
            color = Color(0xA62D3748),
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.formattedSize,
            color = Color(0xA62D3748),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun JunkItemIcon(item: CleanItem) {
    val context = LocalContext.current
    val filePath = item.junkFile.filePath
    val apkIcon by
        produceState<ImageBitmap?>(initialValue = apkIconBitmapCache[filePath], item.category, filePath) {
            value =
                if (item.category == JunkCategory.APK) {
                    withContext(Dispatchers.IO) { loadApkIconBitmap(context.applicationContext, filePath) }
                } else {
                    null
                }
        }

    val loadedApkIcon = apkIcon
    if (loadedApkIcon != null) {
        Image(
            bitmap = loadedApkIcon,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        FileGlyph(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun RoundCheckButton(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(if (checked) CleanXBlue else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color(0xFFC8D2DE),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun FileGlyph(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_junk_result_file),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

private fun displayMainSize(size: Long): String {
    val formatted = JunkFile.formatFileSize(size)
    return formatted.takeWhile { it.isDigit() || it == '.' }.ifBlank { "0" }
}

private fun displayUnit(size: Long): String {
    val formatted = JunkFile.formatFileSize(size)
    return formatted.dropWhile { it.isDigit() || it == '.' }.trim().ifBlank { "B" }
}

private fun compactSizeLabel(size: Long): String {
    val mainSize = displayMainSize(size)
    val unit = displayUnit(size)
    return "$mainSize$unit"
}

private data class ResultDisplayRow(
    @StringRes val titleRes: Int,
    val group: CategoryCleanGroup,
    val categories: List<JunkCategory>,
)

private val apkIconBitmapCache = ConcurrentHashMap<String, ImageBitmap>()

private fun loadApkIconBitmap(
    context: Context,
    filePath: String,
): ImageBitmap? {
    if (filePath.isBlank()) return null
    apkIconBitmapCache[filePath]?.let { return it }

    val packageManager = context.packageManager
    val packageInfo =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(
                    filePath,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(filePath, 0)
            }
        }.getOrNull() ?: return null

    val applicationInfo = packageInfo.applicationInfo ?: return null
    applicationInfo.sourceDir = filePath
    applicationInfo.publicSourceDir = filePath

    val drawable = runCatching { applicationInfo.loadIcon(packageManager) }.getOrNull() ?: return null
    return cacheApkIconBitmap(filePath, drawable.toBitmap().asImageBitmap())
}

private fun cacheApkIconBitmap(
    filePath: String,
    bitmap: ImageBitmap,
): ImageBitmap {
    apkIconBitmapCache[filePath] = bitmap
    return bitmap
}

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

private fun displayGroups(groups: List<CategoryCleanGroup>): List<ResultDisplayRow> {
    fun row(
        @StringRes titleRes: Int,
        category: JunkCategory,
    ): ResultDisplayRow {
        val index = groups.indexOfFirst { it.category == category }
        val group = groups.getOrNull(index) ?: CategoryCleanGroup(category, emptyList())
        return ResultDisplayRow(titleRes = titleRes, group = group, categories = listOf(category))
    }

    val residualGroup =
        mergeDisplayGroup(
            category = JunkCategory.RESIDUAL,
            groups = groups,
            categories = listOf(JunkCategory.RESIDUAL, JunkCategory.DUPLICATE),
        )
    val otherGroup =
        mergeDisplayGroup(
            category = JunkCategory.LARGE_FILE,
            groups = groups,
            categories = listOf(JunkCategory.LARGE_FILE),
        )

    return listOf(
        row(R.string.junk_group_system_cache, JunkCategory.CACHE),
        row(R.string.junk_group_ad_junk_files, JunkCategory.TEMP_FILE),
        ResultDisplayRow(
            R.string.junk_group_residual_junks,
            residualGroup,
            listOf(JunkCategory.RESIDUAL, JunkCategory.DUPLICATE),
        ),
        row(R.string.junk_group_obsolete_apks, JunkCategory.APK),
        ResultDisplayRow(
            R.string.junk_group_other_junk_files,
            otherGroup,
            listOf(JunkCategory.LARGE_FILE),
        ),
    )
}

private fun mergeDisplayGroup(
    category: JunkCategory,
    groups: List<CategoryCleanGroup>,
    categories: List<JunkCategory>,
): CategoryCleanGroup =
    CategoryCleanGroup(
        category = category,
        items = categories.flatMap { displayCategory ->
            groups.firstOrNull { it.category == displayCategory }?.items.orEmpty()
        },
    )

