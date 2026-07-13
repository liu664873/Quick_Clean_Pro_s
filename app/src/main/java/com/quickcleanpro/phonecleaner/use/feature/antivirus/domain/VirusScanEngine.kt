package com.quickcleanpro.phonecleaner.use.feature.antivirus.domain

interface VirusScanEngine {
    fun startScan(
        mode: VirusScanMode,
        listener: VirusScanEngineListener,
    )

    fun cancelScan()
}

interface VirusScanEngineListener {
    fun onScanStarted()

    fun onScanProgress(item: VirusScanItem?)

    fun onScanError(
        code: Int,
        message: String?,
        kind: VirusScanErrorKind,
    )

    fun onScanCanceled()

    fun onScanInterrupt()

    fun onScanFinished()
}

data class VirusScanItem(
    val packageName: String?,
    val apkPath: String?,
    val appName: String?,
    val virusName: String?,
    val summary: List<String>?,
    val score: Int,
)

enum class VirusScanErrorKind {
    Network,
    Unknown,
}
