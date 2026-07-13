package com.quickcleanpro.phonecleaner.notification

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.notification.ToolNotificationIntentFactory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRouteMappingTest {
    @Test
    fun localNotificationContentRoutesResolveToSupportedTargets() {
        val routes = notificationRoutesFrom("app/src/main/res/raw/notification_content.json")

        assertTrue(routes.isNotEmpty())
        assertAllRoutesResolve(routes)
    }

    @Test
    fun remoteNotificationContentRoutesResolveToSupportedTargets() {
        val routes = notificationRoutesFrom("docs/remote_config_quick_clean_pro.json")

        assertTrue(routes.isNotEmpty())
        assertAllRoutesResolve(routes)
    }

    @Test
    fun invalidLegacyRouteDoesNotBlockValidSdkRoute() {
        val route =
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("missing_route", "/junkClean"),
            )

        assertEquals(AppDestination.JunkClean.route, route)
    }

    @Test
    fun firstValidNotificationRouteCandidateWins() {
        val route =
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("missing_route", "/networkScan", "photos"),
            )

        assertEquals(AppDestination.NetworkScan.route, route)
    }

    @Test
    fun routeVariantsNormalizeBeforeResolving() {
        assertEquals(
            AppDestination.JunkClean.route,
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("/junkClean?from=push"),
            ),
        )
        assertEquals(
            AppDestination.NetworkScan.route,
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("/networkScan/"),
            ),
        )
        assertEquals(
            AppDestination.NotificationCleaner.route,
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("notification_bar"),
            ),
        )
        assertEquals(
            AppDestination.NotificationCleaner.route,
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("/notificationClean"),
            ),
        )
    }

    @Test
    fun routeNormalizationTrimsWhitespaceAndFragments() {
        assertEquals(
            AppDestination.JunkClean.route,
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("  /junkClean?from=push#notification  "),
            ),
        )
    }

    @Test
    fun canonicalRouteValuesResolveWithoutLegacyAliases() {
        assertEquals(
            AppDestination.BatteryInfo.route,
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf(" battery_info "),
            ),
        )
    }

    @Test
    fun unknownNotificationRouteFallsBackToHome() {
        assertEquals(
            AppDestination.Home.route,
            ToolNotificationIntentFactory.resolveTargetRouteCandidates(
                listOf("/unknownRoute"),
            ),
        )
    }

    @Test
    fun emptyNotificationRouteCandidatesReturnNull() {
        assertNull(ToolNotificationIntentFactory.resolveTargetRouteCandidates(emptyList()))
    }

    private fun assertAllRoutesResolve(routes: Set<String>) {
        val unresolved =
            routes.filter { route ->
                AppDestination.normalizeNotificationRoute(route) == null ||
                    !ToolNotificationIntentFactory.isValidRoute(route)
            }

        assertTrue("Unresolved notification routes: $unresolved", unresolved.isEmpty())
    }

    private fun notificationRoutesFrom(relativePath: String): Set<String> =
        ROUTE_REGEX
            .findAll(sourceFile(relativePath).readText())
            .map { it.groupValues[1] }
            .toSet()

    private fun sourceFile(relativePath: String): File {
        val startDir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        generateSequence(startDir) { current -> current.parentFile }
            .forEach { dir ->
                val direct = File(dir, relativePath)
                if (direct.isFile) return direct
            }
        error("Cannot find $relativePath from $startDir")
    }

    private companion object {
        val ROUTE_REGEX = Regex("\"Route\"\\s*:\\s*\"([^\"]+)\"")
    }
}
