package com.quickcleanpro.phonecleaner.app.monetization

import android.content.Context
import android.util.Log
import com.pdffox.adv.AdvertiseSdkConfig
import com.pdffox.adv.AdvertiseSdkConfigs
import com.pdffox.adv.DEFAULT_DEBUG_NATIVE_IDS_JSON
import com.pdffox.adv.NotificationActionConfig
import com.pdffox.adv.NotificationFeatureConfig
import com.pdffox.adv.NotificationRouteMapping
import com.pdffox.adv.PushSceneKeyConfig
import com.quickcleanpro.phonecleaner.BuildConfig
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

object AdvertiseConfigFactory {
    private const val TAG = "AdvertiseConfigFactory"

    fun create(context: Context): AdvertiseSdkConfig {
        logMissingRequiredConfig()
        return AdvertiseSdkConfigs.create(context, BuildConfig.DEBUG) {
            legal(
                privacyUrl = BuildConfig.ADV_PRIVACY_URL,
                termsUrl = BuildConfig.ADV_TERMS_URL,
            )
            defaultTopic(BuildConfig.ADV_DEFAULT_TOPIC)
            adDebugOverride(BuildConfig.ADV_AD_DEBUG_OVERRIDE_MODE)

            resources(
                adPolicyRawResId = R.raw.ad_policy,
                adLoadConfigRawResId = R.raw.adload_config,
                nativeAdPolicyRawResId = R.raw.native_ad_policy,
                nativeAdIdsRawResId = R.raw.native_ad_ids,
                cloudCidrsRawResId = com.pdffox.adv.R.raw.cloud,
                googleCidrsRawResId = com.pdffox.adv.R.raw.google,
                pushConfigRawResId = R.raw.push,
            )

            server(
                enabled = true,
                releaseHost = BuildConfig.ADV_SERVER_RELEASE_HOST,
                testHost = BuildConfig.ADV_SERVER_TEST_HOST,
                parseTokenKey = BuildConfig.ADV_PLAY_INTEGRITY_PARSE_TOKEN_KEY,
            )
            firebase(
                analyticsEnabled = true,
                messagingEnabled = true,
                subscribeDefaultTopic = true,
            )
            remoteConfig(
                enabled = true,
                encryptionKeyBase64 = BuildConfig.ADV_REMOTE_CONFIG_ENCRYPTION_KEY,
                encryptionKeyId = BuildConfig.ADV_REMOTE_CONFIG_ENCRYPTION_KEY_ID,
            )
            thinking(
                enabled = true,
                appKey = BuildConfig.ADV_THINKING_APP_KEY,
                serverUrl = BuildConfig.ADV_THINKING_SERVER_URL,
            )
            singular(
                enabled = true,
                apiKey = BuildConfig.ADV_SINGULAR_API_KEY,
                secret = BuildConfig.ADV_SINGULAR_SECRET,
            )

            adMob(
                enabled = true,
                appId = BuildConfig.ADV_ADMOB_APP_ID,
                bannerId = BuildConfig.ADV_ADMOB_BANNER_ID,
                interstitialId = BuildConfig.ADV_ADMOB_INTERSTITIAL_ID,
                nativeId = BuildConfig.ADV_ADMOB_NATIVE_ID,
                openId = BuildConfig.ADV_ADMOB_OPEN_ID,
                rewardedId = BuildConfig.ADV_ADMOB_REWARDED_ID,
                nativeIdsJson = BuildConfig.ADV_ADMOB_NATIVE_IDS_JSON,
                debugNativeIdsJson = DEFAULT_DEBUG_NATIVE_IDS_JSON,
            )

            facebook(
                enabled = true,
                appId = BuildConfig.ADV_FACEBOOK_APP_ID,
                clientToken = BuildConfig.ADV_FACEBOOK_CLIENT_TOKEN,
            )
            tiktok(
                enabled = true,
                accessToken = BuildConfig.ADV_TIKTOK_ACCESS_TOKEN,
                ttAppId = BuildConfig.ADV_TIKTOK_TT_APP_ID,
                appId = BuildConfig.ADV_TIKTOK_APP_ID,
            )
            safe(
                enabled = true,
                expectedSignatures = BuildConfig.ADV_SAFE_EXPECTED_SIGNATURES,
                expectedPackageName = BuildConfig.APPLICATION_ID,
                rejectDebuggableBuilds = true,
                rejectDebuggerAttached = true,
                killProcessOnFailure = true,
                enforceInDebugBuilds = false,
            )
            push(
                enabled = true,
                persistentServiceEnabled = true,
                firebaseMessagingServiceEnabled = true,
                serviceStarterJobEnabled = true,
                bootReceiverEnabled = true,
                notificationDeletedReceiverEnabled = true,
                fileProviderEnabled = true,
                deletionObserverEnabled = true,
                sceneKeys =
                    PushSceneKeyConfig(
                        imageDeleted = "/managePhotos",
                        videoDeleted = "/manageVideos",
                        fileDeleted = "/largeFileManager",
                    ),
            )
            notifications(notificationConfig(context))
            playIntegrity(
                enabled = true,
                cloudProjectNumber = BuildConfig.ADV_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER,
                runInDebugBuilds = false,
            )
        }
    }

