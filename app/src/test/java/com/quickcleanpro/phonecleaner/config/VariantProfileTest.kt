package com.quickcleanpro.phonecleaner.config

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.BuildConfig
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureCatalog
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VariantProfileTest {
    @Test
    fun featureCatalogMapsNestedRoutesToOwningFeature() {
        assertEquals(FeatureKey.ANTI_VIRUS, FeatureCatalog.featureForRoute(AppDestination.VirusQuickScan.route))
        assertEquals(FeatureKey.NETWORK_SCAN, FeatureCatalog.featureForRoute(AppDestination.NetworkScanDevices.route))
    }

    @Test
    fun featureCatalogDoesNotCarryAdPlacementState() {
        FeatureCatalog.specs.forEach { spec ->
            assertFalse(spec.toString().contains("Ad", ignoreCase = true))
        }
    }

    @Test
    fun appDestinationEncodesQueryArgs() {
        val route = AppDestination.Settings.withArgs(mapOf("name" to "a b&c", "path" to "/a/b"))

        assertEquals("settings?name=a+b%26c&path=%2Fa%2Fb", route)
    }

    @Test
    fun productIdentityIsGeneratedIntoBuildConfig() {
        assertEquals("quickcleanpro", BuildConfig.PRODUCT_PROFILE_KEY)
        assertEquals("quickclean_pro", BuildConfig.PRODUCT_THEME_KEY)
        assertEquals("https://sites.google.com/view/quickcleanpro-termsconditions/home", BuildConfig.ADV_TERMS_URL)
        assertEquals("https://sites.google.com/view/quick-clean-pro-privacy-policy/home", BuildConfig.ADV_PRIVACY_URL)
    }
}
