package com.quickcleanpro.phonecleaner.use.skin.toolbox.whatsappcleaner

import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.whatsappcleaner.WhatsAppCleanerViewModel

import androidx.compose.runtime.Composable
import com.quickcleanpro.phonecleaner.use.skin.toolbox.whatsappcleaner.views.WhatsAppCleanerScreenState
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies

@Composable
fun WhatsAppCleanerScreen(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: WhatsAppCleanerViewModel) {
    WhatsAppCleanerScreenState(navigator = navigator, dependencies = dependencies, viewModel = viewModel)
}
