package com.quickcleanpro.phonecleaner.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.quickcleanpro.phonecleaner.common.ads.AdvertiseSdkAdapter
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.app.AppConfig
import com.quickcleanpro.phonecleaner.app.runtime.notification.NotificationIntentRouteResolver
import com.quickcleanpro.phonecleaner.common.ui.theme.QuickCleanProAppTheme
import com.quickcleanpro.phonecleaner.app.AppRoot

class MainActivity : AppCompatActivity() {
    private val launchCoordinator =
        AppLaunchCoordinator(
            targetRouteResolver = NotificationIntentRouteResolver::targetRoute,
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
