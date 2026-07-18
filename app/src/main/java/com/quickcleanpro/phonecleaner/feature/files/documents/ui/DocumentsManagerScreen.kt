package com.quickcleanpro.phonecleaner.feature.files.documents.ui

import com.quickcleanpro.phonecleaner.feature.files.shared.*


import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.feature.files.shared.FileListDisplayItem
import com.quickcleanpro.phonecleaner.feature.files.documents.DocumentsListStyle
import com.quickcleanpro.phonecleaner.feature.files.documents.DocumentsManagerUiState
import com.quickcleanpro.phonecleaner.feature.files.documents.DocumentsManagerViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerDeleteBottomBar
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerScaffold
import com.quickcleanpro.phonecleaner.feature.files.shared.FileOperationPhase
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerListView
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerPageBrush
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveBackForPermissionRejected
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHome
import com.quickcleanpro.phonecleaner.feature.files.shared.leaveHomeWithReturnAd
import com.quickcleanpro.phonecleaner.feature.files.shared.rememberFileManagerPermissionState
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowDialogs
import com.quickcleanpro.phonecleaner.feature.files.shared.FileManagerFlowEffects
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileOperationPhaseContent
import com.quickcleanpro.phonecleaner.feature.files.shared.ui.FileManagerAction

@Composable
internal fun DocumentsManagerScreen(
    state: DocumentsManagerUiState,
    permissionGranted: Boolean,
    onAction: (FileManagerAction) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollStates = remember { mutableMapOf<Int, ScrollState>() }
    val scrollState = scrollStates.getOrPut(state.selectedTabIndex) { ScrollState(0) }
    val visibleItems = state.visibleDisplayItems
    val isDetailMode = state.detailStartIndex != null
    BackHandler { onAction(FileManagerAction.Back) }
    FileManagerScaffold(
        title = stringResource(R.string.nav_documents),
        onBack = { onAction(FileManagerAction.Back) },
        bottomBar = {
            if (permissionGranted && state.phase == FileOperationPhase.Browsing && !isDetailMode) {
                FileManagerDeleteBottomBar(
                    enabled = state.selectedIds.isNotEmpty(),
                    selectedSizeBytes = state.selectedSizeBytes,
                    onClick = { onAction(FileManagerAction.RequestDelete) },
                )
            }
        },
    ) {
        val result = state.resultSize
        Column(modifier = modifier.background(FileManagerPageBrush).padding(horizontal = 16.dp)) {
            FileOperationPhaseContent(
                phase = state.phase,
                scanningText = stringResource(R.string.file_scanning_documents),
                deletingText = stringResource(R.string.file_deleting_files),
                resultAmount = result.first,
                resultUnit = result.second,
                resultCaption = stringResource(R.string.file_deleted_in_cleanup),
                onNavigateTool = onNavigate,
                onContinue = { onAction(FileManagerAction.ContinueManaging) },
            ) {
                FileManagerListView(
                    tabs = state.displayTabs,
                    items = visibleItems,
                    selectedTabIndex = state.selectedTabIndex,
                    selectedIds = state.selectedIds,
                    allSelected = state.allSelected,
                    scrollState = scrollState,
                    style = DocumentsListStyle,
                    onTabSelected = { onAction(FileManagerAction.SelectTab(it)) },
                    onToggleAll = { onAction(FileManagerAction.ToggleVisibleItems) },
                    onSelect = { onAction(FileManagerAction.ToggleSelection(it)) },
                    onOpenDetail = { item ->
                        val index = visibleItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                        onAction(FileManagerAction.OpenDetail(index))
                    },
                )
            }
        }
    }
}

