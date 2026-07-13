package com.quickcleanpro.phonecleaner.feature.applock

interface AppLockMonitoringController {
    fun enableMonitoring()

    fun disableMonitoring()

    fun syncMonitoringService()
}
