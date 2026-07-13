package com.quickcleanpro.phonecleaner.feature.settings

import com.quickcleanpro.phonecleaner.feature.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ui.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.common.ui.components.cleanXPressable
import com.quickcleanpro.phonecleaner.feature.settings.ui.SettingsRateDialog
import com.quickcleanpro.phonecleaner.feature.settings.ui.TemperatureUnitDialog
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXCardColor
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXCardShape
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXContentPadding
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXMutedText
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXRowHeight
import com.quickcleanpro.phonecleaner.common.ui.theme.CleanXText
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.AppConfig
import com.quickcleanpro.phonecleaner.common.ads.AdPrivacyGateway
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.intent.openUrl
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.feature.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


import com.quickcleanpro.phonecleaner.feature.settings.ui.SettingsScreen
import com.quickcleanpro.phonecleaner.feature.settings.ui.openGooglePlayRatePage

@Composable
fun SettingsRoute(
    navigator: AppNavigator,
    externalActivities: ExternalActivityLauncher,
    viewModel: SettingsViewModel = koinViewModel(),
    adPrivacyGateway: AdPrivacyGateway = koinInject(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    fun openLink(url: String, track: () -> Unit) {
        externalActivities.markLaunch()
        track()
        if (!context.openUrl(url)) externalActivities.cancelLaunch()
    }

    fun openAdPrivacy() {
        val activity = context.findActivity()
        if (activity == null) {
            Log.w("SettingsScreen", "skip ad privacy options: activity unavailable")
        } else {
            runCatching { adPrivacyGateway.showPrivacyOptions(activity) }
                .onFailure { Log.w("SettingsScreen", "show ad privacy options failed", it) }
        }
    }

    SettingsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                SettingsAction.Back -> navigator.back()
                SettingsAction.OpenTerms -> openLink(AppConfig.TERMS_OF_SERVICE_URL) {
                    AnalyticsTracker.trackTerms(AnalyticsTracker.Referrer.ABOUT)
                }
                SettingsAction.OpenPrivacy -> openLink(AppConfig.PRIVACY_POLICY_URL) {
                    AnalyticsTracker.trackPrivacy(AnalyticsTracker.Referrer.ABOUT)
                }
                SettingsAction.OpenAdPrivacy -> openAdPrivacy()
                SettingsAction.RateApp -> openGooglePlayRatePage(context)
                is SettingsAction.TemperatureUnitChanged -> viewModel.onAction(action)
            }
        },
        onNavigate = navigator::open,
        showAdPrivacyOptions = adPrivacyGateway.isPrivacyOptionsRequired(),
    )
}


private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
