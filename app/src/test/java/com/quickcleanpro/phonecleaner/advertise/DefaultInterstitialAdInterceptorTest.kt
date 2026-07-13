package com.quickcleanpro.phonecleaner.advertise

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.common.ads.AdAreaKeys
import com.quickcleanpro.phonecleaner.common.ads.AdScene
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.OperationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRuntimeNavigationTest {
    @Test
    fun routeEntryShowsMappedInterstitialBeforeContinue() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)
        var continued = false

        runtime.runRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.JunkClean.route,
        ) {
            continued = true
        }

        assertEquals(listOf(AdAreaKeys.Interstitial.MAIN_JUNK_CLEAN), driver.interstitialAreaKeys)
        assertTrue(!continued)

        driver.completeInterstitial()

        assertTrue(continued)
    }

    @Test
    fun nonEntryRouteContinuesWithoutAd() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)
        var continued = false

        runtime.runRouteEntry(
            fromRoute = AppDestination.Home.route,
            targetRoute = AppDestination.Settings.route,
        ) {
            continued = true
        }

        assertTrue(driver.interstitialAreaKeys.isEmpty())
        assertTrue(continued)
    }

    @Test
    fun featureOperationUsesMappedInterstitial() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)
        var continued = false

        runtime.run(
            scene = AdScene.OperationFinished(
                feature = FeatureKey.JUNK_CLEAN,
                action = OperationAction.CLEAN,
                success = true,
            ),
            requestId = "operation_finished_JUNK_CLEAN_CLEAN_true",
        ) {
            continued = true
        }

        assertEquals(listOf(AdAreaKeys.Interstitial.MAIN_JUNK_CLEAN_FINISH), driver.interstitialAreaKeys)
        assertTrue(!continued)

        driver.completeInterstitial()

        assertTrue(continued)
    }

    @Test
    fun returnHomeUsesMappedInterstitial() {
        val driver = FakeAdRuntimeDriver()
        val runtime = fakeAdRuntime(driver)

        runtime.run(
            scene = AdScene.ReturnHome(FeatureKey.NETWORK_SPEED),
            requestId = "return_home_NETWORK_SPEED",
        ) {}

        assertEquals(listOf(AdAreaKeys.Interstitial.RETURN_FROM_NETWORK_SPEED), driver.interstitialAreaKeys)
    }
}
