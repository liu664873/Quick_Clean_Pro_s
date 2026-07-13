package com.quickcleanpro.phonecleaner.use.feature.toolbox.domain

import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkScanResult
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkSpeedProgress
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkSpeedResult
import com.quickcleanpro.phonecleaner.use.core.model.toolbox.NetworkUsageInfo

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
