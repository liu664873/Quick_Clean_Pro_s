package com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation

import android.graphics.drawable.Drawable
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanMode

data class VirusThreat(
    val id: String,
    val packageName: String?,
    val apkPath: String?,
    val title: String,
    val description: String,
    val isFile: Boolean,
    val icon: Drawable?
)

data class VirusScanUiState(
    val mode: VirusScanMode? = null,
    val isScanning: Boolean = false,
    val scanCompleted: Boolean = false,
    val hasAdbRisk: Boolean = false,
    val isPathMode: Boolean = false,
    val currentLabel: String = "",
    val currentIcon: Drawable? = null,
    val threats: List<VirusThreat> = emptyList(),
    val appThreatCount: Int = 0,
    val fileThreatCount: Int = 0,
    val progressFraction: Float = 0f,
    val errorMessage: String? = null
) {
    val effectiveThreatCount: Int
        get() = threats.size + if (hasAdbRisk) 1 else 0
}
