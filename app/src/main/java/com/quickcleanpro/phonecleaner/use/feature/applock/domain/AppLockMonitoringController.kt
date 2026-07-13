package com.quickcleanpro.phonecleaner.use.feature.applock.domain

interface AppLockMonitoringController {
    fun enableMonitoring()

    fun disableMonitoring()

    fun syncMonitoringService()
}
