package com.quickcleanpro.phonecleaner.use.feature.antivirus.data

import android.content.Context
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanEngine
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanEngineListener
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanErrorKind
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanItem
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanMode
import com.trustlook.sdk.cloudscan.CloudScanClient
import com.trustlook.sdk.cloudscan.CloudScanListener
import com.trustlook.sdk.data.AppInfo
import com.trustlook.sdk.data.Error as TrustlookError
import com.trustlook.sdk.data.Region

internal class TrustlookVirusScanEngine(
    context: Context,
) : VirusScanEngine {
    private val appContext = context.applicationContext
    private var cloudScanClient: CloudScanClient? = null

    override fun startScan(
        mode: VirusScanMode,
        listener: VirusScanEngineListener,
    ) {
        cloudScanClient = CloudScanClient.Builder(appContext)
            .setRegion(Region.INTL)
            .setConnectionTimeout(30_000)
            .setSocketTimeout(30_000)
            .build()

        val cloudListener = object : CloudScanListener() {
            override fun onScanStarted() {
                listener.onScanStarted()
            }

            override fun onScanProgress(progress: Int, total: Int, appInfo: AppInfo?) {
                listener.onScanProgress(appInfo?.toVirusScanItem())
            }

            override fun onScanError(code: Int, message: String?) {
                listener.onScanError(code, message, code.toErrorKind())
            }

            override fun onScanCanceled() {
                listener.onScanCanceled()
            }

            override fun onScanInterrupt() {
                listener.onScanInterrupt()
            }

            override fun onScanFinished(appList: List<AppInfo?>?) {
                listener.onScanFinished()
            }
        }

        when (mode) {
            VirusScanMode.Quick -> cloudScanClient?.startQuickScan(cloudListener)
            VirusScanMode.Deep -> cloudScanClient?.startComprehensiveScan(cloudListener)
        }
    }

    override fun cancelScan() {
        runCatching { cloudScanClient?.cancelScan() }
        cloudScanClient = null
    }

    private fun AppInfo.toVirusScanItem(): VirusScanItem =
        VirusScanItem(
            packageName = packageName,
            apkPath = apkPath,
            appName = appName,
            virusName = virusName,
            summary = summary?.filterNotNull(),
            score = score,
        )

    private fun Int.toErrorKind(): VirusScanErrorKind =
        if (this in NETWORK_ERROR_CODES) {
            VirusScanErrorKind.Network
        } else {
            VirusScanErrorKind.Unknown
        }

    private companion object {
        private val NETWORK_ERROR_CODES =
            setOf(
                TrustlookError.NO_NETWORK,
                TrustlookError.SOCKET_TIMEOUT_EXCEPTION,
                TrustlookError.UNSTABLE_NETWORK,
            )
    }
}
