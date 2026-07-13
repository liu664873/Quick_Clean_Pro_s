package com.quickcleanpro.phonecleaner.use.skin.common.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.use.core.analytics.AnalyticsTracker
import kotlinx.coroutines.delay
import java.util.Calendar

internal enum class NotificationPermissionRequestSource {
    Splash,
    HomeSystem,
    HomeCustom,
}

internal enum class NotificationPermissionHomePromptAction {
    None,
    RequestSystemPermission,
    ShowCustomDialog,
}

internal data class NotificationPermissionPromptState(
    val isSplashVisible: Boolean,
    val isHomeVisible: Boolean,
    val hasNotificationPermission: Boolean,
    val hasRequestedBefore: Boolean,
    val shouldShowRationale: Boolean,
    val lastCustomPromptAt: Long,
)

internal data class NotificationPermissionRefreshAction(
    val homePromptAction: NotificationPermissionHomePromptAction,
    val notifyPermissionGranted: Boolean,
)

private const val HOME_NOTIFICATION_PERMISSION_PROMPT_DELAY_MILLIS = 350L

internal fun shouldRequestSplashNotificationPermission(state: NotificationPermissionPromptState): Boolean =
    state.isSplashVisible && !state.hasNotificationPermission && !state.hasRequestedBefore

internal fun notificationPermissionRefreshAction(
    state: NotificationPermissionPromptState,
    returningFromSettings: Boolean,
    suppressHomePrompt: Boolean,
    nowMillis: Long,
    allowCustomPromptInCurrentSession: Boolean,
): NotificationPermissionRefreshAction =
    NotificationPermissionRefreshAction(
        homePromptAction =
            when {
                state.hasNotificationPermission ||
                    !state.isHomeVisible ||
                    returningFromSettings ||
                    suppressHomePrompt -> NotificationPermissionHomePromptAction.None
                !state.hasRequestedBefore -> NotificationPermissionHomePromptAction.None
                state.shouldShowRationale -> NotificationPermissionHomePromptAction.RequestSystemPermission
                !allowCustomPromptInCurrentSession -> NotificationPermissionHomePromptAction.None
                canShowNotificationPermissionCustomPrompt(state.lastCustomPromptAt, nowMillis) ->
                    NotificationPermissionHomePromptAction.ShowCustomDialog
                else -> NotificationPermissionHomePromptAction.None
            },
        notifyPermissionGranted = state.hasNotificationPermission,
    )

internal fun canShowNotificationPermissionCustomPrompt(
    lastPromptAt: Long,
    nowMillis: Long,
): Boolean {
    if (lastPromptAt <= 0L) return true
    if (nowMillis <= 0L) return true
    return !isSameLocalDay(lastPromptAt, nowMillis)
}

private fun isSameLocalDay(
    firstMillis: Long,
    secondMillis: Long,
): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

