package com.quickcleanpro.phonecleaner.feature.toolbox.appusage.data

import android.content.Context
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.data.AppUsageDataSource
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.AppUsageInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.data.AppUsageRepository

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
