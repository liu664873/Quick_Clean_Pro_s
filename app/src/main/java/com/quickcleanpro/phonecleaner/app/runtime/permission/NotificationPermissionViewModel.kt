package com.quickcleanpro.phonecleaner.app.runtime.permission

import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModel
import com.quickcleanpro.phonecleaner.app.AppConfig
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsTracker
import com.quickcleanpro.phonecleaner.common.permission.PermissionPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 负责管理 Android 13+ 通知权限（POST_NOTIFICATIONS）的申请流程、状态同步及埋点。
 * 采用 MVI + 状态机模式，通过 Action 驱动状态迁移，并通过 Effect 与 UI 层通信。
 *
 * @param application 应用上下文，用于检查权限状态
 * @param permissionPreferences 持久化存储，记录是否已请求过及自定义弹窗时间
 * @param nowMillis 时间戳提供者，便于单元测试时 mock
 */
class NotificationPermissionViewModel(
    private val application: Application,
    private val permissionPreferences: PermissionPreferences,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    // 当前设备是否需要运行时权限（仅 Android 13+ 需要）
    private val runtimePermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // 状态机的当前状态（不可变快照），所有 UI 状态均由此派生
    private var machineState = initialState()

    // 暴露给 UI 的不可变状态流
    private val _uiState = MutableStateFlow(machineState)
    val uiState: StateFlow<NotificationPermissionUiState> = _uiState.asStateFlow()

    // 用于向 UI 层发送外部副作用（如显示弹窗、跳转设置等）的通道
    private val effectsChannel = Channel<NotificationPermissionEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    /**
     * 处理 UI 层或系统回调传递的 Action。
     * 若当前版本无需运行时权限，则直接忽略所有 Action。
     *
     * 执行流程：
     * 1. 刷新快照（从外部系统同步最新状态）
     * 2. 调用 reducer 计算新状态及副作用列表
     * 3. 更新内部状态并通知 UI
     * 4. 执行所有副作用（内部处理或转发给 UI）
     */
    fun onAction(action: NotificationPermissionAction) {
        if (!runtimePermissionRequired) return

        // 先根据 Action 刷新状态中的瞬时外部数据（如 shouldShowRationale 和 granted）
        machineState = refreshSnapshot(machineState, action)

        // 调用状态机核心 reducer，生成新状态和副作用
        val transition = reduceNotificationPermissionState(machineState, action, nowMillis())
        machineState = transition.state
        _uiState.value = machineState

        // 依次处理副作用
        transition.effects.forEach(::handleSideEffect)
    }

    /**
     * 处理由 reducer 产生的内部或外部副作用。
     * - 内部副作用：直接执行（保存偏好设置、埋点）
     * - 外部副作用：通过 effectsChannel 发送给 UI 层处理
     */
    private fun handleSideEffect(effect: NotificationPermissionSideEffect) {
        when (effect) {
            // 标记已请求过权限（避免 Splash 再次暂停）
            NotificationPermissionSideEffect.SaveRequestedBefore ->
                permissionPreferences.saveNotificationRuntimePermissionRequestedBefore()

            // 记录自定义弹窗展示时间（用于频率控制）
            is NotificationPermissionSideEffect.SaveLastCustomPromptAt ->
                permissionPreferences.saveLastNotificationPermissionCustomPromptAt(effect.timestampMillis)

            // 埋点：自定义弹窗展示（携带场景标识）
            is NotificationPermissionSideEffect.TrackPopup ->
                AnalyticsTracker.trackNotificationPopup(currentNoticeFlag(), ifOk = effect.accepted)

            // 埋点：系统权限结果
            is NotificationPermissionSideEffect.TrackPermissionResult ->
                AnalyticsTracker.trackNotificationPermissionResult(effect.granted)

            // 外部副作用：转发给 UI 层执行
            is NotificationPermissionSideEffect.Host -> effectsChannel.trySend(effect.effect)
        }
    }

    /**
     * 根据当前 Action 从外部系统重新读取最新状态，并更新到当前 state 的快照字段中。
     * 这是保证状态与系统真实状态一致的关键步骤。
     *
     * @param state 当前状态
     * @param action 触发的动作
     * @return 更新后的状态（仅更新与外部事实相关的字段）
     */
    private fun refreshSnapshot(
        state: NotificationPermissionUiState,
        action: NotificationPermissionAction,
    ): NotificationPermissionUiState {
        // 从 Action 中提取最新的 shouldShowRationale（仅特定 Action 携带）
        val rationale =
            when (action) {
                is NotificationPermissionAction.VisibilityChanged -> action.shouldShowRationale
                is NotificationPermissionAction.Refresh -> action.shouldShowRationale
                is NotificationPermissionAction.PermissionResult -> action.shouldShowRationale
                else -> state.shouldShowRationale // 其他 Action 沿用旧值
            }

        // 如果 Action 是权限结果，则用系统回调的 granted 值覆盖（优先级最高）
        val grantedOverride =
            (action as? NotificationPermissionAction.PermissionResult)?.granted

        // 从外部系统读取完整快照
        val snapshot = snapshot(rationale, grantedOverride)

        // 仅更新与外部事实相关的字段，保留状态机内部的其他字段（如 splashPaused 等）
        return state.copy(
            hasPermission = snapshot.hasPermission,
            hasRequestedBefore = snapshot.hasRequestedBefore,
            shouldShowRationale = snapshot.shouldShowRationale,
            lastCustomPromptAt = snapshot.lastCustomPromptAt,
        )
    }

    /**
     * 初始化 ViewModel 的起始状态。
     * 根据当前权限状态决定是否需要暂停 Splash 页面。
     *
     * @return 初始 UI 状态
     */
    private fun initialState(): NotificationPermissionUiState {
        // 初始快照，此时 shouldShowRationale 尚无意义，设为 false
        val snapshot = snapshot(shouldShowRationale = false)

        // 只有在 Android 13+、未授权且未请求过时，才暂停 Splash
        val shouldPauseSplash =
            runtimePermissionRequired &&
                    !snapshot.hasPermission &&
                    !snapshot.hasRequestedBefore

        return NotificationPermissionUiState(
            hasPermission = snapshot.hasPermission,
            hasRequestedBefore = snapshot.hasRequestedBefore,
            shouldShowRationale = snapshot.shouldShowRationale,
            lastCustomPromptAt = snapshot.lastCustomPromptAt,
            splashPaused = shouldPauseSplash,
        )
    }

    /**
     * 从外部系统（系统 API + 本地存储）读取权限相关的事实数据，组装成不可变快照。
     * 此方法不依赖状态机内部状态，是一个“纯”读取函数。
     *
     * @param shouldShowRationale UI 层同步来的系统建议标识
     * @param grantedOverride 如果提供，则优先使用此值作为权限授予状态（通常来自系统回调）
     * @return 包含最新权限事实的快照对象
     */
    private fun snapshot(
        shouldShowRationale: Boolean,
        grantedOverride: Boolean? = null,
    ): NotificationPermissionSnapshot =
        NotificationPermissionSnapshot(
            // 优先使用回调结果，否则实时查询系统权限
            hasPermission = grantedOverride ?: AppConfig.hasPostNotificationsPermission(application),
            hasRequestedBefore = permissionPreferences.hasRequestedNotificationRuntimePermissionBefore(),
            shouldShowRationale = shouldShowRationale,
            lastCustomPromptAt = permissionPreferences.readLastNotificationPermissionCustomPromptAt(),
        )

    /**
     * 计算当前自定义弹窗展示的场景标识，用于埋点区分不同用户旅程阶段。
     * 优先级：Splash 可见 > 已完成清理 > 默认
     *
     * 返回值含义：
     * 1 - 在 Splash 页拦截展示（通常转化率最低）
     * 3 - 用户已完成一次清理操作后展示（理想时机）
     * 2 - 其他常规场景（如设置页手动触发）
     *
     * 注意：此方法依赖 machineState.isSplashVisible，该值需由 UI 层通过 Action 同步。
     */
    private fun currentNoticeFlag(): Int =
        when {
            machineState.isSplashVisible -> 1
            AnalyticsTracker.hasCompletedCleanup() -> 3
            else -> 2
        }
}