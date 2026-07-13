package com.quickcleanpro.phonecleaner.use.core.common.platform

data class ExternalActivityLaunchHandler(
    val markLaunch: () -> Unit = {},
    val cancelLaunch: () -> Unit = {},
    val markReturn: () -> Unit = {},
)
