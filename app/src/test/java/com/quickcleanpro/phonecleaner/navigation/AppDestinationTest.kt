package com.quickcleanpro.phonecleaner.navigation

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureCatalog
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test
    fun routesAndFeatureEntriesAreUnique() {
        assertEquals(AppDestination.all.size, AppDestination.all.map { it.route }.distinct().size)
        assertEquals(FeatureKey.entries.size, AppDestination.featureEntries.size)
        assertEquals(
            FeatureKey.entries.toSet(),
            AppDestination.featureEntries.mapNotNull { it.featureKey }.toSet(),
        )
    }

    @Test
    fun routeLookupDelegatesToDestinations() {
        AppDestination.all.forEach { destination ->
            assertEquals(destination, AppDestination.forRoute(destination.route))
        }
    }

    @Test
    fun featureCatalogUsesDestinationOwnershipForEntryAndNestedRoutes() {
        AppDestination.featureEntries.forEach { destination ->
            val feature = requireNotNull(destination.featureKey)
            assertEquals(destination.route, FeatureCatalog.routeFor(feature))
            assertEquals(feature, FeatureCatalog.featureForRoute(destination.route))
        }

        assertEquals(
            FeatureKey.ANTI_VIRUS,
            FeatureCatalog.featureForRoute(AppDestination.VirusQuickScan.route),
        )
        assertEquals(
            FeatureKey.PHOTOS,
            FeatureCatalog.featureForRoute("${AppDestination.PhotosDetail.route}/3"),
        )
    }

    @Test
    fun everyNotificationAliasResolvesToItsRegisteredDestination() {
        val mappings = AppDestination.notificationAliases()

        assertTrue(mappings.isNotEmpty())
        mappings.forEach { mapping ->
            assertEquals(mapping.route, AppDestination.normalizeNotificationRoute(mapping.rawRoute))
            assertNotNull(AppDestination.forRoute(mapping.route))
        }
        AppDestination.notificationTargetRoutes.forEach { route ->
            assertEquals(route, AppDestination.normalizeNotificationRoute(route))
        }
    }
}
