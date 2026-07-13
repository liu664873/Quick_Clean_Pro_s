package com.quickcleanpro.phonecleaner.feature.toolbox.shared.network

const val UNKNOWN_NETWORK_TRAFFIC_PACKAGE = "com.quickcleanpro.phonecleaner.unknown_network_traffic"

data class NetworkUsageApp(
    val appName: String,
    val packageName: String,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

data class NetworkUsageInfo(
    val wifiRxBytes: Long,
    val wifiTxBytes: Long,
    val cellularRxBytes: Long,
    val cellularTxBytes: Long,
    val apps: List<NetworkUsageApp> = emptyList(),
    val wifiApps: List<NetworkUsageApp> = apps,
    val cellularApps: List<NetworkUsageApp> = emptyList(),
    val fallbackApps: List<NetworkUsageApp> = emptyList(),
    val isToday: Boolean,
    val needsUsageAccess: Boolean,
) {
    val wifiTotalBytes: Long get() = wifiRxBytes + wifiTxBytes
    val cellularTotalBytes: Long get() = cellularRxBytes + cellularTxBytes
}

data class NetworkSpeedResult(
    val downloadMbps: String,
    val uploadMbps: String,
    val latencyMs: Long?,
    val measured: Boolean,
)

data class NetworkSpeedProgress(
    val downloadMbps: String? = null,
    val uploadMbps: String? = null,
    val latencyMs: Long? = null,
    val phase: String = "idle",
)

data class NetworkDeviceInfo(
    val ip: String,
    val hostName: String,
    val macAddress: String,
)

data class NetworkScanResult(
    val ssid: String,
    val gatewayIp: String,
    val dnsIp: String,
    val deviceIp: String,
    val devices: List<NetworkDeviceInfo>,
    val hasWifi: Boolean,
)
