# Quick Clean Pro 权限框架

## 1. 概述

权限系统支持 10 种权限类型和 14 个受保护操作。核心设计原则：
- **关注点分离**：权限检查逻辑（PermissionEngine）、UI 对话框（QuickCleanProPermissionUi）、系统交互（PermissionPromptHost）各自独立
- **CompositionLocal 注入**：通过 CompositionLocal 将 PermissionCoordinator 注入 Compose 树
- **会话式状态机**：PermissionSession 追踪单个权限流程的完整生命周期
- **MVI 驱动通知权限**：通知权限拥有独立的 MVI 状态机 (NotificationPermissionPolicy + NotificationPermissionViewModel)

---

## 2. 权限类型 (PermissionType)

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/PermissionType.kt

10 种权限类型，每种由对应的 PermissionHandler 实现处理：

| 类型 | 键值 | 保护的 Android 权限 / 能力 |
|------|------|---------------------------|
| StorageFiles | storage_files | MANAGE_EXTERNAL_STORAGE (R+) / READ/WRITE_EXTERNAL_STORAGE |
| MediaImages | media_images | READ_MEDIA_IMAGES (TIRAMISU+) |
| MediaImagesWithLocation | media_images_with_location | READ_MEDIA_IMAGES + ACCESS_MEDIA_LOCATION |
| MediaVideo | media_video | READ_MEDIA_VIDEO (TIRAMISU+) |
| MediaAudio | media_audio | READ_MEDIA_AUDIO (TIRAMISU+) |
| Location | location | ACCESS_FINE_LOCATION |
| UsageAccess | usage_access | AppOpsManager.OPSTR_GET_USAGE_STATS |
| NotificationListener | notification_listener | EnabledNotificationListeners (Settings.Secure) |
| Overlay | overlay | SYSTEM_ALERT_WINDOW |
| PostNotifications | post_notifications | POST_NOTIFICATIONS (TIRAMISU+) |

---

## 3. 受保护操作 (ProtectedAction)

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/PermissionModels.kt

ProtectedAction 是一个 enum，每个枚举值直接声明其 requiredPermissions：

```kotlin
enum class ProtectedAction(
    val key: String,
    val requiredPermissions: List<PermissionType>,
) {
    JunkStartScan("junk_start_scan", listOf(PermissionType.StorageFiles)),
    JunkCleanSelected("junk_clean_selected", listOf(PermissionType.StorageFiles)),
    FileManagerLoadFiles("file_manager_load_files", listOf(PermissionType.StorageFiles)),
    FileManagerDeleteFiles("file_manager_delete_files", listOf(PermissionType.StorageFiles)),
    WhatsAppStartScan("whatsapp_start_scan", listOf(PermissionType.StorageFiles)),
    WhatsAppCleanSelected("whatsapp_clean_selected", listOf(PermissionType.StorageFiles)),
    VirusDeepScanStart("virus_deep_scan_start", listOf(PermissionType.StorageFiles)),
    NetworkScanStart("network_scan_start", listOf(PermissionType.Location)),
    AppUsageLoadStats("app_usage_load_stats", listOf(PermissionType.UsageAccess)),
    NetworkUsageLoadStats("network_usage_load_stats", listOf(PermissionType.UsageAccess)),
    NotificationCleanerEnable("notification_cleaner_enable", listOf(PermissionType.NotificationListener)),
    AppLockOpenProtectedArea("app_lock_open_protected_area", listOf(PermissionType.UsageAccess)),
    AppLockEnableMonitoring("app_lock_enable_monitoring", listOf(PermissionType.UsageAccess, PermissionType.Overlay)),
    AppLockRequestOverlay("app_lock_request_overlay", listOf(PermissionType.Overlay)),
}
```

> **不再有独立的 CleanXPermissionRegistry 文件** —— action 到权限的映射直接在 ProtectedAction 枚举中声明。

### PermissionTarget
```kotlin
sealed interface PermissionTarget {
    val key: String
    val requiredPermissions: List<PermissionType>
    data class Action(val action: ProtectedAction) : PermissionTarget
    data class Permission(val permission: PermissionType) : PermissionTarget
}
```

---

## 4. PermissionHandler 接口

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/PermissionHandler.kt

```kotlin
interface PermissionHandler {
    val permission: PermissionType  // 标识此 handler 处理的权限类型

    fun isGranted(context: Context): Boolean
    fun runtimePermissions(context: Context): List<String>
    fun settingsIntents(context: Context): List<Intent>
}
```

