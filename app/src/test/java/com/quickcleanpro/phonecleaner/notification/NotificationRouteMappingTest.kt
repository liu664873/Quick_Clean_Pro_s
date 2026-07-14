package com.quickcleanpro.phonecleaner.notification

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.runtime.notification.NotificationIntentRouteResolver
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRouteMappingTest {
    @Test
    fun sdkRouteExtraWinsOverLegacyExtras() {
        val route =
            NotificationIntentRouteResolver.targetRouteFromExtras(
                mapOf(
                    "Route" to "/networkScan",
                    "quickclean_target_route" to "photos",
                ),
            )

        assertEquals(AppDestination.NetworkScan.route, route)
    }

    @Test
    fun legacyRouteExtrasRemainSupported() {
        listOf(
            "quickclean_target_route",
            "route",
            "target_route",
            "targetRoute",
        ).forEach { key ->
            assertEquals(
                AppDestination.JunkClean.route,
                NotificationIntentRouteResolver.targetRouteFromExtras(mapOf(key to "/junkClean")),
            )
        }
    }

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
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("missing_route", "/junkClean"),
            )

        assertEquals(AppDestination.JunkClean.route, route)
    }

    @Test
    fun firstValidNotificationRouteCandidateWins() {
        val route =
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("missing_route", "/networkScan", "photos"),
            )

        assertEquals(AppDestination.NetworkScan.route, route)
    }

    @Test
    fun routeVariantsNormalizeBeforeResolving() {
        assertEquals(
            AppDestination.JunkClean.route,
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("/junkClean?from=push"),
            ),
        )
        assertEquals(
            AppDestination.NetworkScan.route,
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("/networkScan/"),
            ),
        )
        assertEquals(
            AppDestination.NotificationCleaner.route,
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("notification_bar"),
            ),
        )
        assertEquals(
            AppDestination.NotificationCleaner.route,
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("/notificationClean"),
            ),
        )
    }

    @Test
    fun routeNormalizationTrimsWhitespaceAndFragments() {
        assertEquals(
            AppDestination.JunkClean.route,
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("  /junkClean?from=push#notification  "),
            ),
        )
    }

    @Test
    fun canonicalRouteValuesResolveWithoutLegacyAliases() {
        assertEquals(
            AppDestination.BatteryInfo.route,
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf(" battery_info "),
            ),
        )
    }

    @Test
    fun unknownNotificationRouteFallsBackToHome() {
        assertEquals(
            AppDestination.Home.route,
            NotificationIntentRouteResolver.resolveTargetRouteCandidates(
                listOf("/unknownRoute"),
            ),
        )
    }

    @Test
    fun emptyNotificationRouteCandidatesReturnNull() {
        assertNull(NotificationIntentRouteResolver.targetRoute(null))
        assertNull(NotificationIntentRouteResolver.targetRouteFromExtras(emptyMap()))
        assertNull(NotificationIntentRouteResolver.resolveTargetRouteCandidates(emptyList()))
    }

    private fun assertAllRoutesResolve(routes: Set<String>) {
        val unresolved =
            routes.filter { route ->
                AppDestination.normalizeNotificationRoute(route) == null ||
                    !NotificationIntentRouteResolver.isValidRoute(route)
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
