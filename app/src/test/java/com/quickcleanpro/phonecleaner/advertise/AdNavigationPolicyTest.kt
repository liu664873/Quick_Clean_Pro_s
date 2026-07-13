package com.quickcleanpro.phonecleaner.advertise

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

import com.quickcleanpro.phonecleaner.common.ads.AdNavigationPolicy
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdNavigationPolicyTest {
    @Test
    fun blacklistedTargetWinsOverWhitelist() {
        val policy =
            AdNavigationPolicy(
                routeBlacklist = setOf(AppDestination.Home.route),
                sourceBlacklist = emptySet(),
                entryWhitelist = setOf(AppDestination.Home.route),
            )

        assertNull(policy.entryAdDecision(AppDestination.Settings.route, AppDestination.Home.route))
    }

    @Test
    fun nonWhitelistedTargetDoesNotShowEntryAd() {
        val decision = AdNavigationPolicy().entryAdDecision(AppDestination.Home.route, AppDestination.Settings.route)

        assertNull(decision)
    }

    @Test
    fun sameFeatureInternalRouteDoesNotShowEntryAd() {
        val policy =
            AdNavigationPolicy(
                routeBlacklist = emptySet(),
                sourceBlacklist = emptySet(),
                entryWhitelist = setOf(AppDestination.VirusQuickScan.route),
            )

        assertNull(policy.entryAdDecision(AppDestination.AntiVirus.route, AppDestination.VirusQuickScan.route))
    }

    @Test
    fun legacyNotificationRouteIsNotWhitelistedByDefault() {
        val decision = AdNavigationPolicy().entryAdDecision(AppDestination.Home.route, "notification_bar")

        assertNull(decision)
    }

    @Test
    fun whitelistedFeatureRouteReturnsDecision() {
        val decision = AdNavigationPolicy().entryAdDecision(AppDestination.Home.route, AppDestination.JunkClean.route)

        assertEquals(FeatureKey.JUNK_CLEAN, decision?.feature)
        assertEquals(AppDestination.JunkClean.route, decision?.route)
    }
}
