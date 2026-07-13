package com.quickcleanpro.phonecleaner.use.skin.toolbox.notificationcleaner

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.notification.NotificationCleanerViewModel
import com.quickcleanpro.phonecleaner.use.skin.toolbox.notificationcleaner.views.NotificationCleanerScreenState
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies

@Composable
fun NotificationCleanerScreen(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: NotificationCleanerViewModel) {
    NotificationCleanerScreenState(navigator = navigator, dependencies = dependencies, viewModel = viewModel)
}
