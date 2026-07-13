package com.quickcleanpro.phonecleaner.use.core.analytics

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.quickcleanpro.phonecleaner.core.monetization.analytics.AnalyticsDispatcher
import com.quickcleanpro.phonecleaner.core.monetization.analytics.SdkAnalyticsSink
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureCatalog
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.feature.fileFeatures
import com.quickcleanpro.phonecleaner.use.core.source.local.SharedPreferencesUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AnalyticsTracker {
    private const val TAG = "AnalyticsTracker"
    private const val PREF_TOTAL_CLEANCPL_NUM = "analytics_total_cleancpl_num"
    private const val PREF_ONCE_USER_PROPERTIES_REPORTED = "analytics_once_user_properties_reported"
    private const val ANALYTICS_TIME_ZONE_ID = "America/Los_Angeles"
    private const val ANALYTICS_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"

    private val analytics = AnalyticsDispatcher(SdkAnalyticsSink) { operation, throwable ->
        Log.w(TAG, "analytics operation failed: $operation", throwable)
    }

    private var foregroundStartedAt: Long = 0L

    fun initialize() = Unit

    fun onAppForeground() {
        if (foregroundStartedAt != 0L) return
        foregroundStartedAt = SystemClock.elapsedRealtime()
        val currentOpenTime = analyticsTimeNow()
        writeOnceUserProperties(currentOpenTime)
        setUserAttr(UserProperty.LATEST_OPEN_TIME, currentOpenTime)
        addUserAttr(UserProperty.TOTAL_OPEN_NUM, 1)
    }

    fun onAppBackground() {
        val now = SystemClock.elapsedRealtime()
        val startedAt = foregroundStartedAt
        if (startedAt == 0L) return
        foregroundStartedAt = 0L
        if (now <= startedAt) return
        addUserAttr(UserProperty.TOTAL_USE_TIME, now - startedAt)
    }

    fun handleLaunchIntent(intent: Intent?) {
        val appOpenFrom = intent?.getStringExtra("AppOpenFrom").orEmpty()
        setTrafficSource(trafficSource(appOpenFrom))
    }

    fun track(eventName: String, params: Map<String, Any?> = emptyMap()) {
        analytics.track(eventName, params.clean())
    }

    fun setSuperProperties(properties: Map<String, Any?>) {
        analytics.setSuperProperties(properties)
    }

    fun trackSplashDisplay(durationMillis: Long) {
        track(Event.SPLASH_DISPLAY, mapOf(Param.DURATION_TIME to durationMillis.coerceAtLeast(0L)))
    }

    fun trackGuideScanningEntered() {
        track(Event.ENTER_GUIDE_PAGE_SCANNING)
    }

    fun trackGuideScanResultEntered() {
        track(Event.ENTER_GUIDE_PAGE_SCAN_RESULT)
    }

    fun trackGuideContinueClicked() {
        track(Event.CLICK_CONTINUE_GUIDEPAGE)
    }

    fun trackGuideSkipClicked() {
        track(Event.CLICK_SKIP_GUIDEPAGE)
    }

    fun trackHomeEntered(referrerName: String?) {
        track(Event.ENTER_HOMEPAGE, mapOf(Param.REFERRER_NAME to referrerName))
    }

    fun trackCoreFeatureClicked(feature: FeatureKey) {
        coreFeatureDetails(feature)?.let { details ->
            track(Event.CLICK_CORE_FEATURES_BTN, mapOf(Param.DETAILS to details))
        }
    }

    fun trackCoreFeatureEntered(feature: FeatureKey) {
        coreFeatureDetails(feature)?.let { details ->
            track(Event.ENTER_CORE_FEATURES_PAGE, mapOf(Param.DETAILS to details))
        }
    }

    fun trackRatePopup(ifOk: Boolean) {
        track(Event.RATE_POPUP, mapOf(Param.IF_OK to ifOk))
    }

    fun trackFileManagerPopup(ifOk: Boolean) {
        track(Event.FILEMANAGER_POPUP, mapOf(Param.IF_OK to ifOk))
    }

    fun trackNotificationPopup(noticeFlag: Int, ifOk: Boolean) {
        track(
            Event.NOTIFICATION_POPUP,
            mapOf(
                Param.NOTICE_FLAG to noticeFlag,
                Param.IF_OK to ifOk,
            ),
        )
    }

    fun trackPrivacy(referrerName: String) {
        track(Event.CHECK_PRIVACY, mapOf(Param.REFERRER_NAME to referrerName))
    }

    fun trackTerms(referrerName: String) {
        track(Event.CHECK_TERMS, mapOf(Param.REFERRER_NAME to referrerName))
    }

    fun trackFilePermissionResult(accepted: Boolean) {
        track(if (accepted) Event.AGREE_FILEMANAGE_PERMISSION else Event.DISAGREE_FILEMANAGE_PERMISSION)
    }

    fun trackNotificationPermissionResult(accepted: Boolean) {
        track(if (accepted) Event.AGREE_PUSH_PERMISSION else Event.DISAGREE_PUSH_PERMISSION)
    }

    fun trackFeatureOperation(event: FeatureOperationEvent) {
        when (event) {
            is FeatureOperationEvent.ScanFinished -> {
                if (!event.hasResult && event.feature in cleanCompletionFeatures) {
                    incrementCleanCompleted()
                }
            }
            is FeatureOperationEvent.OperationFinished -> {
                if (event.success && event.feature in cleanCompletionFeatures && event.action in cleanCompletionActions) {
                    incrementCleanCompleted()
                }
            }
            else -> Unit
        }
    }

    fun hasCompletedCleanup(): Boolean = SharedPreferencesUtils.getLong(PREF_TOTAL_CLEANCPL_NUM, 0L) > 0L

    fun featureForPrimaryRoute(route: String?): FeatureKey? {
        if (route.isNullOrBlank()) return null
        return FeatureCatalog.byKey.values.firstOrNull { spec -> spec.route == route }?.key
    }

    fun homepageReferrer(previousRoute: String?): String? =
        when {
            previousRoute == AppDestination.OnboardingScan.route -> Referrer.GUIDEPAGE
            featureForPrimaryRoute(previousRoute) != null -> Referrer.CORE_FEATURES
            else -> null
        }

    private fun setTrafficSource(source: String?) {
        setSuperProperties(mapOf(Param.TRAFFIC_SOURCE to source))
    }

    private fun trafficSource(appOpenFrom: String): String? =
        when (appOpenFrom) {
            "Push" -> TrafficSource.FCM_PUSH
            TrafficSource.APP_PUSH -> TrafficSource.APP_PUSH
            TrafficSource.PERSISTENT -> TrafficSource.PERSISTENT
            TrafficSource.APP_SHORTCUTS -> TrafficSource.APP_SHORTCUTS
            else -> null
        }

    private fun writeOnceUserProperties(firstOpenTime: String) {
        if (SharedPreferencesUtils.getBoolean(PREF_ONCE_USER_PROPERTIES_REPORTED, false)) return
        val deviceWritten = setUserOnceAttr(UserProperty.DEVICE_ID, analytics.deviceId().orEmpty())
        val firstOpenWritten = setUserOnceAttr(UserProperty.FIRST_OPEN_TIME, firstOpenTime)
        if (deviceWritten && firstOpenWritten) {
            SharedPreferencesUtils.putBoolean(PREF_ONCE_USER_PROPERTIES_REPORTED, true, commit = true)
        }
    }

    private fun setUserOnceAttr(key: String, value: String): Boolean {
        if (value.isBlank()) return false
        return analytics.setUserOnceProperty(key, value)
    }

    private fun setUserAttr(key: String, value: Any) {
        analytics.setUserProperty(key, value)
    }

    private fun addUserAttr(key: String, value: Number) {
        analytics.addUserProperty(key, value)
    }

    private fun analyticsTimeNow(): String =
        SimpleDateFormat(ANALYTICS_TIME_PATTERN, Locale.US)
            .apply { timeZone = TimeZone.getTimeZone(ANALYTICS_TIME_ZONE_ID) }
            .format(Date(System.currentTimeMillis()))

    private fun incrementCleanCompleted() {
        addLong(PREF_TOTAL_CLEANCPL_NUM, 1L)
    }

    private fun addLong(key: String, delta: Long): Long {
        val next = SharedPreferencesUtils.getLong(key, 0L) + delta
        SharedPreferencesUtils.putLong(key, next)
        return next
    }

    private fun coreFeatureDetails(feature: FeatureKey): String? =
        when (feature) {
            FeatureKey.JUNK_CLEAN -> Details.JUNK_CLEAN
            FeatureKey.ANTI_VIRUS -> Details.VIRUS_ANTI
            FeatureKey.APP_LOCK -> Details.APP_LOCK
            FeatureKey.DEVICE_INFO -> Details.DEVICE_INFO
            FeatureKey.BATTERY_INFO -> Details.BATTERY_INFO
            FeatureKey.APP_USAGE -> Details.APP_USAGE
            FeatureKey.NOTIFICATION_CLEANER -> Details.NOTIFICATION_CLEAN
            FeatureKey.WHATSAPP_CLEANER -> Details.WHATSAPP_CLEAN
            FeatureKey.NETWORK_USAGE -> Details.NETWORK_USAGE
            FeatureKey.NETWORK_SCAN -> Details.NETWORK_SCAN
            FeatureKey.NETWORK_SPEED -> Details.NETWORK_SPEED
            in fileFeatures -> Details.FILE_MANAGE
            else -> null
        }

    private fun Map<String, Any?>.clean(): Map<String, Any> =
        mapNotNull { (key, value) ->
            if (key.isBlank() || value == null) return@mapNotNull null
            val cleanedValue: Any =
                when (value) {
                    is String -> value.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    is Number, is Boolean -> value
                    else -> value.toString().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                }
            key to cleanedValue
        }.toMap()

    object Event {
        const val SPLASH_DISPLAY = "splash_display"
        const val ENTER_GUIDE_PAGE_SCANNING = "enter_guide_page_scanning"
        const val ENTER_GUIDE_PAGE_SCAN_RESULT = "enter_guide_page_scan_result"
        const val CLICK_CONTINUE_GUIDEPAGE = "click_continue_guidepage"
        const val CLICK_SKIP_GUIDEPAGE = "click_skip_guidepage"
        const val AGREE_FILEMANAGE_PERMISSION = "agree_filemanage_permission"
        const val AGREE_PUSH_PERMISSION = "agree_push_permission"
        const val DISAGREE_FILEMANAGE_PERMISSION = "disagree_filemanage_permission"
        const val DISAGREE_PUSH_PERMISSION = "disagree_push_permission"
        const val ENTER_HOMEPAGE = "enter_homepage"
        const val CLICK_CORE_FEATURES_BTN = "click_corefeatures_btn"
        const val ENTER_CORE_FEATURES_PAGE = "enter_corefeatures_page"
        const val RATE_POPUP = "rate_popup"
        const val FILEMANAGER_POPUP = "filemanager_popup"
        const val NOTIFICATION_POPUP = "notification_popup"
        const val CHECK_PRIVACY = "check_privacy"
        const val CHECK_TERMS = "check_terms"
    }

    object Param {
        const val DURATION_TIME = "duration_time"
        const val REFERRER_NAME = "referrer_name"
        const val DETAILS = "details"
        const val IF_OK = "if_ok"
        const val NOTICE_FLAG = "noticeflag"
        const val TRAFFIC_SOURCE = "traffic_source"
    }

    object Referrer {
        const val GUIDEPAGE = "guidepage"
        const val CORE_FEATURES = "corefeatures"
        const val LAUNCHPAGE = "launchpage"
        const val ABOUT = "about"
    }

    private object Details {
        const val JUNK_CLEAN = "junkClean"
        const val VIRUS_ANTI = "virusAnti"
        const val APP_LOCK = "appLock"
        const val FILE_MANAGE = "fileManage"
        const val DEVICE_INFO = "deviceInfo"
        const val BATTERY_INFO = "batteryInfo"
        const val APP_USAGE = "appUsage"
        const val NOTIFICATION_CLEAN = "notificationClean"
        const val WHATSAPP_CLEAN = "whatsappClean"
        const val NETWORK_USAGE = "networkUsage"
        const val NETWORK_SCAN = "networkScan"
        const val NETWORK_SPEED = "networkSpeed"
    }

    private object TrafficSource {
        const val FCM_PUSH = "fcm_push"
        const val APP_PUSH = "app_push"
        const val PERSISTENT = "persistent"
        const val APP_SHORTCUTS = "app_shortcuts"
    }

    private object UserProperty {
        const val DEVICE_ID = "device_id"
        const val FIRST_OPEN_TIME = "first_open_time"
        const val LATEST_OPEN_TIME = "latest_open_time"
        const val TOTAL_OPEN_NUM = "total_open_num"
        const val TOTAL_USE_TIME = "total_use_time"
    }

    private val cleanCompletionActions = setOf(
        OperationAction.CLEAN,
        OperationAction.DELETE,
        OperationAction.REMOVE_LOCATION,
    )

    private val cleanCompletionFeatures = setOf(
        FeatureKey.JUNK_CLEAN,
        FeatureKey.ANTI_VIRUS,
        FeatureKey.NOTIFICATION_CLEANER,
        FeatureKey.WHATSAPP_CLEANER,
        FeatureKey.PHOTOS,
        FeatureKey.SIMILAR_PHOTOS,
        FeatureKey.PHOTO_PRIVACY,
        FeatureKey.SCREENSHOTS,
        FeatureKey.VIDEOS,
        FeatureKey.AUDIOS,
        FeatureKey.LARGE_FILES,
        FeatureKey.DUPLICATE_FILES,
        FeatureKey.DOCUMENTS,
    )
}
