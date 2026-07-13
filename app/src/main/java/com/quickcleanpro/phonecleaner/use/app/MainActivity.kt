package com.quickcleanpro.phonecleaner.use.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkAdapter
import com.quickcleanpro.phonecleaner.use.core.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.use.core.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.use.brand.AppConfig
import com.quickcleanpro.phonecleaner.use.app.runtime.notification.ToolNotificationIntentFactory
import com.quickcleanpro.phonecleaner.use.skin.common.theme.QuickCleanProAppTheme
import com.quickcleanpro.phonecleaner.use.app.AppRoot

class MainActivity : AppCompatActivity() {
    private val launchCoordinator =
        AppLaunchCoordinator(
            targetRouteResolver = ToolNotificationIntentFactory::targetRoute,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AnalyticsTracker.handleLaunchIntent(intent)
        launchCoordinator.onCreate(intent)

        setContent {
            QuickCleanProAppTheme {
                AppRoot(
                    launchCoordinator = launchCoordinator,
                    onNotificationPermissionGranted = ::ensureSdkPersistentNotificationRunning,
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AnalyticsTracker.handleLaunchIntent(intent)
        launchCoordinator.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        ensureSdkPersistentNotificationRunning()
    }

    private fun ensureSdkPersistentNotificationRunning() {
        if (!AppConfig.hasPostNotificationsPermission(this)) return
        runCatching { AdvertiseSdkAdapter.ensurePersistentNotificationServiceRunning(this) }
    }
}