    private fun notificationConfig(context: Context): NotificationFeatureConfig =
        NotificationFeatureConfig(
            enabled = true,
            smallIconResId = R.drawable.ic_notification_cleaner,
            persistentContentText = context.getString(R.string.app_name),
            persistentActions =
                listOf(
                    NotificationActionConfig(
                        "junk_files",
                        context.getString(R.string.junk_removal),
                        R.drawable.ic_persistent_clean,
                    ),
                    NotificationActionConfig(
                        "file_manager",
                        context.getString(R.string.home_tab_file_manager),
                        R.drawable.ic_persistent_file,
                    ),
                    NotificationActionConfig(
                        "toolbox",
                        context.getString(R.string.home_tab_toolbox),
                        R.drawable.ic_persistent_tools,
                    ),
                    NotificationActionConfig(
                        "battery_info",
                        context.getString(R.string.nav_battery_info),
                        R.drawable.ic_persistent_battery,
                    ),
                ),
            routeMappings = notificationRouteMappings(),
        )

    private fun notificationRouteMappings(): List<NotificationRouteMapping> =
        AppDestination.notificationAliases().map { alias ->
            route(
                rawRoute = alias.rawRoute,
                route = alias.route,
                iconResId = iconForRoute(alias.route),
            )
        }

    private fun route(
        rawRoute: String,
        route: String,
        iconResId: Int,
    ): NotificationRouteMapping =
        NotificationRouteMapping(
            rawRoute = rawRoute,
            route = route,
            temporaryIconResId = iconResId,
            persistentIconResId = iconResId,
        )

    private fun iconForRoute(route: String): Int =
        when (route) {
            AppDestination.JunkClean.route -> R.drawable.ic_n_junk_removal
            AppDestination.AntiVirus.route -> R.drawable.home_virus_shield
            AppDestination.AppLock.route -> R.drawable.home_app_lock
            AppDestination.DeviceInfo.route -> R.drawable.ic_n_device_info
            AppDestination.BatteryInfo.route -> R.drawable.ic_n_battery_info
            AppDestination.AppUsage.route -> R.drawable.ic_app_usage
            AppDestination.NetworkUsage.route -> R.drawable.ic_n_network_usage
            AppDestination.NetworkScan.route -> R.drawable.ic_n_network_scan
            AppDestination.NetworkSpeed.route -> R.drawable.ic_network_speed
            AppDestination.WhatsAppCleaner.route -> R.drawable.ic_n_whatsapp
            AppDestination.NotificationCleaner.route -> R.drawable.ic_n_notification_cleaner
            AppDestination.PhotosManager.route -> R.drawable.ic_photos
            AppDestination.SimilarPhotosManager.route -> R.drawable.ic_similar_photos
            AppDestination.VideosManager.route -> R.drawable.ic_videos
            AppDestination.LargeFilesManager.route -> R.drawable.ic_large_files
            AppDestination.ScreenshotsManager.route -> R.drawable.ic_screenshots
            AppDestination.HOME_FILE_MANAGER_ROUTE -> R.drawable.ic_file_manager
            AppDestination.HOME_TOOLBOX_ROUTE -> R.drawable.ic_toolbox
            else -> R.mipmap.ic_launcher_round
        }

    private fun logMissingRequiredConfig() {
        warnIfBlank("ADV_SERVER_RELEASE_HOST", BuildConfig.ADV_SERVER_RELEASE_HOST)
        warnIfBlank("ADV_SERVER_TEST_HOST", BuildConfig.ADV_SERVER_TEST_HOST)
        warnIfBlank("ADV_PLAY_INTEGRITY_PARSE_TOKEN_KEY", BuildConfig.ADV_PLAY_INTEGRITY_PARSE_TOKEN_KEY)
        warnIfBlank("ADV_FACEBOOK_APP_ID", BuildConfig.ADV_FACEBOOK_APP_ID)
        warnIfBlank("ADV_FACEBOOK_CLIENT_TOKEN", BuildConfig.ADV_FACEBOOK_CLIENT_TOKEN)
        warnIfBlank("ADV_TIKTOK_ACCESS_TOKEN", BuildConfig.ADV_TIKTOK_ACCESS_TOKEN)
        warnIfBlank("ADV_TIKTOK_TT_APP_ID", BuildConfig.ADV_TIKTOK_TT_APP_ID)
        warnIfBlank("ADV_TIKTOK_APP_ID", BuildConfig.ADV_TIKTOK_APP_ID)
        warnIfBlank("ADV_THINKING_APP_KEY", BuildConfig.ADV_THINKING_APP_KEY)
        warnIfBlank("ADV_THINKING_SERVER_URL", BuildConfig.ADV_THINKING_SERVER_URL)
        logMaskedIfPresent("ADV_THINKING_APP_KEY", BuildConfig.ADV_THINKING_APP_KEY)
        warnIfBlank("ADV_SAFE_EXPECTED_SIGNATURES", BuildConfig.ADV_SAFE_EXPECTED_SIGNATURES)
    }

    private fun logMaskedIfPresent(
        name: String,
        value: String,
    ) {
        if (value.isNotBlank()) {
            Log.i(TAG, "$name=${value.toMaskedConfigValue()}")
        }
    }

    private fun String.toMaskedConfigValue(): String =
        when {
            length <= 8 -> "*".repeat(length)
            else -> "${take(6)}...${takeLast(6)}"
        }

    private fun warnIfBlank(
        name: String,
        value: String,
    ) {
        if (value.isBlank()) {
            Log.w(TAG, "$name is blank; related advertise capability is enabled but will wait for release configuration.")
        }
    }
}
