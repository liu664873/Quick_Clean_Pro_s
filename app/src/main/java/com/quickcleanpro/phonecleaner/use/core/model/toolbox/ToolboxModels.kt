package com.quickcleanpro.phonecleaner.use.core.model.toolbox

/**
 * UI 用于标识无法归属到具体应用的系统或未知流量 */
const val UNKNOWN_NETWORK_TRAFFIC_PACKAGE = "com.quickcleanpro.phonecleaner.unknown_network_traffic"

/**
 * 单个应用的使用情况 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalForegroundMs: Long,
    val launchCount: Int,
) {
    /** 已格式化的前台使用时长*/
    val formattedTime: String get() = formatDuration(totalForegroundMs)
}

/**
 * 单个应用的网络用量 */
data class NetworkUsageApp(
    val appName: String,
    val packageName: String,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
) {
    /** 下载与上传合计字节数*/
    val totalBytes: Long get() = rxBytes + txBytes
}

/**
 * 网络用量页面展示所需的汇总数据和应用明细 */
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
    /** Wi-Fi 下载和上传的总字节数*/
    val wifiTotalBytes: Long get() = wifiRxBytes + wifiTxBytes

    /** 蜂窝网络下载和上传的总字节数*/
    val cellularTotalBytes: Long get() = cellularRxBytes + cellularTxBytes
}

/**
 * 网络测速结果 */
data class NetworkSpeedResult(
    val downloadMbps: String,
    val uploadMbps: String,
    val latencyMs: Long?,
    val measured: Boolean,
)

/**
 * 网络测速过程中的阶段性进度 */
data class NetworkSpeedProgress(
    val downloadMbps: String? = null,
    val uploadMbps: String? = null,
    val latencyMs: Long? = null,
    val phase: String = "idle",
)

/**
 * 局域网扫描得到的设备信息 */
data class NetworkDeviceInfo(
    val ip: String,
    val hostName: String,
    val macAddress: String,
)

/**
 * Wi-Fi 扫描结果 */
data class NetworkScanResult(
    val ssid: String,
    val gatewayIp: String,
    val dnsIp: String,
    val deviceIp: String,
    val devices: List<NetworkDeviceInfo>,
    val hasWifi: Boolean,
)

/**
 * 将毫秒时长格式化App Usage 展示文案 */
fun formatDuration(totalMs: Long): String {
    val totalMinutes = (totalMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
        hours > 0L -> "${hours}h"
        else -> "${minutes}m"
    }
}
