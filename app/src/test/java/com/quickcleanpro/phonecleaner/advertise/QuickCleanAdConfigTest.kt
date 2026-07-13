package com.quickcleanpro.phonecleaner.advertise

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.common.ads.AdAreaKeys
import com.quickcleanpro.phonecleaner.common.ads.AdNavigationPolicy
import com.quickcleanpro.phonecleaner.common.ads.AdPlacementRegistry
import com.quickcleanpro.phonecleaner.common.ads.AdScene
import com.quickcleanpro.phonecleaner.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureCatalog
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import java.io.File
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCleanAdConfigTest {
    @Test
    fun rawAdPolicyContainsOnlyDeclaredOpenBannerAndInterstitialKeys() {
        val rawKeys = areaKeysFrom("config/ad_policy.json")
        val expectedKeys =
            constantsIn(AdAreaKeys.Open::class.java) +
                constantsIn(AdAreaKeys.Banner::class.java) +
                constantsIn(AdAreaKeys.Interstitial::class.java)

        assertEquals(expectedKeys, rawKeys)
    }

    @Test
    fun nativePolicyContainsEveryV1NativeKey() {
        val rawKeys = areaKeysFrom("src/main/res/raw/native_ad_policy.json")
        val expectedKeys = constantsIn(AdAreaKeys.Native::class.java)

        assertEquals(28, expectedKeys.size)
        assertEquals(expectedKeys, rawKeys)
    }

    @Test
    fun rawPoliciesDoNotContainRemovedDraftKeys() {
        val rawKeys =
            areaKeysFrom("config/ad_policy.json") +
                areaKeysFrom("src/main/res/raw/native_ad_policy.json")
        val removedKeys =
            rawKeys.filter { key ->
                key == "splashAdv" ||
                    key.startsWith("recall") ||
                    key.startsWith("reload") ||
                    key.startsWith("recom")
            }

        assertTrue("Removed draft keys still present: $removedKeys", removedKeys.isEmpty())
    }

    @Test
    fun declaredAreaKeysCoverAllRawPolicies() {
        val declaredKeys =
            constantsIn(AdAreaKeys.Open::class.java) +
                constantsIn(AdAreaKeys.Banner::class.java) +
                constantsIn(AdAreaKeys.Interstitial::class.java) +
                constantsIn(AdAreaKeys.Native::class.java)
        val rawKeys =
            areaKeysFrom("config/ad_policy.json") +
                areaKeysFrom("src/main/res/raw/native_ad_policy.json")
        val undeclaredKeys = rawKeys - declaredKeys

        assertTrue("Raw policy has undeclared keys: $undeclaredKeys", undeclaredKeys.isEmpty())
    }

    @Test
    fun onboardingScenesResolveToGuideInterstitialAreaKeys() {
        assertEquals(
            AdAreaKeys.Interstitial.NEW_GUIDE_SCAN_FINISH,
            AdPlacementRegistry.interstitialArea(AdScene.OnboardingScanFinished),
        )
        assertEquals(
            AdAreaKeys.Interstitial.NEW_GUIDE_SKIP,
            AdPlacementRegistry.interstitialArea(AdScene.OnboardingSkipped),
        )
    }

    @Test
    fun completionScenesResolveToFinishInterstitialAreaKeys() {
        val expected =
            listOf(
                FeatureKey.JUNK_CLEAN to AdAreaKeys.Interstitial.MAIN_JUNK_CLEAN_FINISH,
                FeatureKey.NETWORK_SPEED to AdAreaKeys.Interstitial.MAIN_NETWORK_SPEED_FINISH,
                FeatureKey.NOTIFICATION_CLEANER to AdAreaKeys.Interstitial.MAIN_NOTIFICATION_CLEAN_FINISH,
                FeatureKey.WHATSAPP_CLEANER to AdAreaKeys.Interstitial.MAIN_WHATSAPP_CLEAN_FINISH,
                FeatureKey.PHOTOS to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.SIMILAR_PHOTOS to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.PHOTO_PRIVACY to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.SCREENSHOTS to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.VIDEOS to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.AUDIOS to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.LARGE_FILES to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.DUPLICATE_FILES to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
                FeatureKey.DOCUMENTS to AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH,
            )

        expected.forEach { (feature, areaKey) ->
            assertEquals(
                areaKey,
                AdPlacementRegistry.interstitialArea(
                    AdScene.OperationFinished(feature, OperationAction.CLEAN, success = true),
                ),
            )
        }
    }

    @Test
    fun notificationCleanerCompletionAndReturnResolveToInterstitialAreaKeys() {
        assertEquals(
            AdAreaKeys.Interstitial.MAIN_NOTIFICATION_CLEAN_FINISH,
            AdPlacementRegistry.interstitialArea(
                AdScene.OperationFinished(
                    FeatureKey.NOTIFICATION_CLEANER,
                    OperationAction.CLEAN,
                    success = true,
                ),
            ),
        )
        assertEquals(
            AdAreaKeys.Interstitial.RETURN_FROM_NOTIFICATION_CLEAN,
            AdPlacementRegistry.interstitialArea(AdScene.ReturnHome(FeatureKey.NOTIFICATION_CLEANER)),
        )
    }

    @Test
    fun recallTargetFeatureRoutesProduceEntryAdDecisions() {
        val policy = AdNavigationPolicy()
        val expected =
            listOf(
                AppDestination.JunkClean.route to FeatureKey.JUNK_CLEAN,
                AppDestination.BatteryInfo.route to FeatureKey.BATTERY_INFO,
                AppDestination.NetworkSpeed.route to FeatureKey.NETWORK_SPEED,
        )

        expected.forEach { (route, feature) ->
            val decision = policy.entryAdDecision(fromRoute = null, targetRoute = route)

            assertEquals(feature, decision?.feature)
            assertEquals(route, decision?.route)
        }
    }

    @Test
    fun fileManagerFinishInterstitialIsEnabledInRawPolicy() {
        assertEquals(1, rawPolicyRateFor(AdAreaKeys.Interstitial.MAIN_FILE_MANAGE_FINISH))
    }

    @Test
    fun returnHomeScenesResolveToReturnInterstitialAreaKeys() {
        val expected =
            listOf(
                FeatureKey.JUNK_CLEAN to AdAreaKeys.Interstitial.RETURN_FROM_JUNK_CLEAN,
                FeatureKey.ANTI_VIRUS to AdAreaKeys.Interstitial.RETURN_FROM_VIRUS_ANTI,
                FeatureKey.APP_LOCK to AdAreaKeys.Interstitial.RETURN_FROM_APP_LOCK,
                FeatureKey.DEVICE_INFO to AdAreaKeys.Interstitial.RETURN_FROM_DEVICE_INFO,
                FeatureKey.BATTERY_INFO to AdAreaKeys.Interstitial.RETURN_FROM_BATTERY_INFO,
                FeatureKey.APP_USAGE to AdAreaKeys.Interstitial.RETURN_FROM_APP_USAGE,
                FeatureKey.NOTIFICATION_CLEANER to AdAreaKeys.Interstitial.RETURN_FROM_NOTIFICATION_CLEAN,
                FeatureKey.WHATSAPP_CLEANER to AdAreaKeys.Interstitial.RETURN_FROM_WHATSAPP_CLEAN,
                FeatureKey.NETWORK_USAGE to AdAreaKeys.Interstitial.RETURN_FROM_NETWORK_USAGE,
                FeatureKey.NETWORK_SCAN to AdAreaKeys.Interstitial.RETURN_FROM_NETWORK_SCAN,
                FeatureKey.NETWORK_SPEED to AdAreaKeys.Interstitial.RETURN_FROM_NETWORK_SPEED,
                FeatureKey.PHOTOS to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.SIMILAR_PHOTOS to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.PHOTO_PRIVACY to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.SCREENSHOTS to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.VIDEOS to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.AUDIOS to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.LARGE_FILES to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.DUPLICATE_FILES to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                FeatureKey.DOCUMENTS to AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
            )

        expected.forEach { (feature, areaKey) ->
            assertEquals(
                areaKey,
                AdPlacementRegistry.interstitialArea(AdScene.ReturnHome(feature)),
            )
        }
    }

    @Test
    fun everyCatalogFeatureHasEntryAndReturnInterstitialAreaKey() {
        FeatureCatalog.specs.forEach { spec ->
            val entry = AdPlacementRegistry.interstitialArea(AdScene.EnterFeature(spec.key, spec.route))
            val returning = AdPlacementRegistry.interstitialArea(AdScene.ReturnHome(spec.key))

            assertTrue("Missing entry interstitial for ${spec.key}", !entry.isNullOrBlank())
            assertTrue("Missing return interstitial for ${spec.key}", !returning.isNullOrBlank())
        }
    }

    @Test
    fun permissionRejectedScenesReuseReturnInterstitialAreaKeys() {
        FeatureCatalog.specs.forEach { spec ->
            assertEquals(
                AdPlacementRegistry.interstitialArea(AdScene.ReturnHome(spec.key)),
                AdPlacementRegistry.interstitialArea(AdScene.PermissionRejected(spec.key)),
            )
        }
    }

    @Test
    fun fileManagerFeaturesShareFileManagerInterstitialAreaKeys() {
        val fileFeatures =
            listOf(
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

        fileFeatures.forEach { feature ->
            assertEquals(
                AdAreaKeys.Interstitial.MAIN_FILE_MANAGE,
                AdPlacementRegistry.interstitialArea(AdScene.EnterFeature(feature, null)),
            )
            assertEquals(
                AdAreaKeys.Interstitial.RETURN_FROM_FILE_MANAGE,
                AdPlacementRegistry.interstitialArea(AdScene.ReturnHome(feature)),
            )
        }
    }

    private fun areaKeysFrom(relativePath: String): Set<String> {
        val content = sourceFile(relativePath).readText()
        return AREA_KEY_REGEX
            .findAll(content)
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun rawPolicyRateFor(areaKey: String): Int {
        val content = sourceFile("config/ad_policy.json").readText()
        val unit =
            AD_UNIT_REGEX
                .findAll(content)
                .firstOrNull { areaKey in it.value }
                ?.value
                ?: error("Missing raw ad unit for $areaKey")
        return requireNotNull(RATE_REGEX.find(unit)) { "Missing rate for $areaKey" }
            .groupValues[1]
            .toInt()
    }

    private fun constantsIn(type: Class<*>): Set<String> =
        type.declaredFields
            .filter { field ->
                field.type == String::class.java &&
                    Modifier.isStatic(field.modifiers)
            }
            .map { field -> field.get(null) as String }
            .toSet()

    private fun sourceFile(relativePath: String): File {
        val startDir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        generateSequence(startDir) { current -> current.parentFile }
            .forEach { dir ->
                val direct = File(dir, relativePath)
                if (direct.isFile) return direct

                val fromRoot = File(dir, "app/$relativePath")
                if (fromRoot.isFile) return fromRoot
            }
        error("Cannot find $relativePath from $startDir")
    }

    private companion object {
        val AREA_KEY_REGEX = Regex("\"areakey\"\\s*:\\s*\"([^\"]+)\"")
        val AD_UNIT_REGEX = Regex("\\{\\s*\"areakey\"\\s*:\\s*\"[^\"]+\".*?\\n\\s*\\}", RegexOption.DOT_MATCHES_ALL)
        val RATE_REGEX = Regex("\"rate\"\\s*:\\s*(\\d+)")
    }
}
