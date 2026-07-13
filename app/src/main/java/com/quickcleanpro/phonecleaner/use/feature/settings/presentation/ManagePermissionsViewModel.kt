package com.quickcleanpro.phonecleaner.use.feature.settings.presentation

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

import android.content.Context
import androidx.lifecycle.ViewModel
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagePermissionRowState(
    val labelRes: Int,
    val item: PermissionType,
    val checked: Boolean,
)

data class ManagePermissionsUiState(
    val rows: List<ManagePermissionRowState> = emptyList(),
)

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

    fun load(context: Context) {
        refresh(context)
    }

    fun refresh(context: Context) {
        val appContext = context.applicationContext ?: context
        _uiState.value = ManagePermissionsUiState(rows = buildRows(appContext))
    }

    fun onResume(context: Context) {
        refresh(context)
    }

    private fun buildRows(context: Context): List<ManagePermissionRowState> {
        val manager = CleanXPermissionRegistry.permissionItemManager(context)
        return quickCleanProManageItems.map { item ->
            ManagePermissionRowState(
                labelRes = item.labelRes,
                item = item.item,
                checked = manager.status(context, item.item).granted,
            )
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
