package com.quickcleanpro.phonecleaner.use.feature.toolbox.domain

import com.quickcleanpro.phonecleaner.use.core.model.toolbox.AppUsageInfo

interface AppUsageRepository {
    fun hasAppUsageAccess(): Boolean

    fun resetAppUsagePermissionCache()

    suspend fun appUsageBetween(
        startMillis: Long,
        endMillis: Long,
    ): List<AppUsageInfo>

    suspend fun runningPackages(packageNames: Set<String>): Set<String>
}
