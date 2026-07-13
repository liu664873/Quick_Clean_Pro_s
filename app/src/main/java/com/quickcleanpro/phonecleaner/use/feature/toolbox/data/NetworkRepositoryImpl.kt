package com.quickcleanpro.phonecleaner.use.feature.toolbox.data

import android.content.Context
import com.quickcleanpro.phonecleaner.use.feature.toolbox.data.source.toolbox.NetworkToolboxDataSource
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkScanResult
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkSpeedProgress
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkSpeedResult
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkUsageInfo
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.NetworkRepository

class NetworkRepositoryImpl(
    context: Context,
) : NetworkRepository {
    private val appContext = context.applicationContext

    override fun isNetworkAvailable(): Boolean = NetworkToolboxDataSource.isNetworkAvailable(appContext)

    override fun isWifiConnected(): Boolean = NetworkToolboxDataSource.isWifiConnected(appContext)

    override fun isMobileConnected(): Boolean = NetworkToolboxDataSource.isMobileConnected(appContext)

    override fun hasNetworkUsageAccess(): Boolean = NetworkToolboxDataSource.hasUsageAccess(appContext)

    override suspend fun readNetworkUsage(): NetworkUsageInfo = NetworkToolboxDataSource.readUsage(appContext)

    override suspend fun runSpeedTest(): NetworkSpeedResult = NetworkToolboxDataSource.runSpeedTest(appContext)

    override suspend fun runSpeedTestWithProgress(onProgress: (NetworkSpeedProgress) -> Unit): NetworkSpeedResult =
        NetworkToolboxDataSource.runSpeedTestWithProgress(appContext, onProgress)

    override suspend fun scanWifi(): NetworkScanResult = NetworkToolboxDataSource.scanWifi(appContext)
}
