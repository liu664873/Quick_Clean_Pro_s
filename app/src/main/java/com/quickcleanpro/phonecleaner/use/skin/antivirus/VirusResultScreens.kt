package com.quickcleanpro.phonecleaner.use.skin.antivirus

import com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation.openAppSettings
import com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation.openDeveloperSettings
import com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation.PackageRemovedReceiver
import com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation.safeDelete
import com.quickcleanpro.phonecleaner.use.feature.antivirus.presentation.VirusScanViewModel

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.common.operation.trackReturnHome
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.DeleteVirusFileDialog
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationTracker
import com.quickcleanpro.phonecleaner.use.skin.antivirus.views.NoVirusResultView
import com.quickcleanpro.phonecleaner.use.skin.antivirus.views.VirusThreatResultView
import java.io.File

@Composable
fun ScanVirusResultScreen(
    navigator: AppNavigator,
    dependencies: AppRouteDependencies,
    viewModel: VirusScanViewModel,
) {
    val router = navigator
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val externalActivityLaunchHandler = dependencies.externalActivities
    val tracker = dependencies.operations
    val deletionFailedText = stringResource(R.string.deletion_failed)
    var fileToDelete by remember { mutableStateOf<String?>(null) }

    fun exitBackWithReturnAd() {
        tracker.trackReturnHome(FeatureKey.ANTI_VIRUS) {
            router.back()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAdbRisk()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAdbRisk()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.effectiveThreatCount) {
        if (uiState.effectiveThreatCount == 0) {
            router.replace(AppDestination.NoVirusResult)
        }
    }

    DisposableEffect(context) {
        val receiver = PackageRemovedReceiver { packageName ->
            viewModel.removeThreatByPackage(packageName)
        }
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    fileToDelete?.let { path ->
        DeleteVirusFileDialog(
            onConfirm = {
                fileToDelete = null
                if (File(path).safeDelete(context)) {
                    viewModel.removeThreatByFilePath(path)
                } else {
                    Toast.makeText(context, deletionFailedText, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { fileToDelete = null }
        )
    }

    VirusThreatResultView(
        uiState = uiState,
        onSolveAdbRisk = {
            openDeveloperSettings(
                context = context,
                externalActivityLaunchHandler = externalActivityLaunchHandler,
            )
        },
        onSolveThreat = { threat ->
            if (threat.isFile) {
                threat.apkPath?.let { fileToDelete = it }
            } else {
                openAppSettings(
                    context = context,
                    packageName = threat.packageName,
                    externalActivityLaunchHandler = externalActivityLaunchHandler,
                )
            }
        },
        onBack = ::exitBackWithReturnAd,
    )

    BackHandler(onBack = ::exitBackWithReturnAd)
}

@Composable
fun NoVirusResultScreen(navigator: AppNavigator, tracker: FeatureOperationTracker) {
    val router = navigator

    fun exitBackWithReturnAd() {
        tracker.trackReturnHome(FeatureKey.ANTI_VIRUS) {
            router.back()
        }
    }

    BackHandler(onBack = ::exitBackWithReturnAd)

    CleanXScaffoldPage(
        title = stringResource(R.string.anti_virus),
        onBack = ::exitBackWithReturnAd,
        scrollEnabled = false,
        contentPadding = PaddingValues(0.dp),
    ) {
        NoVirusResultView(
            onNavigateTool = router::resetTo,
            modifier = Modifier.fillMaxSize()
        )
    }
}
