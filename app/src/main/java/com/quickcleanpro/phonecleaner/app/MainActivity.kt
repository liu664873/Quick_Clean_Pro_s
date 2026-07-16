package com.quickcleanpro.phonecleaner.app

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.quickcleanpro.phonecleaner.common.ads.AdvertiseSdkAdapter
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.app.runtime.startup.AppLaunchCoordinator
import com.quickcleanpro.phonecleaner.app.AppConfig
import com.quickcleanpro.phonecleaner.app.runtime.notification.NotificationIntentRouteResolver
import com.quickcleanpro.phonecleaner.common.ui.theme.QuickCleanProAppTheme
import com.quickcleanpro.phonecleaner.app.AppRoot
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var persistentNotificationStartJob: Job? = null

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
        if (persistentNotificationStartJob?.isActive == true) return
        persistentNotificationStartJob =
            lifecycleScope.launch {
                val app = application as? MyApp
                if (app == null || !app.sdkInitialization.awaitNotificationDefaultsReady()) {
                    Log.w(TAG, "SDK persistent notification start skipped: notification defaults not ready")
                    return@launch
                }
                if (!AppConfig.hasPostNotificationsPermission(this@MainActivity)) {
                    Log.d(TAG, "SDK persistent notification start skipped: permission unavailable")
                    return@launch
                }
                runCatching {
                    AdvertiseSdkAdapter.ensurePersistentNotificationServiceRunning(this@MainActivity)
                }.onSuccess {
                    Log.d(TAG, "SDK persistent notification service start requested")
                }.onFailure { error ->
                    Log.e(TAG, "SDK persistent notification service start failed", error)
                }
            }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
