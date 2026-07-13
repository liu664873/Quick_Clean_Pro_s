package com.quickcleanpro.phonecleaner.feature.settings

import com.quickcleanpro.phonecleaner.common.permission.PermissionType
import androidx.lifecycle.ViewModel
import com.quickcleanpro.phonecleaner.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ManagePermissionRowState(
    val labelRes: Int,
    val item: PermissionType,
    val checked: Boolean,
)

data class ManagePermissionsUiState(
    val rows: List<ManagePermissionRowState> = emptyList(),
)

sealed interface ManagePermissionsAction {
    data object Back : ManagePermissionsAction
    data object Refresh : ManagePermissionsAction
    data class PermissionClicked(val item: PermissionType) : ManagePermissionsAction
    data class StatusesChanged(val statuses: Map<PermissionType, Boolean>) : ManagePermissionsAction
}

private fun initialRows(): List<ManagePermissionRowState> =
    quickCleanProManageItems.map { item ->
        ManagePermissionRowState(
            labelRes = item.labelRes,
            item = item.item,
            checked = false,
        )
    }

class ManagePermissionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ManagePermissionsUiState(rows = initialRows()))
    val uiState: StateFlow<ManagePermissionsUiState> = _uiState.asStateFlow()

    fun onAction(action: ManagePermissionsAction) {
        when (action) {
            is ManagePermissionsAction.StatusesChanged -> {
                _uiState.value =
                    ManagePermissionsUiState(
                        rows = initialRows().map { row ->
                            row.copy(checked = action.statuses[row.item] == true)
                        },
                    )
            }
            ManagePermissionsAction.Refresh,
            ManagePermissionsAction.Back,
            is ManagePermissionsAction.PermissionClicked -> Unit
        }
    }
}

private data class QuickCleanProPermissionManageItem(
    val item: PermissionType,
    val labelRes: Int,
)

private val quickCleanProManageItems: List<QuickCleanProPermissionManageItem> =
    listOf(
        QuickCleanProPermissionManageItem(PermissionType.StorageFiles, R.string.settings_storage_permission),
        QuickCleanProPermissionManageItem(PermissionType.UsageAccess, R.string.settings_usage_data_permission),
        QuickCleanProPermissionManageItem(PermissionType.Location, R.string.settings_location_permission),
        QuickCleanProPermissionManageItem(
            PermissionType.NotificationListener,
            R.string.settings_notification_permission,
        ),
//        QuickCleanProPermissionManageItem(PermissionType.Overlay, R.string.settings_overlay_permission),
//        QuickCleanProPermissionManageItem(
//            PermissionType.PostNotifications,
//            R.string.settings_post_notifications_permission,
//        ),
    )
