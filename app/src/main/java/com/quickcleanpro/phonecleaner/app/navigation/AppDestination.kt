package com.quickcleanpro.phonecleaner.app.navigation

import com.quickcleanpro.phonecleaner.use.core.feature.FeatureGroup
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import java.net.URLEncoder

const val DETAIL_INITIAL_INDEX_ARG = "initialIndex"

/**
 * The single source of truth for navigation destinations.
 *
 * Legacy route, screen and notification APIs are kept as adapters around this
 * registry while callers migrate to [AppDestination].
 */
sealed class AppDestination private constructor(
    val route: String,
    val featureKey: FeatureKey? = null,
    val featureGroup: FeatureGroup? = null,
    val featureEntry: Boolean = false,
    val notificationAliases: Set<String> = emptySet(),
    val notificationTarget: Boolean = false,
) {
    fun withArgs(args: Map<String, String> = emptyMap()): String {
        if (args.isEmpty()) return route
        val query = args.entries.joinToString("&") { (key, value) ->
            "${key.encodeQueryPart()}=${value.encodeQueryPart()}"
        }
        return "$route?$query"
    }

    fun withDetailInitialIndex(index: Int): String = "$route/${index.coerceAtLeast(0)}"

    fun detailPattern(): String = "$route/{$DETAIL_INITIAL_INDEX_ARG}"

    data object Splash : AppDestination("splash")

    data object OnboardingScan : AppDestination("onboarding_scan")

    data object Home :
        AppDestination(
            route = "home",
            notificationAliases = setOf("home", "main", "/main"),
            notificationTarget = true,
        )

    data object HomeFileManager :
        AppDestination(
            route = HOME_FILE_MANAGER_ROUTE,
            featureGroup = FeatureGroup.FILES,
            notificationAliases = setOf("file_manager", HOME_FILE_MANAGER_ROUTE),
            notificationTarget = true,
        )

    data object HomeToolbox :
        AppDestination(
            route = HOME_TOOLBOX_ROUTE,
            featureGroup = FeatureGroup.TOOLBOX,
            notificationAliases = setOf("toolbox", HOME_TOOLBOX_ROUTE),
            notificationTarget = true,
        )

    data object Settings : AppDestination("settings")

    data object ManagePermissions : AppDestination("manage_permissions")

    data object JunkClean :
        AppDestination(
            route = "scan",
            featureKey = FeatureKey.JUNK_CLEAN,
            featureGroup = FeatureGroup.HOME,
            featureEntry = true,
            notificationAliases = setOf("junk_files", "clean", "scan", "/scan", "/junkClean"),
            notificationTarget = true,
        )

    data object AntiVirus :
        AppDestination(
            route = "anti_virus",
            featureKey = FeatureKey.ANTI_VIRUS,
            featureGroup = FeatureGroup.HOME,
            featureEntry = true,
            notificationAliases = setOf("virus_anti", "virusAnti", "/virusAnti"),
            notificationTarget = true,
        )

    data object VirusQuickScan :
        AppDestination(
            route = "virus_quick_scan",
            featureKey = FeatureKey.ANTI_VIRUS,
            featureGroup = FeatureGroup.HOME,
        )

    data object VirusDeepScan :
        AppDestination(
            route = "virus_deep_scan",
            featureKey = FeatureKey.ANTI_VIRUS,
            featureGroup = FeatureGroup.HOME,
        )

    data object VirusResult :
        AppDestination(
            route = "virus_result",
            featureKey = FeatureKey.ANTI_VIRUS,
            featureGroup = FeatureGroup.HOME,
        )

    data object NoVirusResult :
        AppDestination(
            route = "no_virus_result",
            featureKey = FeatureKey.ANTI_VIRUS,
            featureGroup = FeatureGroup.HOME,
        )

    data object AppLock :
        AppDestination(
            route = "app_lock",
            featureKey = FeatureKey.APP_LOCK,
            featureGroup = FeatureGroup.HOME,
            featureEntry = true,
            notificationAliases = setOf("app_lock", "appLock", "/appLock"),
            notificationTarget = true,
        )

    data object DeviceInfo :
        AppDestination(
            route = "device_info",
            featureKey = FeatureKey.DEVICE_INFO,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf("device_info", "deviceInfo", "/deviceInfo"),
            notificationTarget = true,
        )

    data object BatteryInfo :
        AppDestination(
            route = "battery_info",
            featureKey = FeatureKey.BATTERY_INFO,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf(
                "battery_info",
                "batteryInfo",
                "/batteryInfo",
                "checkBatteryInfo",
                "/checkBatteryInfo",
            ),
            notificationTarget = true,
        )

    data object AppUsage :
        AppDestination(
            route = "app_usage",
            featureKey = FeatureKey.APP_USAGE,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf("app_usage", "appUsage", "/appUsage"),
            notificationTarget = true,
        )

    data object NetworkUsage :
        AppDestination(
            route = "network_usage",
            featureKey = FeatureKey.NETWORK_USAGE,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf("network_usage", "networkUsage", "/networkUsage"),
            notificationTarget = true,
        )

    data object NetworkScan :
        AppDestination(
            route = "network_scan",
            featureKey = FeatureKey.NETWORK_SCAN,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf("network_scan", "networkScan", "/networkScan"),
            notificationTarget = true,
        )

    data object NetworkScanDevices :
        AppDestination(
            route = "network_scan_devices",
            featureKey = FeatureKey.NETWORK_SCAN,
            featureGroup = FeatureGroup.TOOLBOX,
        )

    data object NetworkSpeed :
        AppDestination(
            route = "network_speed",
            featureKey = FeatureKey.NETWORK_SPEED,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf("network_speed", "networkSpeed", "/networkSpeed"),
            notificationTarget = true,
        )

    data object WhatsAppCleaner :
        AppDestination(
            route = "whatsapp_cleaner",
            featureKey = FeatureKey.WHATSAPP_CLEANER,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf("whatsapp", "/whatsapp", "whatsapp_cleaner"),
            notificationTarget = true,
        )

    data object NotificationCleaner :
        AppDestination(
            route = "notification_cleaner",
            featureKey = FeatureKey.NOTIFICATION_CLEANER,
            featureGroup = FeatureGroup.TOOLBOX,
            featureEntry = true,
            notificationAliases = setOf(
                "notification_bar",
                "notification_clean",
                "notificationClean",
                "/notificationClean",
            ),
            notificationTarget = true,
        )

    data object PhotosManager :
        AppDestination(
            route = "photos_manager",
            featureKey = FeatureKey.PHOTOS,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
            notificationAliases = setOf("photos", "managePhotos", "/managePhotos"),
            notificationTarget = true,
        )

    data object SimilarPhotosManager :
        AppDestination(
            route = "similar_photos_manager",
            featureKey = FeatureKey.SIMILAR_PHOTOS,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
            notificationAliases = setOf("similar_photos", "manageSimilarPhotos", "/manageSimilarPhotos"),
            notificationTarget = true,
        )

    data object PhotoPrivacyManager :
        AppDestination(
            route = "photo_privacy_manager",
            featureKey = FeatureKey.PHOTO_PRIVACY,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
        )

    data object ScreenshotsManager :
        AppDestination(
            route = "screenshots_manager",
            featureKey = FeatureKey.SCREENSHOTS,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
            notificationAliases = setOf("screenshots", "screenshot_manager", "/screenshotManager"),
            notificationTarget = true,
        )

    data object VideosManager :
        AppDestination(
            route = "videos_manager",
            featureKey = FeatureKey.VIDEOS,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
            notificationAliases = setOf("videos", "manageVideos", "/manageVideos"),
            notificationTarget = true,
        )

    data object AudiosManager :
        AppDestination(
            route = "audios_manager",
            featureKey = FeatureKey.AUDIOS,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
        )

    data object LargeFilesManager :
        AppDestination(
            route = "large_files_manager",
            featureKey = FeatureKey.LARGE_FILES,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
            notificationAliases = setOf("large_files", "largeFileManager", "/largeFileManager"),
            notificationTarget = true,
        )

    data object DuplicateFilesManager :
        AppDestination(
            route = "duplicate_files_manager",
            featureKey = FeatureKey.DUPLICATE_FILES,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
        )

    data object DocumentsManager :
        AppDestination(
            route = "documents_manager",
            featureKey = FeatureKey.DOCUMENTS,
            featureGroup = FeatureGroup.FILES,
            featureEntry = true,
        )

    data object PhotosDetail :
        AppDestination(
            route = "photos_detail",
            featureKey = FeatureKey.PHOTOS,
            featureGroup = FeatureGroup.FILES,
        )

    data object SimilarPhotosDetail :
        AppDestination(
            route = "similar_photos_detail",
            featureKey = FeatureKey.SIMILAR_PHOTOS,
            featureGroup = FeatureGroup.FILES,
        )

    data object ScreenshotsDetail :
        AppDestination(
            route = "screenshots_detail",
            featureKey = FeatureKey.SCREENSHOTS,
            featureGroup = FeatureGroup.FILES,
        )

    data object VideosDetail :
        AppDestination(
            route = "videos_detail",
            featureKey = FeatureKey.VIDEOS,
            featureGroup = FeatureGroup.FILES,
        )

    data object AudiosDetail :
        AppDestination(
            route = "audios_detail",
            featureKey = FeatureKey.AUDIOS,
            featureGroup = FeatureGroup.FILES,
        )

    data object LargeFilesDetail :
        AppDestination(
            route = "large_files_detail",
            featureKey = FeatureKey.LARGE_FILES,
            featureGroup = FeatureGroup.FILES,
        )

    data object DocumentsDetail :
        AppDestination(
            route = "documents_detail",
            featureKey = FeatureKey.DOCUMENTS,
            featureGroup = FeatureGroup.FILES,
        )

    companion object {
        const val HOME_FILE_MANAGER_ROUTE = "home_file_manager"
        const val HOME_TOOLBOX_ROUTE = "home_toolbox"

        /** All registered destinations, in stable navigation/catalog order. */
        val all: List<AppDestination> by lazy {
            listOf(
                Splash,
                OnboardingScan,
                Home,
                HomeFileManager,
                HomeToolbox,
                Settings,
                ManagePermissions,
                JunkClean,
                AntiVirus,
                VirusQuickScan,
                VirusDeepScan,
                VirusResult,
                NoVirusResult,
                AppLock,
                DeviceInfo,
                BatteryInfo,
                AppUsage,
                NetworkUsage,
                NetworkScan,
                NetworkScanDevices,
                NetworkSpeed,
                WhatsAppCleaner,
                NotificationCleaner,
                PhotosManager,
                SimilarPhotosManager,
                PhotoPrivacyManager,
                ScreenshotsManager,
                VideosManager,
                AudiosManager,
                LargeFilesManager,
                DuplicateFilesManager,
                DocumentsManager,
                PhotosDetail,
                SimilarPhotosDetail,
                ScreenshotsDetail,
                VideosDetail,
                AudiosDetail,
                LargeFilesDetail,
                DocumentsDetail,
            )
        }

        val featureEntries: List<AppDestination> by lazy { all.filter(AppDestination::featureEntry) }

        private val byRoute: Map<String, AppDestination> by lazy { all.associateBy(AppDestination::route) }
        private val byFeature: Map<FeatureKey, AppDestination> by lazy {
            featureEntries.associateBy { requireNotNull(it.featureKey) }
        }

        val startupRoutes: Set<String> by lazy { setOf(Splash.route, OnboardingScan.route) }
        val homeRoutes: Set<String> by lazy { setOf(Home.route, HomeFileManager.route, HomeToolbox.route) }
        val rootRoutes: Set<String> by lazy { startupRoutes + homeRoutes }

        val notificationTargets: List<AppDestination> by lazy { all.filter(AppDestination::notificationTarget) }
        val notificationTargetRoutes: Set<String> by lazy {
            notificationTargets.mapTo(linkedSetOf(), AppDestination::route)
        }

        private val notificationAliasToRoute: Map<String, String> by lazy {
            notificationTargets
                .flatMap { destination ->
                    destination.notificationAliases.map { alias ->
                        alias.normalizedKey() to destination.route
                    }
                }.toMap()
        }

        private val notificationRouteByKey: Map<String, String> by lazy {
            notificationTargetRoutes.associateBy { route -> route.normalizedKey() }
        }

        fun forRoute(route: String?): AppDestination? {
            val key = route?.routeKey() ?: return null
            return byRoute[key] ?: detailDestinations.firstOrNull { destination -> key.startsWith("${destination.route}/") }
        }

        fun forFeature(featureKey: FeatureKey): AppDestination? = byFeature[featureKey]

        fun normalizeNotificationRoute(rawRoute: String?): String? {
            val key = rawRoute?.normalizedKey()?.takeIf(String::isNotEmpty) ?: return null
            return notificationAliasToRoute[key] ?: notificationRouteByKey[key]
        }

        fun notificationAliases(): List<NotificationAlias> =
            notificationTargets.flatMap { destination ->
                destination.notificationAliases.map { rawRoute ->
                    NotificationAlias(rawRoute = rawRoute, route = destination.route)
                }
            }

        private fun String.routeKey(): String =
            trim()
                .substringBefore("#")
                .substringBefore("?")
                .trim()

        private fun String.normalizedKey(): String =
            routeKey()
                .trim('/')
                .lowercase()

        private val detailDestinations: Set<AppDestination> by lazy {
            setOf(
                PhotosDetail,
                SimilarPhotosDetail,
                ScreenshotsDetail,
                VideosDetail,
                AudiosDetail,
                LargeFilesDetail,
                DocumentsDetail,
            )
        }
    }
}

data class NotificationAlias(
    val rawRoute: String,
    val route: String,
)

private fun String.encodeQueryPart(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name())
