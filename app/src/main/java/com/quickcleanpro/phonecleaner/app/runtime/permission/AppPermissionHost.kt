package com.quickcleanpro.phonecleaner.app.runtime.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.quickcleanpro.phonecleaner.common.permission.CleanXPermissionRegistry
import com.quickcleanpro.phonecleaner.common.permission.PermissionPreferences
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.common.permission.runtime.CleanXPermissionCoordinatorState
import com.quickcleanpro.phonecleaner.common.permission.ui.CleanXPermissionPromptHost
import com.quickcleanpro.phonecleaner.common.permission.ui.LocalPermissionCoordinator
import com.quickcleanpro.phonecleaner.common.ui.permission.QuickCleanProPermissionUi
import org.koin.compose.koinInject

@Composable
internal fun AppPermissionHost(
    externalActivityLauncher: ExternalActivityLauncher,
    onPermissionFlowActiveChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissionPreferences = koinInject<PermissionPreferences>()
    val actionManager =
        remember(permissionPreferences) {
            CleanXPermissionRegistry.protectedActionPermissionManager(permissionPreferences)
        }
    val itemManager =
        remember(permissionPreferences) {
            CleanXPermissionRegistry.permissionItemManager(permissionPreferences)
        }
    val coordinator =
        remember(context, actionManager, itemManager) {
            CleanXPermissionCoordinatorState(context, actionManager, itemManager)
        }
    val latestOnPermissionFlowActiveChange =
        rememberUpdatedState(onPermissionFlowActiveChange)

    LaunchedEffect(coordinator.session != null) {
        latestOnPermissionFlowActiveChange.value(coordinator.session != null)
    }

    CompositionLocalProvider(LocalPermissionCoordinator provides coordinator) {
        content()
        CleanXPermissionPromptHost(
            state = coordinator,
            externalActivityLaunchHandler = externalActivityLauncher,
            permissionPrompt = QuickCleanProPermissionUi::PermissionPrompt,
        )
    }
}
