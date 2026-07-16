package com.quickcleanpro.phonecleaner.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.quickcleanpro.phonecleaner.BuildConfig
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.common.ads.AdvertiseSdkAdapter
import com.quickcleanpro.phonecleaner.app.di.dataModule
import com.quickcleanpro.phonecleaner.app.di.presentationModule
import com.quickcleanpro.phonecleaner.app.runtime.SdkInitializationCoordinator
import com.quickcleanpro.phonecleaner.app.runtime.notification.NotificationDefaultsInitializer
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {
    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        lateinit var instance: MyApp
            private set

        private const val TAG = "MyApplication"
    }

    lateinit var sdkInitialization: SdkInitializationCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val koinApplication = startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(dataModule, presentationModule)
        }
        AnalyticsTracker.initialize(koinApplication.koin.get<AnalyticsPreferences>())

        sdkInitialization =
            SdkInitializationCoordinator(
                scope = initializationScope,
                advertiseInitializer = {
                    withContext(Dispatchers.Main.immediate) {
                        AdvertiseSdkAdapter.initialize(this@MyApp, BuildConfig.DEBUG)
                    }
                },
                analyticsInitializer = {
                    withContext(Dispatchers.Main.immediate) {
                        ProcessLifecycleOwner.get().lifecycle.addObserver(AppAnalyticsLifecycleObserver)
                    }
                },
                notificationDefaultsInitializer = {
                    val result =
                        NotificationDefaultsInitializer(
                            awaitRemoteConfig = { AdvertiseSdkAdapter.awaitRemoteConfigInitialization() },
                            hasConfig = AdvertiseSdkAdapter::hasNotificationConfig,
                            hasContent = AdvertiseSdkAdapter::hasNotificationContent,
                            loadLocalConfig = { readRawResource(R.raw.notification_config) },
                            loadLocalContent = { readRawResource(R.raw.notification_content) },
                            updateConfig = AdvertiseSdkAdapter::updateNotificationConfig,
                            updateContent = AdvertiseSdkAdapter::updateNotificationContent,
                        ).initialize()
                    Log.d(
                        TAG,
                        "Advertise notification defaults ready: " +
                            "config=${result.configSource}, content=${result.contentSource}, " +
                            "remoteCompleted=${result.remoteConfigCompleted}",
                    )
                },
                logger = { message, throwable ->
                    if (throwable == null) {
                        Log.d(TAG, message)
                    } else {
                        Log.e(TAG, message, throwable)
                    }
                },
            ).also { it.start() }
    }

    private fun readRawResource(resourceId: Int): String =
        resources.openRawResource(resourceId)
            .bufferedReader()
            .use { it.readText() }

    private object AppAnalyticsLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            AnalyticsTracker.onAppForeground()
        }

        override fun onStop(owner: LifecycleOwner) {
            AnalyticsTracker.onAppBackground()
        }
    }
}
