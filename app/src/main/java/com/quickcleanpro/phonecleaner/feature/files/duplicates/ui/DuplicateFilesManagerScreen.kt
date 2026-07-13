package com.quickcleanpro.phonecleaner.feature.files.duplicates.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*


import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFilesManagerViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXBottomActionBar
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerScaffold
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveBackForPermissionRejected
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveBackWithReturnAd
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHome
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHomeWithReturnAd
import com.quickcleanpro.phonecleaner.feature.files.shared.rememberFileManagerPermissionState
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowDialogs
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowEffects
import com.quickcleanpro.phonecleaner.feature.files.duplicates.ui.DuplicateFilesManagerContentView
import com.quickcleanpro.phonecleaner.common.format.FileSizeFormatter
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFilesManagerUiState
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateGroupItem
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFileEntry
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFilesAction

@Composable
internal fun DuplicateFilesManagerScreen(
    state: DuplicateFilesManagerUiState,
    permissionGranted: Boolean,
    onAction: (DuplicateFilesAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupListScrollState = remember { ScrollState(0) }
    val groupDetailScrollStates = remember { mutableMapOf<Int, ScrollState>() }
    fun scrollStateForGroup(groupId: Int): ScrollState =
        groupDetailScrollStates.getOrPut(groupId) { ScrollState(0) }
    val selectedGroup = state.selectedGroup

    BackHandler { onAction(DuplicateFilesAction.Back) }
    FileManagerScaffold(
        title = stringResource(R.string.nav_duplicate_files),
        onBack = { onAction(DuplicateFilesAction.Back) },
        bottomBar = {
            if (permissionGranted &&
                (state.phase == FileOperationPhase.Browsing || state.phase == FileOperationPhase.ConfirmDelete)
            ) {
                val cleanupEnabled = if (selectedGroup == null) {
                    state.globalFilesToDelete.isNotEmpty()
                } else state.selectedGroupFilesToDelete.isNotEmpty()
                val cleanupSize = if (selectedGroup == null) {
                    state.selectedDeleteSize
                } else state.selectedGroupDeleteSize
                CleanXBottomActionBar(
                    enabled = cleanupEnabled,
                    text = stringResource(R.string.file_clean_up_size, formatDuplicateCleanupSize(cleanupSize)),
                    onClick = {
                        onAction(
                            if (selectedGroup == null) DuplicateFilesAction.RequestDelete
                            else DuplicateFilesAction.RequestCurrentGroupDelete,
                        )
                    },
                    backgroundColor = Color.Transparent,
                    buttonModifier = Modifier.height(52.dp),
                    buttonCornerRadius = 10.dp,
                    buttonFontSize = 20.sp,
                )
            }
        },
    ) {
        DuplicateFilesManagerContentView(
            uiState = state,
            groupListScrollState = groupListScrollState,
            scrollStateForGroup = ::scrollStateForGroup,
            onToggleAll = { onAction(DuplicateFilesAction.ToggleAll) },
            onOpenGroup = { onAction(DuplicateFilesAction.OpenGroup(it)) },
            onToggleFile = { onAction(DuplicateFilesAction.ToggleFile(it)) },
            onAutoSelect = { onAction(DuplicateFilesAction.AutoSelect) },
            onToggleGroupSelection = { onAction(DuplicateFilesAction.ToggleGroupSelection(it)) },
            onAcceptWarning = { onAction(DuplicateFilesAction.AcceptWarning) },
            onNavigateTool = onNavigate,
            onContinue = { onAction(DuplicateFilesAction.ContinueManaging) },
        )
    }
}

private fun formatDuplicateCleanupSize(bytes: Long): String =
    if (bytes == 0L) "0KB" else FileSizeFormatter.format(bytes)
