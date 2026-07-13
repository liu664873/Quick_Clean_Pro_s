package com.quickcleanpro.phonecleaner.use.skin.files.common

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationTracker
import com.quickcleanpro.phonecleaner.use.core.common.operation.exitHandler
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.DeleteConfirmDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.NoResultsDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.StopScanDialog
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionCoordinator
import com.quickcleanpro.phonecleaner.use.skin.permission.rememberPermissionGranted
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.feature.files.presentation.common.BaseFileManagerViewModel
import com.quickcleanpro.phonecleaner.use.feature.files.domain.model.FileUri
import com.quickcleanpro.phonecleaner.use.skin.files.common.components.FileManagerPageBrush
import com.quickcleanpro.phonecleaner.use.skin.files.common.platform.requestMediaStoreDeleteOrDeleteDirectly

@Composable
internal fun FileManagerOperationEventsEffect(
    viewModel: BaseFileManagerViewModel,
    tracker: FeatureOperationTracker,
    onCompletionAdInFlightChanged: (Boolean) -> Unit = {},
) {
    var completionAdInFlight by remember { mutableStateOf(false) }

    fun setCompletionAdInFlight(value: Boolean) {
        completionAdInFlight = value
        onCompletionAdInFlightChanged(value)
    }

    BackHandler(enabled = completionAdInFlight) {}

    LaunchedEffect(viewModel, tracker) {
        viewModel.operationEvents.collect { event ->
            if (event.isFileManagerCompletionSuccess()) {
                if (!completionAdInFlight) {
                    setCompletionAdInFlight(true)
                    tracker.trackWithAd(event) {
                        setCompletionAdInFlight(false)
                        viewModel.showResultAfterCompletionAd()
                    }
                }
            } else {
                tracker.trackWithAd(event) {}
            }
        }
    }
}

private val FileManagerFeatureKeys =
    setOf(
        FeatureKey.PHOTOS,
        FeatureKey.SIMILAR_PHOTOS,
        FeatureKey.PHOTO_PRIVACY,
        FeatureKey.SCREENSHOTS,
        FeatureKey.VIDEOS,
        FeatureKey.AUDIOS,
        FeatureKey.LARGE_FILES,
        FeatureKey.DUPLICATE_FILES,
        FeatureKey.DOCUMENTS,
    )

private fun FeatureOperationEvent.isFileManagerCompletionSuccess(): Boolean =
    this is FeatureOperationEvent.OperationFinished &&
        feature in FileManagerFeatureKeys &&
        success

@Composable
internal fun rememberFileManagerPermissionState(
): FileManagerPermissionState {
    val observedPermissionGranted = rememberPermissionGranted(PermissionType.StorageFiles)
    var permissionGranted by remember {
        mutableStateOf(observedPermissionGranted)
    }
    var isLeavingPage by remember { mutableStateOf(false) }

    LaunchedEffect(observedPermissionGranted) {
        permissionGranted = observedPermissionGranted
    }

    return FileManagerPermissionState(
        granted = permissionGranted,
        leavingPage = isLeavingPage,
        markLeaving = { isLeavingPage = true },
        onPermissionChanged = { permissionGranted = it },
    )
}

internal class FileManagerPermissionState(
    val granted: Boolean,
    val leavingPage: Boolean,
    val markLeaving: () -> Unit,
    val onPermissionChanged: (Boolean) -> Unit,
)

internal fun FileManagerPermissionState.leaveHome(router: AppNavigator) {
    markLeaving()
    router.home()
}

internal fun FileManagerPermissionState.leaveBackForPermissionRejected(
    router: AppNavigator,
    feature: FeatureKey,
    tracker: FeatureOperationTracker,
) {
    markLeaving()
    tracker.exitHandler().exitAfterPermissionRejected(feature) {
        router.back()
    }
}

internal fun FileManagerPermissionState.leaveBackWithReturnAd(
    router: AppNavigator,
    feature: FeatureKey,
    tracker: FeatureOperationTracker,
) {
    markLeaving()
    tracker.exitHandler().exitBack(feature) {
        router.back()
    }
}

