package com.quickcleanpro.phonecleaner.use.feature.applock.domain.model

data class AppLockApp(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean,
)
