package com.quickcleanpro.phonecleaner.use.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.quickcleanpro.phonecleaner.BuildConfig
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkAdapter
import com.quickcleanpro.phonecleaner.use.app.di.dataModule
import com.quickcleanpro.phonecleaner.use.app.di.presentationModule
import com.quickcleanpro.phonecleaner.use.app.runtime.SdkInitializationCoordinator
import com.quickcleanpro.phonecleaner.use.core.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.use.core.source.local.SharedPreferencesUtils
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
        SharedPreferencesUtils.init(this)
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(dataModule, presentationModule)
        }

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
                        AnalyticsTracker.initialize()
                        ProcessLifecycleOwner.get().lifecycle.addObserver(AppAnalyticsLifecycleObserver)
                    }
                },
                notificationDefaultsInitializer = {
                    withContext(Dispatchers.Main.immediate) {
                        loadSdkNotificationDefaultsIfNeeded()
                    }
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

    private fun loadSdkNotificationDefaultsIfNeeded() {
        resources.openRawResource(R.raw.notification_content)
            .bufferedReader()
            .use { it.readText() }
            .takeIf { it.isNotBlank() }
            ?.let(AdvertiseSdkAdapter::updateNotificationContent)
    }

    private object AppAnalyticsLifecycleObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            AnalyticsTracker.onAppForeground()
        }

        override fun onStop(owner: LifecycleOwner) {
            AnalyticsTracker.onAppBackground()
        }
    }
}
