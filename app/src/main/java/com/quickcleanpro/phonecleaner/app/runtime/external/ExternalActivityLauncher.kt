package com.quickcleanpro.phonecleaner.app.runtime.external

class ExternalActivityLauncher(
    val markLaunch: () -> Unit = {},
    val cancelLaunch: () -> Unit = {},
    val markReturn: () -> Unit = {},
)