10 个实现类集中在 CommonPermissionHandlers.kt 一个文件中。

### RuntimePermissionDenialStore 接口

```kotlin
interface RuntimePermissionDenialStore {
    fun hasDenied(permission: PermissionType): Boolean
    fun markDenied(permission: PermissionType)
    fun hasRequestedBefore(permission: PermissionType): Boolean = hasDenied(permission)
    fun markRequested(permission: PermissionType) = Unit
    fun shouldRequestRuntimePermission(
        context: Context, permission: PermissionType, runtimePermissions: Array<String>
    ): Boolean = !hasDenied(permission)
}
```

实现类 AppRuntimePermissionDenialStore 基于 PermissionPreferences 持久化。

---

## 5. PermissionEngine - 核心引擎

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/PermissionEngine.kt

PermissionEngine 替代了旧版文档中的 PermissionManager<F>。它是非泛型的，直接接收 List<PermissionType>：

```kotlin
class PermissionEngine(
    handlers: List<PermissionHandler> = commonPermissionHandlers(),
    private val denialStore: RuntimePermissionDenialStore,
    ...
)
```

### 5.1 权限状态模型

```kotlin
data class PermissionStatus(
    val granted: Boolean,
    val missing: List<PermissionType>,
)
```

### 5.2 决策模型

```kotlin
sealed interface PermissionDecision {
    data object Granted : PermissionDecision
    data class RequestRuntime(val permissions: Array<String>) : PermissionDecision
    data class OpenSettings(val intents: List<Intent>) : PermissionDecision
    data object Unavailable : PermissionDecision
}
```

### 5.3 核心方法

```kotlin
// 检查权限状态
fun status(context: Context, requiredPermissions: List<PermissionType>): PermissionStatus

// 决策引擎：计算下一步操作
fun decide(context: Context, requiredPermissions: List<PermissionType>): PermissionDecision

// 直接获取设置页决策（跳过运行时请求）
fun settingsDecision(context: Context, permission: PermissionType): PermissionDecision

// 处理运行时权限回调结果
fun onRuntimeResult(result: Map<String, Boolean>)
```

### 5.4 decide() 决策流程

```
1. 遍历 requiredPermissions → 找到第一个未授予的 PermissionType
2. 获取该权限的 handler.runtimePermissions() → 过滤已授予的
3. 如果还有未授予的运行时权限 且 denialStore 允许请求
   → markRequested → 返回 RequestRuntime(permissions)
4. 否则 → 返回 settingsDecision() → OpenSettings 或 Unavailable
```

---

## 6. AppPermissionCoordinator 接口 + PermissionCoordinator 实现

### 6.1 接口定义

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/AppPermissionCoordinator.kt

```kotlin
interface AppPermissionCoordinator {
    fun isGranted(permission: PermissionType): Boolean

    fun ensure(
        action: ProtectedAction,
        mode: PermissionPromptMode = PermissionPromptMode.Explained,
        onDenied: () -> Unit = {},
        onGranted: () -> Unit,
    )

    fun ensure(
        permission: PermissionType,
        mode: PermissionPromptMode = PermissionPromptMode.Explained,
        onDenied: () -> Unit = {},
        onGranted: () -> Unit,
    )

    fun openSettings(
        permission: PermissionType,
        onReturn: () -> Unit = {},
    )
}
```

PermissionPromptMode 枚举:
- `Explained`: 先显示解释弹窗，再进入系统请求流程
- `Direct`: 跳过弹窗，直接进入系统请求

### 6.2 PermissionCoordinator 实现

文件: app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/permission/PermissionCoordinator.kt

PermissionCoordinator 是 AppPermissionCoordinator 的具体实现，同时维护会话状态：

```kotlin
internal class PermissionCoordinator(
    private val context: Context,
    private val engine: PermissionEngine,
) : AppPermissionCoordinator {
    var session by mutableStateOf<PermissionSession?>(null)
    var pendingLaunch by mutableStateOf<PermissionLaunch?>(null)
    // ...
}
```

### 6.3 ensure() 完整流程

