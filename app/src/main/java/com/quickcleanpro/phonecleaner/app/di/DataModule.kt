package com.quickcleanpro.phonecleaner.app.di

import com.quickcleanpro.phonecleaner.feature.antivirus.TrustlookVirusScanEngine
import com.quickcleanpro.phonecleaner.common.ads.AdvertiseSdkPrivacyGateway
import com.quickcleanpro.phonecleaner.common.ads.AdPrivacyGateway
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusSecurityRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.antivirus.AntivirusPreferencesImpl
import com.quickcleanpro.phonecleaner.feature.antivirus.AntivirusPreferences
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanEngine
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusSecurityRepository
import com.quickcleanpro.phonecleaner.feature.applock.AppLockMonitoringControllerImpl
import com.quickcleanpro.phonecleaner.feature.applock.AppLockRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.AndroidNetworkInfoReader
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.data.AppUsageRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.data.BatteryHistoryRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.DeviceInfoRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.files.shared.data.FileRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.files.duplicates.data.DuplicateFilesPreferencesImpl
import com.quickcleanpro.phonecleaner.feature.files.duplicates.data.DuplicateFilesPreferences
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingPreferencesImpl
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingPreferences
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.settings.SettingsRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.AndroidNotificationSettingsGateway
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationRepository
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationRepositoryImpl
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationSettingsGateway
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.data.BatteryHistorySamplerImpl
import com.quickcleanpro.phonecleaner.feature.applock.AppLockMonitoringController
import com.quickcleanpro.phonecleaner.feature.applock.AppLockRepository
import com.quickcleanpro.phonecleaner.feature.applock.AppLockMonitoringService
import com.quickcleanpro.phonecleaner.app.runtime.notification.PersistentAppLockMonitoringService
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.data.AppUsageRepository
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.data.BatteryHistorySampler
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.data.BatteryHistoryRepository
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanRepository
import com.quickcleanpro.phonecleaner.feature.junkclean.CleanSessionStore
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.device.DeviceInfoRepository
import com.quickcleanpro.phonecleaner.feature.files.shared.data.FileRepository
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkInfoReader
import com.quickcleanpro.phonecleaner.feature.toolbox.shared.network.NetworkRepository
import com.quickcleanpro.phonecleaner.feature.settings.SettingsRepository
import com.quickcleanpro.phonecleaner.feature.junkclean.scanner.SharedScanState
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsPreferences
import com.quickcleanpro.phonecleaner.common.analytics.DefaultAnalyticsPreferences
import com.quickcleanpro.phonecleaner.common.storage.preferences.AppPreferences
import com.quickcleanpro.phonecleaner.common.permission.PermissionPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val dataModule: Module =
    module {
        single { AppPreferences(androidContext()) }
        single { PermissionPreferences(get()) }
        single<AnalyticsPreferences> { DefaultAnalyticsPreferences(get()) }
        single<AdPrivacyGateway> { AdvertiseSdkPrivacyGateway() }
        single<DeviceInfoRepository> { DeviceInfoRepositoryImpl(androidContext()) }
        single<VirusScanEngine> { TrustlookVirusScanEngine(androidContext()) }
        single<VirusSecurityRepository> { VirusSecurityRepositoryImpl(androidContext()) }
        single<AntivirusPreferences> { AntivirusPreferencesImpl(get()) }
        single<AppLockRepository> { AppLockRepositoryImpl(androidContext()) }
        single<AppLockMonitoringService> { PersistentAppLockMonitoringService(androidContext()) }
        single<AppLockMonitoringController> { AppLockMonitoringControllerImpl(get(), get()) }
        single<AppUsageRepository> { AppUsageRepositoryImpl(androidContext()) }
        single<NetworkInfoReader> { AndroidNetworkInfoReader(androidContext()) }
        single<NetworkRepository> { NetworkRepositoryImpl(androidContext()) }
        single<FileRepository> { FileRepositoryImpl(androidContext()) }
        single<DuplicateFilesPreferences> { DuplicateFilesPreferencesImpl(get()) }
        single<OnboardingPreferences> { OnboardingPreferencesImpl(get()) }
        single<CleanSessionStore> { SharedScanState() }
        single<CleanRepository> { CleanRepositoryImpl(androidContext(), get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(get()) }
        single<NotificationRepository> { NotificationRepositoryImpl(androidContext()) }
        single<NotificationSettingsGateway> { AndroidNotificationSettingsGateway() }
        single<BatteryHistoryRepository> { BatteryHistoryRepositoryImpl(androidContext()) }
        single<BatteryHistorySampler> {
            BatteryHistorySamplerImpl(
                deviceInfoRepository = get(),
                historyRepository = get(),
            )
        }
    }
