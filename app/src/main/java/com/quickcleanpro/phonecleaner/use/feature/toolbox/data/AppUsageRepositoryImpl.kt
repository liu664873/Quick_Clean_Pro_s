package com.quickcleanpro.phonecleaner.use.feature.toolbox.data

import android.content.Context
import com.quickcleanpro.phonecleaner.use.feature.toolbox.data.source.toolbox.AppUsageDataSource
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.AppUsageInfo
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.AppUsageRepository

class AppUsageRepositoryImpl(
    context: Context,
) : AppUsageRepository {
    private val appContext = context.applicationContext

    override fun hasAppUsageAccess(): Boolean = AppUsageDataSource.hasUsageAccess(appContext)

    override fun resetAppUsagePermissionCache() {
        AppUsageDataSource.resetPermissionCache()
    }

    override suspend fun appUsageBetween(
        startMillis: Long,
        endMillis: Long,
    ): List<AppUsageInfo> = AppUsageDataSource.usageBetween(appContext, startMillis, endMillis)

    override suspend fun runningPackages(packageNames: Set<String>): Set<String> =
        AppUsageDataSource.runningPackages(appContext, packageNames)
}
