package com.quickcleanpro.phonecleaner.use.skin.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.quickcleanpro.phonecleaner.app.monetization.AdPrivacyGateway
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.brand.AppConfig
import com.quickcleanpro.phonecleaner.use.core.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.use.core.common.platform.ExternalActivityLaunchHandler
import com.quickcleanpro.phonecleaner.use.core.common.platform.openUrl
import com.quickcleanpro.phonecleaner.use.core.repository.SettingsRepository
import org.koin.compose.koinInject

@Composable
fun SettingsRoute(
    navigator: AppNavigator,
    externalActivityLaunchHandler: ExternalActivityLaunchHandler,
    settingsRepository: SettingsRepository = koinInject(),
    adPrivacyGateway: AdPrivacyGateway = koinInject(),
) {
    val context = LocalContext.current
    var temperatureUnit by remember {
        mutableStateOf(settingsRepository.readTemperatureUnit().normalizeTemperatureUnit())
    }

    fun openLink(url: String, track: () -> Unit) {
        externalActivityLaunchHandler.markLaunch()
        track()
        if (!context.openUrl(url)) externalActivityLaunchHandler.cancelLaunch()
    }

    SettingsScreen(
        temperatureUnit = temperatureUnit,
        showAdPrivacyOptions = adPrivacyGateway.isPrivacyOptionsRequired(),
        onBack = { navigator.back() },
        onOpenManagePermissions = { navigator.open(AppDestination.ManagePermissions) },
        onOpenTerms = {
            openLink(AppConfig.TERMS_OF_SERVICE_URL) {
                AnalyticsTracker.trackTerms(AnalyticsTracker.Referrer.ABOUT)
            }
        },
        onOpenPrivacy = {
            openLink(AppConfig.PRIVACY_POLICY_URL) {
                AnalyticsTracker.trackPrivacy(AnalyticsTracker.Referrer.ABOUT)
            }
        },
        onOpenAdPrivacy = {
            val activity = context.findActivity()
            if (activity == null) {
                Log.w("SettingsRoute", "skip ad privacy options: activity unavailable")
            } else {
                runCatching { adPrivacyGateway.showPrivacyOptions(activity) }
                    .onFailure { Log.w("SettingsRoute", "show ad privacy options failed", it) }
            }
        },
        onTemperatureUnitChanged = { unit ->
            val normalized = unit.normalizeTemperatureUnit()
            settingsRepository.saveTemperatureUnit(normalized)
            temperatureUnit = normalized
        },
    )
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
