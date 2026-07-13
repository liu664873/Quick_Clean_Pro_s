package com.quickcleanpro.phonecleaner.app.navigation.feature

import com.quickcleanpro.phonecleaner.app.navigation.AppDestination

data class FeatureSpec(
    val key: FeatureKey,
    val route: String,
    val group: FeatureGroup,
)

object FeatureCatalog {
    val specs: List<FeatureSpec> =
        AppDestination.featureEntries.map { destination ->
            FeatureSpec(
                key = requireNotNull(destination.featureKey),
                route = destination.route,
                group = requireNotNull(destination.featureGroup),
            )
        }

    val byKey: Map<FeatureKey, FeatureSpec> = specs.associateBy(FeatureSpec::key)

    fun spec(key: FeatureKey): FeatureSpec = byKey.getValue(key)

    fun routeFor(feature: FeatureKey): String? = byKey[feature]?.route

    fun featureForRoute(route: String): FeatureKey? =
        AppDestination.forRoute(route)?.featureKey

    fun groupFeatures(group: FeatureGroup): Set<FeatureKey> =
        specs.filter { it.group == group }.mapTo(linkedSetOf(), FeatureSpec::key)
}

val fileFeatures: Set<FeatureKey> = FeatureCatalog.groupFeatures(FeatureGroup.FILES)

val toolboxFeatures: Set<FeatureKey> = FeatureCatalog.groupFeatures(FeatureGroup.TOOLBOX)

fun featureForRoute(route: String): FeatureKey? = FeatureCatalog.featureForRoute(route)