```
① 业务层调用 coordinator.ensure(action, mode=Explained, ...)
   检查 status().granted → 已授予直接 onGranted()
   创建 PermissionSession (showDialog = mode == Explained)

② mode == Direct 时: 跳过弹窗，直接调用 launchDecision()
   mode == Explained 时: 等待用户交互

③ PermissionPromptHost 检测到 session.showDialog == true
   渲染对应的权限解释弹窗

④ 用户点击"允许" → onDialogSubmit()
   调用 engine.decide():
   ├─ Granted → finishIfGranted() → onGranted()
   ├─ RequestRuntime → pendingLaunch + session.showDialog=false
   │    → PermissionPromptHost 的 runtimeLauncher 调用系统弹窗
   ├─ OpenSettings → queueSettings()
   │    → PermissionPromptHost 的 settingsLauncher 跳转设置页
   └─ Unavailable → dismissUnavailable() → onDenied()

⑤ 系统回调:
   运行时回调 → onRuntimeResult() → engine.onRuntimeResult()
   设置返回 → onSettingsReturnIfReady()

⑥ recheckAfterPermissionReturn():
   ├─ Granted → onGranted()
   ├─ Continue(nextMissingPermission) → 链式处理多权限
   └─ Denied → onDenied()
```

### 6.4 PermissionSession

```kotlin
internal data class PermissionSession(
    val target: PermissionTarget,
    val missingPermission: PermissionType?,
    val onGranted: () -> Unit,
    val onDenied: () -> Unit,
    val showDialog: Boolean = true,
    val settingsLaunchPending: Boolean = false,
    val settingsLaunchObservedPause: Boolean = false,
)
```

### 6.5 PermissionLaunch

```kotlin
internal sealed interface PermissionLaunch {
    val target: PermissionTarget
    data class Runtime(val target: PermissionTarget, val permissions: Array<String>) : PermissionLaunch
    data class Settings(val target: PermissionTarget, val intents: List<Intent>) : PermissionLaunch
}
```

### 6.6 PermissionRecheckDecision

```kotlin
internal sealed interface PermissionRecheckDecision {
    data object Granted : PermissionRecheckDecision
    data class Continue(val missingPermission: PermissionType) : PermissionRecheckDecision
    data object Denied : PermissionRecheckDecision
}
```

resolvePermissionRecheck() 纯函数:
- 全部授予 → Granted
- 还有一个不同的权限缺失 → Continue (链式处理)
- 同一权限仍被拒绝 → Denied

---

## 7. UI 层

### 7.1 CompositionLocal 注入

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/ui/PermissionCompositionLocal.kt

```kotlin
val LocalPermissionCoordinator =
    staticCompositionLocalOf<AppPermissionCoordinator> {
        error("Permission coordinator is not available")
    }
```

功能模块使用:
```kotlin
val coordinator = LocalPermissionCoordinator.current
coordinator.ensure(
    action = ProtectedAction.JunkStartScan,
    onDenied = { /* 提示 */ },
    onGranted = { startScan() }
)
```

### 7.2 PermissionObservation

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/ui/PermissionObservation.kt

rememberPermissionGranted() Composable: 在 ON_RESUME 时自动刷新指定权限的状态，用于设置页等需要实时反映权限状态的场景。

### 7.3 对话框分发 (QuickCleanProPermissionUi.PermissionPrompt)

文件: app/src/main/java/com/quickcleanpro/phonecleaner/common/ui/permission/QuickCleanProPermissionUi.kt

```kotlin
fun PermissionPrompt(
    request: PermissionPromptRequest,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    when {
        // 通知清理 + NotificationListener → 特殊引导弹窗
        request is NotificationCleaner + NotificationListener ->
            NotificationEnableGuideDialog(...)

        // UsageAccess → AppLock 专用弹窗
        request.missingPermission == UsageAccess ->
            AppLockUsageAccessPermissionDialog(...)

        // Overlay → AppLock 浮窗权限弹窗
        request.missingPermission == Overlay ->
            AppLockOverlayPermissionDialog(...)

        // 默认 → 通用权限请求弹窗
        else -> CleanXPermissionRequiredDialog(request, onSubmit, onDismiss)
    }
}
```

### 7.4 PermissionPromptHost

文件: app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/permission/PermissionPromptHost.kt

```kotlin
@Composable
internal fun PermissionPromptHost(
    state: PermissionCoordinator,
    externalActivityLauncher: ExternalActivityLauncher,
    permissionPrompt: @Composable (
        request: PermissionPromptRequest,
        onSubmit: () -> Unit,
        onDismiss: () -> Unit,
    ) -> Unit,
)
```