@Composable
internal fun NotificationPermissionPrompt(
    isSplashVisible: Boolean,
    isHomeVisible: Boolean,
    hasNotificationPermission: () -> Boolean,
    hasRequestedNotificationPermissionBefore: () -> Boolean,
    saveNotificationPermissionRequestedBefore: () -> Unit,
    shouldShowNotificationPermissionRationale: () -> Boolean,
    readLastCustomPromptAt: () -> Long,
    saveLastCustomPromptAt: (Long) -> Unit,
    openAppSettings: () -> Boolean,
    allowCustomPromptInCurrentSession: Boolean = true,
    onHomeSystemPermissionRejectedThisSession: () -> Unit = {},
    onPermissionGranted: () -> Unit,
    onSplashPermissionActiveChange: (Boolean) -> Unit,
    onPermissionUiActiveChange: (Boolean) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var promptState by remember {
        mutableStateOf(
            NotificationPermissionPromptState(
                isSplashVisible = isSplashVisible,
                isHomeVisible = isHomeVisible,
                hasNotificationPermission = hasNotificationPermission(),
                hasRequestedBefore = hasRequestedNotificationPermissionBefore(),
                shouldShowRationale = shouldShowNotificationPermissionRationale(),
                lastCustomPromptAt = readLastCustomPromptAt(),
            ),
        )
    }
    var showCustomDialog by remember { mutableStateOf(false) }
    var requestSource by remember { mutableStateOf<NotificationPermissionRequestSource?>(null) }
    var notificationSettingsLaunchPending by rememberSaveable { mutableStateOf(false) }
    var suppressHomePromptUntilMillis by remember { mutableLongStateOf(0L) }

    fun currentNoticeFlag(): Int =
        when {
            isSplashVisible -> 1
            AnalyticsTracker.hasCompletedCleanup() -> 3
            else -> 2
        }

    fun setPermissionUiActive(active: Boolean) {
        onPermissionUiActiveChange(active)
    }

    fun updatePromptState(
        returningFromSettings: Boolean = false,
        keepHomeVisibility: Boolean = isHomeVisible,
        keepSplashVisibility: Boolean = isSplashVisible,
    ): NotificationPermissionPromptState {
        val updated =
            NotificationPermissionPromptState(
                isSplashVisible = keepSplashVisibility,
                isHomeVisible = keepHomeVisibility,
                hasNotificationPermission = hasNotificationPermission(),
                hasRequestedBefore = hasRequestedNotificationPermissionBefore(),
                shouldShowRationale = shouldShowNotificationPermissionRationale(),
                lastCustomPromptAt = readLastCustomPromptAt(),
            )
        promptState = updated
        if (updated.hasNotificationPermission) {
            onPermissionGranted()
            showCustomDialog = false
            onSplashPermissionActiveChange(false)
            setPermissionUiActive(false)
        } else if (returningFromSettings) {
            setPermissionUiActive(false)
        }
        if (returningFromSettings) {
            AnalyticsTracker.trackNotificationPermissionResult(updated.hasNotificationPermission)
        }
        return updated
    }

    fun openNotificationSettings() {
        showCustomDialog = false
        requestSource = NotificationPermissionRequestSource.HomeCustom
        AnalyticsTracker.trackNotificationPopup(currentNoticeFlag(), ifOk = true)
        setPermissionUiActive(true)
        val launched = openAppSettings()
        notificationSettingsLaunchPending = launched
        if (!launched) {
            requestSource = null
            setPermissionUiActive(false)
        }
    }

    fun clearPermissionUi(clearSplash: Boolean = false) {
        if (clearSplash) {
            onSplashPermissionActiveChange(false)
        }
        setPermissionUiActive(false)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            val source = requestSource
            requestSource = null
            showCustomDialog = false
            AnalyticsTracker.trackNotificationPopup(currentNoticeFlag(), ifOk = isGranted)
            AnalyticsTracker.trackNotificationPermissionResult(isGranted)
            if (isGranted) {
                updatePromptState()
                clearPermissionUi(clearSplash = source == NotificationPermissionRequestSource.Splash)
                return@rememberLauncherForActivityResult
            }
            if (source == NotificationPermissionRequestSource.HomeSystem) {
                onHomeSystemPermissionRejectedThisSession()
            }
            promptState =
                promptState.copy(
                    hasNotificationPermission = false,
                    hasRequestedBefore = true,
                    shouldShowRationale = shouldShowNotificationPermissionRationale(),
                )
            suppressHomePromptUntilMillis = 0L
            clearPermissionUi(clearSplash = source == NotificationPermissionRequestSource.Splash)
        }

    fun launchPermissionRequest(source: NotificationPermissionRequestSource) {
        saveNotificationPermissionRequestedBefore()
        promptState =
            promptState.copy(
                hasRequestedBefore = true,
                shouldShowRationale = shouldShowNotificationPermissionRationale(),
            )
        requestSource = source
        if (source == NotificationPermissionRequestSource.Splash) {
            onSplashPermissionActiveChange(true)
        }
        setPermissionUiActive(true)
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun showDailyCustomPrompt() {
        val now = System.currentTimeMillis()
        saveLastCustomPromptAt(now)
        promptState = promptState.copy(lastCustomPromptAt = now)
        showCustomDialog = true
        setPermissionUiActive(true)
    }

    fun dismissCustomPrompt() {
        showCustomDialog = false
        AnalyticsTracker.trackNotificationPopup(currentNoticeFlag(), ifOk = false)
        setPermissionUiActive(false)
    }

    fun refreshPermission(returningFromSettings: Boolean) {
        if (returningFromSettings) {
            requestSource = null
        }
        val now = System.currentTimeMillis()
        val updated = updatePromptState(returningFromSettings = returningFromSettings)
        val action =
            notificationPermissionRefreshAction(
                state = updated,
                returningFromSettings = returningFromSettings,
                suppressHomePrompt = now < suppressHomePromptUntilMillis,
                nowMillis = now,
                allowCustomPromptInCurrentSession = allowCustomPromptInCurrentSession,
            )
        if (action.notifyPermissionGranted) {
            return
        }
        when (action.homePromptAction) {
            NotificationPermissionHomePromptAction.None -> {
                showCustomDialog = false
                setPermissionUiActive(false)
            }
            NotificationPermissionHomePromptAction.RequestSystemPermission -> {
                showCustomDialog = false
                suppressHomePromptUntilMillis = System.currentTimeMillis() + 5_000L
                launchPermissionRequest(NotificationPermissionRequestSource.HomeSystem)
            }
            NotificationPermissionHomePromptAction.ShowCustomDialog -> showDailyCustomPrompt()
        }
    }

    LaunchedEffect(isSplashVisible, isHomeVisible) {
        promptState =
            promptState.copy(
                isSplashVisible = isSplashVisible,
                isHomeVisible = isHomeVisible,
            )
    }

    LaunchedEffect(isSplashVisible, promptState.hasNotificationPermission, promptState.hasRequestedBefore) {
        if (
            shouldRequestSplashNotificationPermission(
                promptState.copy(
                    isSplashVisible = isSplashVisible,
                    isHomeVisible = isHomeVisible,
                ),
            )
        ) {
            launchPermissionRequest(NotificationPermissionRequestSource.Splash)
        }
    }

    LaunchedEffect(isSplashVisible, promptState.hasNotificationPermission) {
        if (!isSplashVisible || promptState.hasNotificationPermission) {
            onSplashPermissionActiveChange(false)
        }
    }

    LaunchedEffect(isHomeVisible) {
        if (isHomeVisible) {
            delay(HOME_NOTIFICATION_PERMISSION_PROMPT_DELAY_MILLIS)
            refreshPermission(returningFromSettings = false)
        } else {
            showCustomDialog = false
            suppressHomePromptUntilMillis = 0L
            setPermissionUiActive(false)
        }
    }

    LaunchedEffect(isHomeVisible, promptState.hasNotificationPermission, requestSource, suppressHomePromptUntilMillis) {
        val delayMillis = suppressHomePromptUntilMillis - System.currentTimeMillis()
        if (isHomeVisible && !promptState.hasNotificationPermission && requestSource == null && delayMillis > 0L) {
            delay(delayMillis)
            refreshPermission(returningFromSettings = false)
        }
    }

    DisposableEffect(lifecycleOwner, isHomeVisible) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && isHomeVisible) {
                    val returningFromSettings = notificationSettingsLaunchPending
                    notificationSettingsLaunchPending = false
                    refreshPermission(returningFromSettings = returningFromSettings)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showCustomDialog && !promptState.hasNotificationPermission && isHomeVisible) {
        Dialog(
            onDismissRequest = ::dismissCustomPrompt,
        ) {
            NotificationPermissionCustomDialog(
                onSubmit = ::openNotificationSettings,
                onCancel = ::dismissCustomPrompt,
            )
        }
    }
}