internal fun FileManagerPermissionState.leaveHomeWithReturnAd(
    router: AppNavigator,
    feature: FeatureKey,
    tracker: FeatureOperationTracker,
) {
    markLeaving()
    tracker.exitHandler().exitHome(feature) {
        router.home()
    }
}

@Composable
internal fun FileManagerScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable (androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    CleanXScaffoldPage(
        title = title,
        scrollEnabled = false,
        contentPadding = PaddingValues(0.dp),
        backgroundBrush = FileManagerPageBrush,
        onBack = onBack,
        actions = actions,
        bottomBar = bottomBar,
    ) {
        content()
    }
}

@Composable
internal fun FileManagerErrorToastEffect(
    errorMessage: String?,
    onConsumed: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        onConsumed()
    }
}

@Composable
internal fun FileManagerStartEffect(
    feature: FeatureKey,
    permissionState: FileManagerPermissionState,
    permissionCoordinator: CleanXPermissionCoordinator,
    tracker: FeatureOperationTracker,
    onStartIfNeeded: () -> Unit,
    onPermissionRejected: () -> Unit,
) {
    val latestStartIfNeeded by rememberUpdatedState(onStartIfNeeded)
    val latestPermissionRejected by rememberUpdatedState(onPermissionRejected)
    LaunchedEffect(permissionState.granted, permissionState.leavingPage) {
        if (permissionState.leavingPage) return@LaunchedEffect
        if (permissionState.granted) {
            latestStartIfNeeded()
            return@LaunchedEffect
        }
        permissionCoordinator.guard(
            action = CleanXProtectedAction.FileManagerLoadFiles,
            onGranted = {
                permissionState.onPermissionChanged(true)
                latestStartIfNeeded()
            },
            onRejected = {
                tracker.exitHandler().exitAfterPermissionRejected(feature) {
                    latestPermissionRejected()
                }
            },
        )
    }
}

@Composable
internal fun FileManagerStopOperationDialog(
    visible: Boolean,
    permissionGranted: Boolean,
    onQuit: () -> Unit,
    onResume: () -> Unit,
) {
    if (permissionGranted && visible) {
        StopScanDialog(
            onQuit = onQuit,
            onResume = onResume,
        )
    }
}

@Composable
internal fun FileManagerDeleteConfirmDialog(
    visible: Boolean,
    permissionGranted: Boolean,
    selectedUris: List<FileUri>,
    permissionCoordinator: CleanXPermissionCoordinator,
    onCancel: () -> Unit,
    onBeforeDeleteRequest: () -> Unit = {},
    onDeleteReady: () -> Unit,
    onRejected: () -> Unit,
) {
    val context = LocalContext.current
    val (launchDeleteRequest, deleteDirectly) = rememberMediaStoreDeleteLauncher(
        onConfirmed = onDeleteReady,
        onRejected = onRejected,
    )
    if (permissionGranted && visible) {
        DeleteConfirmDialog(
            onCancel = onCancel,
            onDelete = {
                permissionCoordinator.guard(CleanXProtectedAction.FileManagerDeleteFiles) {
                    onBeforeDeleteRequest()
                    requestMediaStoreDeleteOrDeleteDirectly(
                        context = context,
                        uris = selectedUris,
                        launchRequest = launchDeleteRequest,
                        deleteDirectly = deleteDirectly,
                    )
                }
            },
        )
    }
}

@Composable
internal fun FileManagerNoResultsDialog(
    visible: Boolean,
    permissionGranted: Boolean,
    onBack: () -> Unit,
) {
    if (permissionGranted && visible) {
        NoResultsDialog(onBack = onBack)
    }
}

@Composable
internal fun rememberMediaStoreDeleteLauncher(
    onConfirmed: () -> Unit,
    onRejected: () -> Unit,
): Pair<(IntentSenderRequest) -> Unit, () -> Unit> {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onConfirmed()
        } else {
            onRejected()
        }
    }
    return ({ request: IntentSenderRequest -> launcher.launch(request) }) to onConfirmed
}
