package com.quickcleanpro.phonecleaner.feature.antivirus

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.quickcleanpro.phonecleaner.R
import java.io.File

internal class TrustlookConfigurationException : IllegalStateException("Trustlook API key is not configured")

internal fun scanErrorMessage(context: Context, kind: VirusScanErrorKind): String {
    return if (kind == VirusScanErrorKind.Network) {
        context.getString(R.string.scan_virus_network_failed)
    } else {
        context.getString(R.string.scan_virus_failed)
    }
}

internal fun logScanError(code: Int, message: String?, apiKeyState: String) {
    Log.w(
        "VirusScan",
        "Trustlook scan error code=$code message=${message.orEmpty()} apiKey=$apiKeyState",
    )
}

internal fun scanStartErrorMessage(context: Context, error: Throwable): String =
    if (error is TrustlookConfigurationException) {
        context.getString(R.string.scan_virus_authorization_missing)
    } else {
        scanErrorMessage(context, VirusScanErrorKind.Unknown)
    }

internal fun getAppLabelAndIcon(context: Context, packageName: String): Pair<String, Drawable?> {
    if (packageName.isBlank()) return "" to null
    return try {
        val packageManager = context.packageManager
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        }
        packageManager.getApplicationLabel(appInfo).toString() to packageManager.getApplicationIcon(packageName)
    } catch (error: Exception) {
        packageName to null
    }
}

internal fun VirusScanItem.toThreat(context: Context, isFile: Boolean): VirusThreat {
    val path = apkPath?.takeIf { it.isNotBlank() }
    val packageName = packageName?.takeIf { it.isNotBlank() }
    val description = threatDescription(context)
    return if (isFile || appName.isNullOrBlank()) {
        VirusThreat(
            id = "file:${path ?: description}",
            packageName = packageName,
            apkPath = path,
            title = path?.let { File(it).name } ?: virusName.orEmpty().ifBlank {
                context.getString(R.string.threat_file)
            },
            description = description,
            isFile = true,
            icon = null
        )
    } else {
        val fallback = getAppLabelAndIcon(context, packageName.orEmpty())
        VirusThreat(
            id = "app:${packageName ?: appName}",
            packageName = packageName,
            apkPath = path,
            title = appName.takeIf { it.isNotBlank() } ?: fallback.first,
            description = description,
            isFile = false,
            icon = fallback.second
        )
    }
}

internal fun Application.getProtectionIcon(): Drawable? {
    return runCatching { getDrawable(R.drawable.ic_virus_protection_shield) }.getOrNull()
}

private fun VirusScanItem.threatDescription(context: Context): String {
    val summaryText = runCatching {
        if (summary != null && summary.size > 1) summary[1] else null
    }.getOrNull()
    return summaryText?.takeIf { it.isNotBlank() }
        ?: virusName?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.high_risk)
}

