package com.quickcleanpro.phonecleaner.app.di

import com.quickcleanpro.phonecleaner.common.coroutines.AppDispatchers
import com.quickcleanpro.phonecleaner.feature.antivirus.VirusScanViewModel
import com.quickcleanpro.phonecleaner.feature.applock.AppLockViewModel
import com.quickcleanpro.phonecleaner.feature.files.audios.AudiosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.documents.DocumentsManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.duplicates.DuplicateFilesManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.largefiles.LargeFilesManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.photoprivacy.PhotoPrivacyManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.photos.PhotosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.screenshots.ScreenshotsManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.similarphotos.SimilarPhotosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.files.videos.VideosManagerViewModel
import com.quickcleanpro.phonecleaner.feature.home.HomeViewModel
import com.quickcleanpro.phonecleaner.feature.onboarding.OnboardingScanViewModel
import com.quickcleanpro.phonecleaner.feature.junkclean.JunkCleanViewModel
import com.quickcleanpro.phonecleaner.feature.startup.SplashViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.AppUsageViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.battery.BatteryInfoViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.deviceinfo.DeviceInfoViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanDevicesViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkscan.NetworkScanViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkspeed.NetworkSpeedViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.networkusage.NetworkUsageViewModel
import com.quickcleanpro.phonecleaner.feature.toolbox.whatsappcleaner.WhatsAppCleanerViewModel
import com.quickcleanpro.phonecleaner.app.runtime.permission.NotificationPermissionViewModel
import com.quickcleanpro.phonecleaner.feature.settings.ManagePermissionsViewModel
import com.quickcleanpro.phonecleaner.feature.settings.SettingsViewModel
import com.quickcleanpro.phonecleaner.feature.notificationcleaner.NotificationCleanerViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import com.quickcleanpro.phonecleaner.app.MyApp
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule =
    module {
        single { AppDispatchers() }
        viewModel { HomeViewModel(get(), get(), get()) }
        viewModel {
            NotificationPermissionViewModel(
                application = androidApplication(),
                permissionPreferences = get(),
            )
        }
        viewModel { AppUsageViewModel(get(), get<AppDispatchers>().io) }
        viewModel { DeviceInfoViewModel(get(), get(), get(), get(), get<AppDispatchers>().io) }
        viewModel { BatteryInfoViewModel(get(), get(), get(), get(), get<AppDispatchers>().io) }
        viewModel { NetworkScanViewModel(get(), get(), get<AppDispatchers>().io) }
        viewModel { NetworkScanDevicesViewModel(get(), get<AppDispatchers>().io) }
        viewModel { NetworkSpeedViewModel(get(), get(), get<AppDispatchers>().io) }
        viewModel { NetworkUsageViewModel(get(), get<AppDispatchers>().io) }
        viewModel { WhatsAppCleanerViewModel(androidApplication(), get(), get<AppDispatchers>().io) }
        viewModel {
            NotificationCleanerViewModel(
                application = androidApplication(),
                repository = get(),
                ioDispatcher = get<AppDispatchers>().io,
                settingsGateway = get(),
            )
        }
        viewModel { OnboardingScanViewModel(get(), get()) }
        viewModel { JunkCleanViewModel(get(), get(), Dispatchers.IO) }
        viewModel { SplashViewModel(get(), (androidApplication() as MyApp).sdkInitialization) }
        viewModel { PhotosManagerViewModel(get(), Dispatchers.IO) }
        viewModel { ScreenshotsManagerViewModel(get(), Dispatchers.IO) }
        viewModel { VideosManagerViewModel(get(), Dispatchers.IO) }
        viewModel { AudiosManagerViewModel(get(), Dispatchers.IO) }
        viewModel { SimilarPhotosManagerViewModel(get(), Dispatchers.IO) }
        viewModel { PhotoPrivacyManagerViewModel(get(), Dispatchers.IO) }
        viewModel { LargeFilesManagerViewModel(get(), Dispatchers.IO) }
        viewModel { DocumentsManagerViewModel(get(), Dispatchers.IO) }
        viewModel { DuplicateFilesManagerViewModel(get(), get(), Dispatchers.IO) }
        viewModel { VirusScanViewModel(androidApplication(), get(), get(), get()) }
        viewModel { ManagePermissionsViewModel() }
        viewModel { SettingsViewModel(get()) }
        viewModel {
            AppLockViewModel(
                application = androidApplication(),
                repository = get(),
                monitoringController = get(),
                ioDispatcher = Dispatchers.IO,
            )
        }
    }
