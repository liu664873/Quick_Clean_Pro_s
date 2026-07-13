package com.quickcleanpro.phonecleaner.feature.applock

data class AppLockApp(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean,
)