核心能力:
- runtimeLauncher: RequestMultiplePermissions 注册和回调
- settingsLauncher: StartActivityForResult 注册和回调
- 生命周期: ON_PAUSE/ON_STOP → markSettingsLaunchObservedPause(), ON_RESUME → settingsReturnGeneration++
- LaunchedEffect(state.pendingLaunch): 消费 pendingLaunch 执行系统操作
- LaunchedEffect(settingsReturnGeneration): 触发 onSettingsReturnIfReady()
- 当 session.showDialog == true 时渲染 permissionPrompt()

### 7.5 AppPermissionHost

文件: app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/permission/AppPermissionHost.kt

顶层 Composable，在 AppRoot 中包裹 NavGraph:

```kotlin
@Composable
internal fun AppPermissionHost(
    externalActivityLauncher: ExternalActivityLauncher,
    onPermissionFlowActiveChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissionPreferences = koinInject<PermissionPreferences>()

    // ① 创建 PermissionEngine (单例)
    val engine = remember(permissionPreferences) {
        PermissionEngine(denialStore = AppRuntimePermissionDenialStore(permissionPreferences))
    }

    // ② 创建 PermissionCoordinator
    val coordinator = remember(context, engine) {
        PermissionCoordinator(context, engine)
    }

    // ③ 通知权限流程状态变化
    LaunchedEffect(coordinator.session != null) {
        onPermissionFlowActiveChange(coordinator.session != null)
    }

    // ④ 通过 CompositionLocal 提供
    CompositionLocalProvider(LocalPermissionCoordinator provides coordinator) {
        content()
        // ⑤ 渲染 PermissionPromptHost
        PermissionPromptHost(
            state = coordinator,
            externalActivityLauncher = externalActivityLauncher,
            permissionPrompt = QuickCleanProPermissionUi::PermissionPrompt,
        )
    }
}
```

> **关键变化**: AppPermissionHost 不再创建 actionManager/itemManager 两个分离的 Manager，而是创建一个 PermissionEngine 和一个 PermissionCoordinator。

---

## 8. 通知权限专用子系统

POST_NOTIFICATIONS 拥有独立的 MVI 子系统，因为在 Splash 和 Home 两个不同时机触发，需要更精细的冷却和频控。

### 8.1 架构组件

| 组件 | 文件路径 | 职责 |
|------|---------|------|
| PermissionPreferences | `common/permission/PermissionPreferences.kt` | 持久化存储 |
| NotificationPermissionPolicy | `app/runtime/permission/NotificationPermissionPolicy.kt` | MVI 类型 (UiState/Action/Effect/SideEffect) + reducer |
| NotificationPermissionViewModel | `app/runtime/permission/NotificationPermissionViewModel.kt` | ViewModel (snapshot刷新 + reducer + 副作用处理) |
| NotificationPermissionHost | `app/runtime/permission/NotificationPermissionHost.kt` | Compose Host (路由感知 + 系统交互) |

### 8.2 NotificationPermissionPolicy - MVI 类型

文件: app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/permission/NotificationPermissionPolicy.kt

**NotificationPermissionSnapshot** (外部事实):
```kotlin
data class NotificationPermissionSnapshot(
    val hasPermission: Boolean,
    val hasRequestedBefore: Boolean,
    val shouldShowRationale: Boolean,
    val lastCustomPromptAt: Long,
)
```

**NotificationPermissionUiState** (12 个字段):
- isSplashVisible, isHomeVisible: 当前页面可见性
- hasPermission, hasRequestedBefore, shouldShowRationale: 权限事实
- lastCustomPromptAt: 上次自定义弹窗时间
- customDialogVisible: 自定义弹窗是否可见
- requestSource: 当前请求来源 (Splash/HomeSystem/HomeCustom)
- settingsLaunchPending: 设置页跳转中
- suppressHomePromptUntilMillis: Home 提示冷却时间
- splashPaused: Splash 是否暂停
- permissionUiActive: 权限 UI 是否活跃
- homeCustomPromptDeferred: Home 自定义弹窗已延期

**NotificationPermissionAction** (9 种):
- VisibilityChanged: Splash/Home 可见性变化
- Refresh: 系统权限状态刷新
- PermissionResult: 系统权限回调结果
- HomePromptDelayElapsed / HomePromptCooldownElapsed: 定时器触发
- CustomPromptConfirmed / CustomPromptDismissed: 自定义弹窗交互
- SettingsLaunchResult: 设置页跳转结果

