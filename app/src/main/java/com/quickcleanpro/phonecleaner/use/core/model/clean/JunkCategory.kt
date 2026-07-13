package com.quickcleanpro.phonecleaner.use.core.model.clean

/**
 * 垃圾文件业务分类 *
 * 用于扫描、结果预览和清理汇总等主清理链路，
 * 因此放在 domain 层作为跨 data �?presentation 的稳定模型�? */
enum class JunkCategory(
    val displayName: String,
    val description: String,
) {
    CACHE("Cache Files", "App and system cache"),
    TEMP_FILE("Temp Files", "Temporary files in temp directories"),
    RESIDUAL("Residual Files", "Leftover files from uninstalled apps"),
    APK("APK Files", "Downloaded APK installers"),
    DUPLICATE("Duplicate Files", "Files with duplicate content"),
    LARGE_FILE("Large Files", "Files taking up too much space"),
}
