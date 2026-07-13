package com.quickcleanpro.phonecleaner.use.core.ads

import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey

object AdPlacementRegistry {
    fun interstitialArea(scene: AdScene): String? =
        when (scene) {
            AdScene.OnboardingScanFinished -> AdAreaKeys.Interstitial.NEW_GUIDE_SCAN_FINISH
            AdScene.OnboardingSkipped -> AdAreaKeys.Interstitial.NEW_GUIDE_SKIP
            is AdScene.EnterFeature -> enterArea(scene.feature)
            is AdScene.OperationFinished -> if (scene.success) finishArea(scene.feature) else null
            is AdScene.ReturnHome -> returnArea(scene.feature)
            is AdScene.PermissionRejected -> returnArea(scene.feature)
            is AdScene.ScanFinished,
            is AdScene.Reload,
            is AdScene.RecommendClick,
            -> null
        }

    private fun enterArea(feature: FeatureKey): String? =
        when (feature) {
            FeatureKey.JUNK_CLEAN -> AdAreaKeys.Interstitial.MAIN_JUNK_CLEAN
            FeatureKey.ANTI_VIRUS -> AdAreaKeys.Interstitial.MAIN_VIRUS_ANTI
            FeatureKey.APP_LOCK -> AdAreaKeys.Interstitial.MAIN_APP_LOCK
            FeatureKey.DEVICE_INFO -> AdAreaKeys.Interstitial.MAIN_DEVICE_INFO
            FeatureKey.BATTERY_INFO -> AdAreaKeys.Interstitial.MAIN_BATTERY_INFO
            FeatureKey.APP_USAGE -> AdAreaKeys.Interstitial.MAIN_APP_USAGE
            FeatureKey.NETWORK_USAGE -> AdAreaKeys.Interstitial.MAIN_NETWORK_USAGE
            FeatureKey.NETWORK_SCAN -> AdAreaKeys.Interstitial.MAIN_NETWORK_SCAN
            FeatureKey.NETWORK_SPEED -> AdAreaKeys.Interstitial.MAIN_NETWORK_SPEED
            FeatureKey.NOTIFICATION_CLEANER -> AdAreaKeys.Interstitial.MAIN_NOTIFICATION_CLEAN
            FeatureKey.WHATSAPP_CLEANER -> AdAreaKeys.Interstitial.MAIN_WHATSAPP_CLEAN
            FeatureKey.PHOTOS,
            FeatureKey.SIMILAR_PHOTOS,
            FeatureKey.PHOTO_PRIVACY,
            FeatureKey.SCREENSHOTS,
            FeatureKey.VIDEOS,
            FeatureKey.AUDIOS,
            FeatureKey.LARGE_FILES,
            FeatureKey.DUPLICATE_FILES,
            FeatureKey.DOCUMENTS,
            -> AdAreaKeys.Interstitial.MAIN_FILE_MANAGE
        }

    private fun finishArea(feature: FeatureKey): String? =
        when (feature) {
            FeatureKey.JUNK_CLEAN -> AdAreaKeys.Interstitial.MAIN_JUNK_CLEAN_FINISH
            FeatureKey.NETWORK_SPEED -> AdAreaKeys.Interstitial.MAIN_NETWORK_SPEED_FINISH
            FeatureKey.NOTIFICATION_CLEANER -> AdAreaKeys.Interstitial.MAIN_NOTIFICATION_CLEAN_FINISH
            FeatureKey.WHATSAPP_CLEANER -> AdAreaKeys.Interstitial.MAIN_WHATSAPP_CLEAN_FINISH
            FeatureKey.PHOTOS,
            FeatureKey.SIMILAR_PHOTOS,
            FeatureKey.PHOTO_PRIVACY,
            FeatureKey.SCREENSHOTS,
            FeatureKey.VIDEOS,
            FeatureKey.AUDIOS,
            FeatureKey.LARGE_FILES,
            FeatureKey.DUPLICATE_FILES,
            FeatureKey.DOCUMENTS,
            -> AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH
            else -> null
        }

    private fun returnArea(feature: FeatureKey): String? =
        when (feature) {
            FeatureKey.JUNK_CLEAN -> AdAreaKeys.Interstitial.RETURN_FROM_JUNK_CLEAN
            FeatureKey.ANTI_VIRUS -> AdAreaKeys.Interstitial.RETURN_FROM_VIRUS_ANTI
            FeatureKey.APP_LOCK -> AdAreaKeys.Interstitial.RETURN_FROM_APP_LOCK
            FeatureKey.DEVICE_INFO -> AdAreaKeys.Interstitial.RETURN_FROM_DEVICE_INFO
            FeatureKey.BATTERY_INFO -> AdAreaKeys.Interstitial.RETURN_FROM_BATTERY_INFO
            FeatureKey.APP_USAGE -> AdAreaKeys.Interstitial.RETURN_FROM_APP_USAGE
            FeatureKey.NOTIFICATION_CLEANER -> AdAreaKeys.Interstitial.RETURN_FROM_NOTIFICATION_CLEAN
            FeatureKey.WHATSAPP_CLEANER -> AdAreaKeys.Interstitial.RETURN_FROM_WHATSAPP_CLEAN
            FeatureKey.NETWORK_USAGE -> AdAreaKeys.Interstitial.RETURN_FROM_NETWORK_USAGE
            FeatureKey.NETWORK_SCAN -> AdAreaKeys.Interstitial.RETURN_FROM_NETWORK_SCAN
            FeatureKey.NETWORK_SPEED -> AdAreaKeys.Interstitial.RETURN_FROM_NETWORK_SPEED
            FeatureKey.PHOTOS,
            FeatureKey.SIMILAR_PHOTOS,
            FeatureKey.PHOTO_PRIVACY,
            FeatureKey.SCREENSHOTS,
            FeatureKey.VIDEOS,
            FeatureKey.AUDIOS,
            FeatureKey.LARGE_FILES,
            FeatureKey.DUPLICATE_FILES,
            FeatureKey.DOCUMENTS,
            -> AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE
        }
}