**NotificationPermissionEffect** (3 种):
- RequestSystemPermission(source): 请求系统权限弹窗
- OpenAppSettings: 打开应用设置页
- NotifyPermissionGranted: 通知权限已授予

**NotificationPermissionSideEffect** (5 种内部副作用):
- SaveRequestedBefore: 持久化已请求状态
- SaveLastCustomPromptAt: 持久化自定义弹窗时间
- TrackPopup(accepted): 埋点弹窗
- TrackPermissionResult(granted): 埋点权限结果
- Host(effect): 转发给 UI 层

### 8.3 状态机关键逻辑

**Splash 场景**:
1. Splash 可见 → VisibilityChanged
2. 未授权 + 未请求过 + 无活跃请求
   → 标记 hasRequestedBefore=true + splashPaused=true
   → 发出 RequestSystemPermission(Splash)
3. 系统回调 → PermissionResult → NotifyPermissionGranted 或解除暂停

**Home 场景 (核心 refreshPermission 逻辑)**:
1. Home 可见 → VisibilityChanged → refreshPermission
2. 已授权 → 清理所有状态 + NotifyPermissionGranted
3. 有 shouldShowRationale → 标记请求来源 + 5s cooldown → RequestSystemPermission(HomeSystem)
4. 无 rationale + 自定义弹窗未延期 + 每日一次限制 → 显示自定义弹窗
5. 自定义弹窗确认 → OpenAppSettings → 设置页返回 → Refresh → NotifyPermissionGranted 或继续

**自定义弹窗频控**: canShowNotificationPermissionCustomPrompt() 使用 Calendar 比较日期间隔，每日最多一次。

### 8.4 NotificationPermissionViewModel

文件: app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/permission/NotificationPermissionViewModel.kt

核心设计:
- Android 13+ 才启用 (runtimePermissionRequired)
- 每次 onAction 前调用 refreshSnapshot() 从系统/持久化同步最新事实
- reducer 计算新状态 + 副作用列表
- handleSideEffect() 分类处理: 内部副作用直接执行, Host 副作用通过 effectsChannel 转发 UI
- currentNoticeFlag(): 埋点场景标识 (1=Splash, 2=常规, 3=已完成清理)

---

## 9. 权限分析埋点

文件: app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/permission/PermissionAnalytics.kt

```kotlin
internal object PermissionAnalytics {
    fun trackDialogAccepted(target: PermissionTarget)  // StorageFiles 弹窗接受
    fun trackDismissed(target: PermissionTarget, dialogVisible: Boolean)  // 弹窗拒绝
    fun trackGranted(target: PermissionTarget)  // 权限最终授予
}
```

仅对 StorageFiles 相关 target 上报，其他权限类型不埋点。

---

## 10. 架构总览