@Composable
private fun NotificationPermissionCustomDialog(
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val appName = stringResource(R.string.app_name)
            Text(
                text = stringResource(R.string.notification_enable_title),
                color = CleanXText,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text =
                    buildAnnotatedString {
                        append(stringResource(R.string.notification_enable_step_find_prefix))
                        withStyle(SpanStyle(color = CleanXBlue, fontWeight = FontWeight.Bold)) {
                            append(appName)
                        }
                        append(stringResource(R.string.notification_enable_step_find_suffix))
                    },
                color = Color(0xFF7D8EA8),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.notification_enable_step_allow),
                color = Color(0xFF7D8EA8),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                color = Color(0xFFF5F8FC),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularAppLogo(
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appName,
                        color = CleanXText,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    NotificationGuideSwitch()
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            NotificationGuideButton(
                text = stringResource(R.string.ok),
                filled = true,
                onClick = onSubmit,
            )
            Spacer(modifier = Modifier.height(12.dp))
            NotificationGuideButton(
                text = stringResource(R.string.cancel),
                filled = false,
                onClick = onCancel,
            )
        }
    }
}

@Composable
private fun NotificationGuideButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        onClick = onClick,
        color = if (filled) CleanXBlue else Color.White,
        shape = RoundedCornerShape(50),
        border = if (filled) null else BorderStroke(1.dp, CleanXBlue),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (filled) Color.White else CleanXBlue,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun NotificationGuideSwitch() {
    Box(
        modifier =
            Modifier
                .size(width = 31.dp, height = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(CleanXBlue)
                .padding(2.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier =
                Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White),
        )
    }
}
