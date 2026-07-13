package com.quickcleanpro.phonecleaner.use.skin.toolbox.notificationcleaner.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.common.operation.FeatureOperationEvent
import com.quickcleanpro.phonecleaner.use.core.common.operation.OperationAction
import com.quickcleanpro.phonecleaner.use.core.common.operation.trackReturnHome
import com.quickcleanpro.phonecleaner.use.core.feature.FeatureKey
import com.quickcleanpro.phonecleaner.use.core.model.notification.BlockableNotificationApp
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXBottomActionBar
import com.quickcleanpro.phonecleaner.use.skin.common.components.CleanXScaffoldPage
import com.quickcleanpro.phonecleaner.use.skin.common.components.PackageAppIcon
import com.quickcleanpro.phonecleaner.use.skin.common.components.animations.CleanXNotificationGuideAnimation
import com.quickcleanpro.phonecleaner.use.skin.common.components.buttons.CleanXPrimaryButton
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.NotificationBlockingTurnedOffDialog
import com.quickcleanpro.phonecleaner.use.skin.common.components.popups.StopScanDialog
import com.quickcleanpro.phonecleaner.use.skin.toolbox.common.ToolboxScanningContent
import com.quickcleanpro.phonecleaner.use.skin.common.theme.CleanXBlue
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.use.skin.navigation.AppRouteDependencies
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.notification.NotificationCleanerPage
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.notification.NotificationCleanerUiState
import com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.common.notification.NotificationCleanerViewModel
import kotlinx.coroutines.delay

private val CardBg = Color.White
private val Navy = Color(0xFF2D3748)
private val NavyMuted = Color(0xFF8190A5)
private val Divider15 = Color(0xFFE2EAF3)
private val CardRadius = 10.dp
private val NotificationStatusIconSize = 256.dp
private val NotificationRowHeight = 78.dp
private val NotificationBellBadgeSize = 68.dp
private val NotificationBellIconSize = 34.dp
private val NotificationSelectionSize = 21.dp
private val NotificationSelectionCheckSize = 15.dp
private val NotificationSelectionStrokeWidth = 2.dp

@Composable
internal fun NotificationCleanerScreenState(navigator: AppNavigator, dependencies: AppRouteDependencies, viewModel: NotificationCleanerViewModel) {
    val router = navigator
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionCoordinator = dependencies.permissions
    val tracker = dependencies.operations
    var showBlockingTurnedOffDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var completionAdInFlight by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshState()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, tracker) {
        viewModel.operationEvents.collect { event ->
            if (event.isNotificationCleanerCompletionSuccess()) {
                if (!completionAdInFlight) {
                    completionAdInFlight = true
                    tracker.trackWithAd(event) {
                        completionAdInFlight = false
                        viewModel.showStatusAfterCompletionAd()
                    }
                }
            } else {
                tracker.trackWithAd(event) {}
            }
        }
    }

    fun exitBackWithReturnAd() {
        tracker.trackReturnHome(FeatureKey.NOTIFICATION_CLEANER) {
            router.back()
        }
    }

    fun handleBack() {
        if (completionAdInFlight) return
        when (uiState.page) {
            NotificationCleanerPage.Settings -> viewModel.leaveSettings()
            NotificationCleanerPage.Scanning -> {
                showStopDialog = true
            }
            else -> exitBackWithReturnAd()
        }
    }

    BackHandler(onBack = ::handleBack)

    CleanXScaffoldPage(
        title = stringResource(R.string.notification_cleaner),
        onBack = ::handleBack,
        actions = {
            if (uiState.page == NotificationCleanerPage.Status) {
                IconButton(
                    onClick = { viewModel.showSettings() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = Navy,
                    )
                }
            }
        },
        bottomBar = {
            if (
                uiState.isInitialized &&
                uiState.errorMessage == null &&
                uiState.page == NotificationCleanerPage.Onboarding
            ) {
                CleanXBottomActionBar(
                    text = stringResource(R.string.notification_one_tap_tidy_up),
                    onClick = {
                        permissionCoordinator.guard(CleanXProtectedAction.NotificationCleanerEnable) {
                            viewModel.setBlockingEnabled(true)
                        }
                    },
                    backgroundColor = Color.Transparent,
                )
            }
        },
        scrollEnabled = uiState.page != NotificationCleanerPage.Onboarding,
    ) {
        when {
            !uiState.isInitialized -> Spacer(modifier = Modifier.height(220.dp))
            uiState.errorMessage != null -> ErrorCard(
                message = uiState.errorMessage.orEmpty(),
                onRetry = viewModel::refreshState,
            )
            else -> {
                when (uiState.page) {
                    NotificationCleanerPage.Onboarding -> OnboardingContent()
                    NotificationCleanerPage.Scanning -> ScanningContent(
                        paused = showStopDialog,
                        onFinished = viewModel::finishScanning,
                    )
                    NotificationCleanerPage.Status -> StatusContent(uiState = uiState)
                    NotificationCleanerPage.Settings -> SettingsContent(
                        uiState = uiState,
                        onEnabledChange = { checked ->
                            if (checked) {
                                permissionCoordinator.guard(CleanXProtectedAction.NotificationCleanerEnable) {
                                    viewModel.setBlockingEnabled(true)
                                }
                            } else {
                                showBlockingTurnedOffDialog = true
                            }
                        },
                        onTogglePackage = viewModel::togglePackage,
                    )
                }
            }
        }
        if (uiState.page != NotificationCleanerPage.Onboarding) {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showBlockingTurnedOffDialog) {
        NotificationBlockingTurnedOffDialog(
            onCancel = { showBlockingTurnedOffDialog = false },
            onConfirm = {
                showBlockingTurnedOffDialog = false
                viewModel.setBlockingEnabled(false)
            },
        )
    }

    if (showStopDialog) {
        StopScanDialog(
            onQuit = {
                showStopDialog = false
                exitBackWithReturnAd()
            },
            onResume = {
                showStopDialog = false
            },
        )
    }
}