```
┌──────────────────────────────────────────────────────────┐
│                     AppPermissionHost                      │
│  ┌──────────────────────────────────────────────────────┐ │
│  │          CompositionLocalProvider                     │ │
│  │  ┌──────────────────────────────────────────────────┐│ │
│  │  │            PermissionPromptHost                   ││ │
│  │  │  ┌─────────────┐  ┌──────────────┐              ││ │
│  │  │  │ runtime     │  │ settings     │              ││ │
│  │  │  │ Launcher    │  │ Launcher     │              ││ │
│  │  │  └─────────────┘  └──────────────┘              ││ │
│  │  │  ┌──────────────────────────────────────────────┐││ │
│  │  │  │  QuickCleanProPermissionUi.PermissionPrompt  │││ │
│  │  │  │  (Dialog dispatch)                            │││ │
│  │  │  └──────────────────────────────────────────────┘││ │
│  │  └──────────────────────────────────────────────────┘│ │
│  │  ┌──────────────────────────────────────────────────┐│ │
│  │  │           PermissionCoordinator                   ││ │
│  │  │   session: PermissionSession?                     ││ │
│  │  │   pendingLaunch: PermissionLaunch?                ││ │
│  │  │   ┌─────────────────────────────────────────────┐││ │
│  │  │   │           PermissionEngine                  │││ │
│  │  │   │  status() / decide() / onRuntimeResult()    │││ │
│  │  │   │  denialStore: AppRuntimePermissionDenialStore│││ │
│  │  │   │  handlers: commonPermissionHandlers()       │││ │
│  │  │   └─────────────────────────────────────────────┘││ │
│  │  │   ┌─────────────────────────────────────────────┐││ │
│  │  │   │         PermissionAnalytics                 │││ │
│  │  │   └─────────────────────────────────────────────┘││ │
│  │  └──────────────────────────────────────────────────┘│ │
│  └──────────────────────────────────────────────────────┘ │
│                         ↓                                  │
│  ┌──────────────────────────────────────────────────────┐ │
│  │               Feature Modules (NavGraph)              │ │
│  │  coordinator.ensure(action, onDenied=..., onGranted=...)│
│  └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

## 11. 使用示例

### 示例 1：保护功能入口 (Explained 模式)

```kotlin
val coordinator = LocalPermissionCoordinator.current
coordinator.ensure(
    action = ProtectedAction.JunkStartScan,
    mode = PermissionPromptMode.Explained,
    onDenied = { Toast.makeText(context, "需要存储权限", Toast.LENGTH_SHORT).show() },
    onGranted = { startScan() }
)
```

### 示例 2：直接请求系统权限 (Direct 模式)

```kotlin
coordinator.ensure(
    action = ProtectedAction.JunkStartScan,
    mode = PermissionPromptMode.Direct,
    onDenied = { /* 降级处理 */ },
    onGranted = { startScan() }
)
```

### 示例 3：检查权限状态

```kotlin
if (coordinator.isGranted(PermissionType.UsageAccess)) {
    loadUsageStats()
} else {
    coordinator.ensure(
        permission = PermissionType.UsageAccess,
        onDenied = { /* fallback */ },
        onGranted = { loadUsageStats() }
    )
}
```

### 示例 4：打开设置页

```kotlin
coordinator.openSettings(
    permission = PermissionType.StorageFiles,
    onReturn = { refreshPermissionStatus() }
)
```

---

## 12. 关键设计决策

| 决策 | 理由 |
|------|------|
| PermissionEngine 非泛型 | 实际使用中总是针对 List<PermissionType> 操作，泛型增加了不必要的复杂度 |
| ProtectedAction 枚举内联权限映射 | 避免单独的 Registry 文件，减少层级，action 和权限的关联关系一目了然 |
| PermissionPromptMode (Explained/Direct) | 用单一 ensure() 方法 + mode 参数替代 guard()/guardDirect()/request() 三个方法 |
| PermissionCoordinator 直接维护 session | 不再分离为接口+状态实现，简化了架构层级 |
| 通知权限独立 MVI | POST_NOTIFICATIONS 需要在 Splash/Home 两个时机触发，有冷却和频控需求，独立子系统更清晰 |
| RuntimePermissionDenialStore 接口 | 通过接口抽象，方便单元测试时 mock；实际使用 AppRuntimePermissionDenialStore 基于 PermissionPreferences 持久化 |
| PermissionPromptHost 接收 composable lambda | 而非直接渲染对话框，使得具体的对话框分发逻辑可替换 |

---

## 13. 文件索引

| 文件 | 包路径 |
|------|-------|
| PermissionType.kt | common/permission/ |
| PermissionModels.kt | common/permission/ |
| PermissionHandler.kt | common/permission/ |
| CommonPermissionHandlers.kt | common/permission/ |
| PermissionEngine.kt | common/permission/ |
| AppPermissionCoordinator.kt | common/permission/ |
| PermissionPreferences.kt | common/permission/ |
| PermissionPromptRequest.kt | common/permission/ |
| PermissionCompositionLocal.kt | common/permission/ui/ |
| PermissionObservation.kt | common/permission/ui/ |
| QuickCleanProPermissionUi.kt | common/ui/permission/ |
| AppLockPermissionPopups.kt | common/ui/permission/ |
| NotificationEnableGuideDialog.kt | common/ui/permission/ |
| AppPermissionHost.kt | app/runtime/permission/ |
| PermissionCoordinator.kt | app/runtime/permission/ |
| PermissionPromptHost.kt | app/runtime/permission/ |
| PermissionCoordinatorModels.kt | app/runtime/permission/ |
| PermissionAnalytics.kt | app/runtime/permission/ |
| NotificationPermissionPolicy.kt | app/runtime/permission/ |
| NotificationPermissionViewModel.kt | app/runtime/permission/ |
| NotificationPermissionHost.kt | app/runtime/permission/ |
