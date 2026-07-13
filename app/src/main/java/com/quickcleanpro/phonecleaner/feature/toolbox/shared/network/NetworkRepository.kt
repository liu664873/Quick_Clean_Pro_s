package com.quickcleanpro.phonecleaner.feature.toolbox.shared.network

import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkScanResult
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkSpeedProgress
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkSpeedResult
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkUsageInfo

interface NetworkRepository {
    fun isNetworkAvailable(): Boolean

    fun isWifiConnected(): Boolean

    fun isMobileConnected(): Boolean

    fun hasNetworkUsageAccess(): Boolean

    suspend fun readNetworkUsage(): NetworkUsageInfo

    suspend fun runSpeedTest(): NetworkSpeedResult

    suspend fun runSpeedTestWithProgress(onProgress: (NetworkSpeedProgress) -> Unit): NetworkSpeedResult

    suspend fun scanWifi(): NetworkScanResult
}
