package com.quickcleanpro.phonecleaner.use.app.di

import com.quickcleanpro.phonecleaner.use.feature.antivirus.data.TrustlookVirusScanEngine
import com.quickcleanpro.phonecleaner.app.monetization.AdvertiseSdkPrivacyGateway
import com.quickcleanpro.phonecleaner.app.monetization.AdPrivacyGateway
import com.quickcleanpro.phonecleaner.use.feature.antivirus.data.VirusSecurityRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.antivirus.data.AntivirusPreferencesImpl
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.AntivirusPreferences
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusScanEngine
import com.quickcleanpro.phonecleaner.use.feature.antivirus.domain.VirusSecurityRepository
import com.quickcleanpro.phonecleaner.use.feature.applock.data.AppLockMonitoringControllerImpl
import com.quickcleanpro.phonecleaner.use.feature.applock.data.AppLockRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.toolbox.data.AndroidNetworkInfoReader
import com.quickcleanpro.phonecleaner.use.feature.toolbox.data.AppUsageRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.toolbox.battery.data.BatteryHistoryRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.junkclean.data.CleanRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.toolbox.data.DeviceInfoRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.files.data.FileRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.files.data.DuplicateFilesPreferencesImpl
import com.quickcleanpro.phonecleaner.use.feature.files.domain.DuplicateFilesPreferences
import com.quickcleanpro.phonecleaner.use.feature.onboarding.data.OnboardingPreferencesImpl
import com.quickcleanpro.phonecleaner.use.feature.onboarding.domain.OnboardingPreferences
import com.quickcleanpro.phonecleaner.use.feature.toolbox.data.NetworkRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.notification.data.NotificationRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.notification.data.AndroidNotificationSettingsGateway
import com.quickcleanpro.phonecleaner.use.feature.notification.data.NotificationSettingsGateway
import com.quickcleanpro.phonecleaner.use.feature.settings.data.SettingsRepositoryImpl
import com.quickcleanpro.phonecleaner.use.feature.toolbox.data.source.battery.BatteryHistorySamplerImpl
import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockMonitoringController
import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockRepository
import com.quickcleanpro.phonecleaner.use.feature.applock.domain.AppLockMonitoringService
import com.quickcleanpro.phonecleaner.use.app.runtime.notification.PersistentAppLockMonitoringService
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.AppUsageRepository
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.BatteryHistorySampler
import com.quickcleanpro.phonecleaner.use.core.repository.BatteryHistoryRepository
import com.quickcleanpro.phonecleaner.use.feature.junkclean.domain.CleanRepository
import com.quickcleanpro.phonecleaner.use.feature.junkclean.domain.CleanSessionStore
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.DeviceInfoRepository
import com.quickcleanpro.phonecleaner.use.feature.files.domain.FileRepository
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.NetworkInfoReader
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.NetworkRepository
import com.quickcleanpro.phonecleaner.use.core.repository.NotificationRepository
import com.quickcleanpro.phonecleaner.use.core.repository.SettingsRepository
import com.quickcleanpro.phonecleaner.use.feature.junkclean.data.scanner.source.SharedScanState
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val dataModule: Module =
    module {
        single<AdPrivacyGateway> { AdvertiseSdkPrivacyGateway() }
        single<DeviceInfoRepository> { DeviceInfoRepositoryImpl(androidContext()) }
        single<VirusScanEngine> { TrustlookVirusScanEngine(androidContext()) }
        single<VirusSecurityRepository> { VirusSecurityRepositoryImpl(androidContext()) }
        single<AntivirusPreferences> { AntivirusPreferencesImpl() }
        single<AppLockRepository> { AppLockRepositoryImpl(androidContext()) }
        single<AppLockMonitoringService> { PersistentAppLockMonitoringService(androidContext()) }
        single<AppLockMonitoringController> { AppLockMonitoringControllerImpl(get(), get()) }
        single<AppUsageRepository> { AppUsageRepositoryImpl(androidContext()) }
        single<NetworkInfoReader> { AndroidNetworkInfoReader(androidContext()) }
        single<NetworkRepository> { NetworkRepositoryImpl(androidContext()) }
        single<NotificationRepository> { NotificationRepositoryImpl(androidContext()) }
        single<NotificationSettingsGateway> { AndroidNotificationSettingsGateway() }
        single<FileRepository> { FileRepositoryImpl(androidContext()) }
        single<DuplicateFilesPreferences> { DuplicateFilesPreferencesImpl() }
        single<OnboardingPreferences> { OnboardingPreferencesImpl() }
        single<CleanSessionStore> { SharedScanState() }
        single<CleanRepository> { CleanRepositoryImpl(androidContext(), get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(androidContext()) }
        single<BatteryHistoryRepository> { BatteryHistoryRepositoryImpl(androidContext()) }
        single<BatteryHistorySampler> {
            BatteryHistorySamplerImpl(
                deviceInfoRepository = get(),
                historyRepository = get(),
            )
        }
    }