private fun FeatureOperationEvent.isNotificationCleanerCompletionSuccess(): Boolean =
    this is FeatureOperationEvent.OperationFinished &&
        feature == FeatureKey.NOTIFICATION_CLEANER &&
        action == OperationAction.CLEAN &&
        success

@Composable
private fun OnboardingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CleanXNotificationGuideAnimation(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
        )
        Text(
            text = stringResource(R.string.notification_tidy_one_tap),
            color = Navy,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScanningContent(
    paused: Boolean,
    onFinished: () -> Unit,
) {
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        delay(1800L)
        onFinished()
    }

    ToolboxScanningContent(
        centerIconRes = R.drawable.ic_scan_notification_clean,
        captionText = stringResource(R.string.scan_loading_fallback),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun StatusContent(uiState: NotificationCleanerUiState) {
    val blockedRows =
        remember(uiState.apps, uiState.blockedCountsByPackage) {
            val appsByPackage = uiState.apps.associateBy { it.packageName }
            uiState.blockedCountsByPackage.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .map { (packageName, count) ->
                    (appsByPackage[packageName] ?: BlockableNotificationApp(packageName, packageName)) to count
                }
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(CardRadius),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BellBadge()
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(
                    if (uiState.enabled) {
                        R.string.notification_status_enabled
                    } else {
                        R.string.notification_status_disabled
                    },
                ),
                color = Navy,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = pluralStringResource(R.plurals.notifications_blocked_count, uiState.blockedCount, uiState.blockedCount),
                color = Navy,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.notification_showing_limit),
                color = NavyMuted,
                fontSize = 16.sp,
            )
            if (uiState.blockedCount == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(R.drawable.files_blank),
                    contentDescription = null,
                    modifier = Modifier.size(NotificationStatusIconSize),
                )
            }
        }
    }

    if (blockedRows.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        blockedRows.forEach { (app, count) ->
            BlockedAppRow(app = app, count = count)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: NotificationCleanerUiState,
    onEnabledChange: (Boolean) -> Unit,
    onTogglePackage: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(CardRadius),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification_blocked_notifications),
                    color = Navy,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.notification_block_settings_desc),
                    color = NavyMuted,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
            }
            Switch(
                checked = uiState.enabled,
                onCheckedChange = onEnabledChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CleanXBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD8DDE6),
                    ),
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    uiState.apps.forEach { app ->
        NotificationAppRow(
            app = app,
            enabled = uiState.enabled,
            selected = app.packageName in uiState.selectedPackages,
            onToggleSelection = { onTogglePackage(app.packageName) },
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun NotificationAppRow(
    app: BlockableNotificationApp,
    enabled: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(NotificationRowHeight)
                .clickable(enabled = enabled, onClick = onToggleSelection),
        color = CardBg,
        shape = RoundedCornerShape(CardRadius),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageAppIcon(
                packageName = app.packageName,
                fallbackText = app.appName.take(1).ifBlank { "A" },
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = app.appName,
                color = if (enabled) Navy else NavyMuted,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            SelectionBubble(selected = selected, enabled = enabled)
        }
    }
}

@Composable
private fun BlockedAppRow(
    app: BlockableNotificationApp,
    count: Int,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(NotificationRowHeight),
        color = CardBg,
        shape = RoundedCornerShape(CardRadius),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackageAppIcon(
                packageName = app.packageName,
                fallbackText = app.appName.take(1).ifBlank { "A" },
                modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = app.appName,
                color = Navy,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(R.plurals.notifications_blocked_count, count, count),
                color = NavyMuted,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun BellBadge() {
    Box(
        modifier =
            Modifier
                .size(NotificationBellBadgeSize)
                .clip(CircleShape)
                .background(Color(0xFFEAF3FF)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = Color(0xFF1877FF),
            modifier = Modifier.size(NotificationBellIconSize),
        )
    }
}

@Composable
private fun SelectionBubble(
    selected: Boolean,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .size(NotificationSelectionSize)
                .clip(CircleShape)
                .background(
                    when {
                        selected && enabled -> CleanXBlue
                        selected -> CleanXBlue.copy(alpha = 0.35f)
                        else -> Color.Transparent
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(NotificationSelectionCheckSize),
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFD7DEE8).copy(alpha = if (enabled) 1f else 0.35f),
                    radius = size.minDimension * 0.42f,
                    style = Stroke(width = NotificationSelectionStrokeWidth.toPx()),
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBg,
        shape = RoundedCornerShape(CardRadius),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.error),
                color = Navy,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = NavyMuted,
                fontSize = 15.sp,
            )
            CleanXPrimaryButton(
                text = stringResource(R.string.retry),
                onClick = onRetry,
            )
        }
    }
}
