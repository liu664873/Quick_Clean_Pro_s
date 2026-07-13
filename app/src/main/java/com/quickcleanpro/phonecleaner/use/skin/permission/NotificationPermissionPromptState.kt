package com.quickcleanpro.phonecleaner.use.skin.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quickcleanpro.phonecleaner.use.core.permission.NotificationRuntimePermissionController
import com.quickcleanpro.phonecleaner.use.core.repository.SettingsRepository

class NotificationPermissionPromptState {
    var splashPermissionActive by mutableStateOf(false)

    var notificationPermissionUiActive by mutableStateOf(false)

    fun shouldPauseSplashForInitialNotificationRequest(
        currentRoute: String?,
        splashRoute: String,
        settingsRepository: SettingsRepository,
        permissionController: NotificationRuntimePermissionController,
    ): Boolean =
        (currentRoute == null || currentRoute == splashRoute) &&
            !permissionController.hasPostNotificationsPermission() &&
            !settingsRepository.hasRequestedNotificationRuntimePermissionBefore()
}

@Composable
fun rememberNotificationPermissionPromptState(): NotificationPermissionPromptState =
    remember { NotificationPermissionPromptState() }
