package com.quickcleanpro.phonecleaner.use.feature.applock.data

import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockMonitoringController
import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockRepository
import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockMonitoringService

internal class AppLockMonitoringControllerImpl(
    private val repository: AppLockRepository,
    private val monitoringService: AppLockMonitoringService,
) : AppLockMonitoringController {
    override fun enableMonitoring() {
        repository.setMonitoringEnabled(true)
        monitoringService.enable()
    }

    override fun disableMonitoring() {
        repository.setMonitoringEnabled(false)
        monitoringService.disable()
    }

    override fun syncMonitoringService() {
        if (repository.isPinSet() &&
            repository.isMonitoringEnabled() &&
            repository.lockedAppCount() > 0 &&
            repository.hasOverlayPermission() &&
            repository.hasUsageAccess()
        ) {
            monitoringService.enable()
        } else {
            monitoringService.disable()
        }
    }
}
