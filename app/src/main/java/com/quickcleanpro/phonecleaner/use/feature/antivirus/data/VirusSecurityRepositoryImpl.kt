package com.quickcleanpro.phonecleaner.use.feature.antivirus.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.quickcleanpro.phonecleaner.BuildConfig
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.TrustlookApiKeyState
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusSecurityRepository

internal class VirusSecurityRepositoryImpl(
    context: Context,
) : VirusSecurityRepository {
    private val appContext = context.applicationContext

    override fun hasInstalledAppsAccess(): Boolean {
        val packageManager = appContext.packageManager
        val currentPackageName = appContext.packageName
        val installedPackages = try {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    TRUSTLOOK_INSTALLED_PACKAGES_FLAGS_API_30_PLUS
                } else {
                    TRUSTLOOK_INSTALLED_PACKAGES_FLAGS_LEGACY
                },
            )
        } catch (_: Exception) {
            return false
        }

        return installedPackages.any { packageInfo ->
            packageInfo.packageName.isNotBlank() && packageInfo.packageName != currentPackageName
        }
    }

    override fun hasAdbRisk(): Boolean {
        val adbEnabled = try {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (_: Exception) {
            runCatching {
                Settings.Secure.getInt(appContext.contentResolver, Settings.Secure.ADB_ENABLED, 0) == 1
            }.getOrDefault(false)
        }
        if (!adbEnabled) return false

        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        return intent.resolveActivityInfo(appContext.packageManager, 0) != null
    }

    override fun trustlookApiKeyState(): TrustlookApiKeyState {
        val apiKey = BuildConfig.TRUSTLOOK_API_KEY
        return TrustlookApiKeyState(
            isConfigured = apiKey.isNotBlank(),
            logState = if (apiKey.isBlank()) {
                "blank"
            } else {
                "present(length=${apiKey.length}, suffix=${apiKey.takeLast(6)})"
            },
        )
    }

    private companion object {
        private const val TRUSTLOOK_INSTALLED_PACKAGES_FLAGS_API_30_PLUS = 131072
        private const val TRUSTLOOK_INSTALLED_PACKAGES_FLAGS_LEGACY = 64
    }
}
