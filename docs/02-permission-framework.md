# Quick Clean Pro 权限框架

## 1. 概述

权限系统是一个通用的、类型参数化的框架，支持 **10 种权限类型** 和 **14 个受保护操作** 的双重维度管理。框架的核心设计遵循以下原则：

- **类型安全**：通过泛型 `PermissionManager<F : PermissionFeature>` 确保编译期类型检查
- **关注点分离**：权限检查逻辑、UI 对话框、系统交互各自独立
- **组合优于继承**：通过 `CompositionLocal` 将协调器注入 Compose 树，各功能模块按需消费
- **会话式状态机**：`PermissionSession` 追踪单个权限流程的完整生命周期

---

## 2. 权限类型 (PermissionType)

框架定义了 **10 种权限类型**，每种由对应的 `PermissionHandler` 实现类处理检查、请求和设置跳转。

| 类型 | 键值 | 保护的 Android 权限 / 能力 |
|------|------|---------------------------|
| `StorageFiles` | `storage_files` | `MANAGE_EXTERNAL_STORAGE` (Android R+) / `READ_EXTERNAL_STORAGE` + `WRITE_EXTERNAL_STORAGE` |
| `MediaImages` | `media_images` | `READ_MEDIA_IMAGES` (Android 13+) |
| `MediaImagesWithLocation` | `media_images_with_location` | `READ_MEDIA_IMAGES` + `ACCESS_MEDIA_LOCATION` (Android 13+) |
| `MediaVideo` | `media_video` | `READ_MEDIA_VIDEO` (Android 13+) |
| `MediaAudio` | `media_audio` | `READ_MEDIA_AUDIO` (Android 13+) |
| `Location` | `location` | `ACCESS_FINE_LOCATION` |
| `UsageAccess` | `usage_access` | `AppOpsManager.OPSTR_GET_USAGE_STATS` |
| `NotificationListener` | `notification_listener` | `EnabledNotificationListeners` (Settings.Secure) |
| `Overlay` | `overlay` | `SYSTEM_ALERT_WINDOW` |
| `PostNotifications` | `post_notifications` | `POST_NOTIFICATIONS` (Android 13+) |

---

## 3. 受保护操作 (CleanXProtectedAction)

**14 个用户可见的功能操作**，每个操作映射到一个或多个所需权限。框架在执行操作前通过 `PermissionCoordinator` 确保所需权限均已授予。

| 操作 | 所需权限 |
|------|---------|
| `JunkStartScan` | `StorageFiles` |
| `JunkCleanSelected` | `StorageFiles` |
| `FileManagerLoadFiles` | `StorageFiles` |
| `FileManagerDeleteFiles` | `StorageFiles` |
| `WhatsAppStartScan` | `StorageFiles` |
| `WhatsAppCleanSelected` | `StorageFiles` |
| `VirusDeepScanStart` | `StorageFiles` |
| `NetworkScanStart` | `Location` |
| `AppUsageLoadStats` | `UsageAccess` |
| `NetworkUsageLoadStats` | `UsageAccess` |
| `NotificationCleanerEnable` | `NotificationListener` |
| `AppLockOpenProtectedArea` | `UsageAccess` |
| `AppLockEnableMonitoring` | `UsageAccess` + `Overlay` |
| `AppLockRequestOverlay` | `Overlay` |
| `PostNotificationsEnable` | `PostNotifications` |

> **注意**：`AppLockEnableMonitoring` 是唯一需要 **多个权限** (`UsageAccess` + `Overlay`) 的操作，框架通过会话链式处理：授予第一个权限后自动进入第二个权限的请求流程。

---

## 4. PermissionHandler 接口

```kotlin
interface PermissionHandler {
    /**
     * 同步检查当前权限是否已授予。
     */
    fun isGranted(context: Context): Boolean

    /**
     * 返回该权限所需的运行时权限列表。
     * 例如 StorageFiles 可能返回空列表（当通过 MANAGE_ALL_FILES_ACCESS 处理时）。
     */
    fun runtimePermissions(context: Context): List<String>

    /**
     * 返回引导用户前往系统设置页面的 Intent 列表。
     * 某些权限可能需要多个设置步骤（例如 NotificationListener 需要先到达通知设置页）。
     */
    fun settingsIntents(context: Context): List<Intent>
}
```

### 具体实现示例

框架包含 **10 个具体实现类**，每个对应一种权限类型。以 `StorageFiles` 为例，其 Handler 会根据 API 级别做版本适配：

**文件路径**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/handler/StorageFilesHandler.kt`

```kotlin
class StorageFilesHandler : PermissionHandler {
    override fun isGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == GRANTED
        }
    }

    override fun runtimePermissions(context: Context): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            emptyList() // 通过 MANAGE_ALL_FILES_ACCESS 处理，不走运行时请求
        } else {
            listOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun settingsIntents(context: Context): List<Intent> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            listOf(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } else {
            listOf(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
        }
    }
}
```

---

## 5. PermissionManager - 核心引擎

**文件路径**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/PermissionManager.kt`

泛型权限管理器，参数化在 `PermissionFeature` 上：

```kotlin
class PermissionManager<F : PermissionFeature> {

    // ...
}
```

### 5.1 权限状态模型

```kotlin
sealed class PermissionStatus {
    object Granted : PermissionStatus()
    data class Denied(
        val permissions: List<PermissionType>,
        val denialReasons: Map<PermissionType, DenialReason>
    ) : PermissionStatus()
}

enum class DenialReason {
    NOT_GRANTED,
    PERMANENTLY_DENIED,
    UNAVAILABLE
}
```

### 5.2 核心方法

```kotlin
// 检查全部权限的授予状态
fun status(context: Context, feature: F): PermissionStatus

// 决策引擎：返回下一步要执行的操作计划
fun requestPlan(context: Context, feature: F): PermissionRequestPlan

// 处理系统 Activity 回调结果
fun onRuntimeResult(context: Context, feature: F, result: Map<String, Boolean>): PermissionRequestPlan

// 跳过运行时请求，直接提供设置页跳转计划
fun settingsPlan(context: Context, feature: F): PermissionRequestPlan
```

### 5.3 决策计划 (PermissionRequestPlan)

```kotlin
sealed class PermissionRequestPlan {
    /** 所有权限已授予，无需任何操作 */
    object AlreadyGranted : PermissionRequestPlan()

    /** 通过系统运行时权限弹窗请求 */
    data class RequestRuntime(val permissions: List<String>) : PermissionRequestPlan()

    /** 跳转到系统设置页面 */
    data class OpenSettings(val permission: PermissionType) : PermissionRequestPlan()

    /** 当前设备/版本不支持该权限 */
    object Unavailable : PermissionRequestPlan()
}
```

### 5.4 RuntimePermissionDenialStore

```kotlin
object RuntimePermissionDenialStore {
    // 追踪用户对运行时权限的拒绝状态，防止重复弹窗骚扰
    // key: permission string, value: 是否已被永久拒绝
}
```

当用户首次拒绝运行时权限时，框架记录该状态；后续 `requestPlan()` 会根据是否曾被拒绝来决定返回 `RequestRuntime` 还是 `OpenSettings`。

---

## 6. CleanXPermissionCoordinator - 会话状态机

**文件路径**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/CleanXPermissionCoordinator.kt`

协调器是功能模块与权限框架之间的主要对外接口。

### 6.1 接口定义

```kotlin
interface CleanXPermissionCoordinator {
    /** 检查某个操作或权限项是否已授予 */
    fun isGranted(target: PermissionTarget): Boolean

    /**
     * 完整的权限保护流程：
     * 创建 Session（弹窗） → 请求 → 重检 →
     * onGranted 或 onRejected 回调
     */
    fun guard(
        target: PermissionTarget,
        onRejected: () -> Unit,
        onGranted: () -> Unit
    )

    /** 跳过对话框，直接进入系统请求流程 */
    fun guardDirect(
        target: PermissionTarget,
        onRejected: () -> Unit,
        onGranted: () -> Unit
    )

    /** 直接请求指定权限项 */
    fun request(
        item: PermissionType,
        onRejected: () -> Unit,
        onGranted: () -> Unit
    )

    /** 直接打开指定权限项的设置页面 */
    fun openSettings(
        item: PermissionType,
        onRejected: () -> Unit,
        onGranted: () -> Unit
    )
}
```

### 6.2 guard() 完整流程

```
┌─────────────────────────────────────────────────────┐
│  ① 业务层调用 coordinator.guard(action, ...)        │
│     创建 PermissionSession (showDialog=true)         │
│         ↓                                            │
│  ② CleanXPermissionPromptHost 检测到 session         │
│     根据权限类型选择并显示对应对话框                    │
│         ↓                                            │
│  ③ 用户点击"允许" → requestPlan()                    │
│      ┌─ AlreadyGranted         → onGranted()         │
│      ├─ RequestRuntime(perms)  → 调用系统弹窗         │
│      ├─ OpenSettings(perm)     → 跳转系统设置         │
│      └─ Unavailable            → onRejected()        │
│         ↓                                            │
│  ④ 系统回调 → recheckAfterPermissionReturn()         │
│      ┌─ 全部授予               → onGranted()         │
│      └─ 仍有拒绝               → onRejected()        │
│         ↓                                            │
│  ⑤ 多权限链式处理 (如 AppLock 的 UsageAccess+Overlay)│
│     授予第一个后自动请求下一个 (showDialog=false)     │
└─────────────────────────────────────────────────────┘
```

### 6.3 PermissionSession 状态

```kotlin
data class PermissionSession(
    val id: String,
    val target: PermissionTarget,
    val showDialog: Boolean,       // 是否显示前置解释弹窗
    val onGranted: () -> Unit,
    val onRejected: () -> Unit
)
```

---

## 7. UI 层

### 7.1 CompositionLocal 注入

框架通过 Compose 的 `CompositionLocal` 机制将协调器注入整个 Compose 树：

```kotlin
// 文件：app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/ui/...
val LocalPermissionCoordinator = staticCompositionLocalOf<CleanXPermissionCoordinator> {
    error("CleanXPermissionCoordinator not provided")
}
```

功能模块通过以下方式获取协调器：

```kotlin
@Composable
fun SomeFeatureScreen() {
    val coordinator = LocalPermissionCoordinator.current

    Button(onClick = {
        coordinator.guard(
            target = PermissionTarget.Action(CleanXProtectedAction.JunkStartScan),
            onRejected = { /* 显示被拒绝提示 */ },
            onGranted = { startScan() }
        )
    }) {
        Text("开始扫描")
    }
}
```

### 7.2 对话框分发 (QuickCleanProPermissionUi.PermissionPrompt)

根据权限类型和操作类型，框架会显示不同的对话框：

```kotlin
fun PermissionPrompt(session: PermissionSession) {
    when {
        // 通知清理 → 特殊引导弹窗
        session is NotificationCleaner + NotificationListener ->
            NotificationListenerEnableGuide()

        // UsageAccess → AppLock 专用弹窗
        session.requiredPermission == UsageAccess ->
            AppLockUsageAccessPermissionDialog()

        // Overlay → AppLock 浮窗权限弹窗
        session.requiredPermission == Overlay ->
            AppLockOverlayPermissionDialog()

        // 默认 → 通用权限请求弹窗（通过 CleanXPermissionCopy 获取文案）
        else -> CleanXPermissionRequiredDialog(session)
    }
}
```

### 7.3 CleanXPermissionPromptHost

**文件路径**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/ui/PermissionPromptHost.kt`

核心 Host Composable，负责：

```kotlin
@Composable
fun CleanXPermissionPromptHost(
    state: CleanXPermissionCoordinatorState,
    // ...
) {
    // ① 注册系统运行时权限 launcher
    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> state.onRuntimeResult(result) }

    // ② 注册设置页 launcher
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { state.onSettingsReturn() }

    // ③ ON_RESUME 时触发 settingsReturnGeneration++
    //    用于区分"从设置返回"和常规 resume
    DisposableEffect(Unit) { /* lifecycle observer */ }

    // ④ LaunchedEffect 监听 pendingLaunch 执行系统操作
    LaunchedEffect(state.pendingLaunch) {
        when (val plan = state.pendingLaunch) {
            is RequestRuntime -> runtimeLauncher.launch(plan.permissions.toTypedArray())
            is OpenSettings -> settingsLauncher.launch(plan.intent)
            // ...
        }
    }
}
```

### 7.4 AppPermissionHost

**文件路径**：`app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/permission/AppPermissionHost.kt`

顶层 Composable，在 `AppRoot` 中包裹 `NavGraph`：

```kotlin
@Composable
fun AppPermissionHost(content: @Composable () -> Unit) {
    // ① 创建 Action Manager 和 Item Manager
    val actionManager = remember { PermissionManager<CleanXProtectedAction>() }
    val itemManager = remember { PermissionManager<PermissionType>() }

    // ② 创建协调器状态（管理 session 队列、pending launch）
    val coordinatorState = remember {
        CleanXPermissionCoordinatorState(actionManager, itemManager)
    }

    // ③ 通过 CompositionLocal 提供协调器
    CompositionLocalProvider(
        LocalPermissionCoordinator provides coordinatorState.coordinator
    ) {
        // ④ 内嵌 PermissionPromptHost 以处理对话框和系统交互
        CleanXPermissionPromptHost(
            state = coordinatorState,
            /* ... */
        ) {
            // ⑤ 通知外部权限流程状态变化（如暂停 splash 动画）
            OnPermissionFlowActiveChange { active ->
                // 暂停/恢复外部 UI
            }

            content()
        }
    }
}
```

---

## 8. 通知权限专用子系统

`POST_NOTIFICATIONS` 拥有一个独立的子系统，因为它需要在 Splash 和 Home 两个不同的入口时机触发，且需要更精细的冷却和频控。

### 8.1 架构组件

| 组件 | 文件路径 | 职责 |
|------|---------|------|
| `NotificationPermissionContract` | `.../permission/notification/NotificationPermissionContract.kt` | 定义包含 26 个字段的 `UiState` data class |
| `NotificationPermissionStateMachine` | `.../permission/notification/NotificationPermissionStateMachine.kt` | Pure function reducer，状态转换逻辑 |
| `NotificationPermissionViewModel` | `.../permission/notification/NotificationPermissionViewModel.kt` | MVI ViewModel，协调事件和状态 |
| `NotificationPermissionHost` | `.../permission/notification/NotificationPermissionHost.kt` | Compose Host，渲染 UI 和绑定生命周期 |

### 8.2 UiState (26 字段)

```kotlin
data class NotificationPermissionUiState(
    val permissionGranted: Boolean,
    val showRationaleDialog: Boolean,
    val showSystemDialog: Boolean,
    val dailyDialogCount: Int,
    val cooldownRemaining: Long,
    val splashVisible: Boolean,
    val homeVisible: Boolean,
    // ...共 26 个字段
)
```

### 8.3 关键流程

```
场景 A: Splash 可见时
┌──────────────────────────────────────┐
│ ① Splash 动画播放中                   │
│ ② 子系统触发权限请求                   │
│ ③ 暂停 Splash 动画                    │
│ ④ 调用系统 POST_NOTIFICATIONS 弹窗    │
│ ⑤ 回调后恢复 Splash 动画              │
└──────────────────────────────────────┘

场景 B: Home 可见时
┌──────────────────────────────────────┐
│ ① 检查是否在 5s cooldown 冷却期       │
│ ② 评估 rationale:                     │
│    ├─ 应显示 rationale → 自定义弹窗    │
│    │    (每日最多 1 次)                │
│    └─ 不应显示 rationale → 直接系统弹窗│
└──────────────────────────────────────┘

场景 C: 从设置页返回
┌──────────────────────────────────────┐
│ ON_RESUME → 立即刷新权限状态           │
└──────────────────────────────────────┘
```

---

## 9. 权限分析埋点

`PermissionAnalytics` 负责追踪权限流程中的关键事件，当前聚焦于 `StorageFiles` 相关操作。

**文件路径**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/analytics/PermissionAnalytics.kt`

```kotlin
object PermissionAnalytics {
    /**
     * 弹窗接受：用户在权限解释弹窗点击"允许"
     */
    fun trackFileManagerPopup(accepted: Boolean) {
        // 埋点上报："file_permission_popup" + accepted 状态
    }

    /**
     * 弹窗拒绝：用户在权限解释弹窗点击"拒绝" 或 "取消"
     * 同时触发 trackFileManagerPopup(accepted=false)
     */
    fun trackFilePermissionResult(granted: Boolean) {
        // 埋点上报："file_permission_result" + granted 状态
    }
}
```

### 埋点事件流

```
用户操作 → 弹窗出现
    ├─ 接受 → trackFileManagerPopup(accepted=true) → 系统弹窗 → 授予
    │         └→ trackFilePermissionResult(granted=true)
    └─ 拒绝 → trackFileManagerPopup(accepted=false)
               └→ trackFilePermissionResult(granted=false)
```

---

## 10. CleanXPermissionRegistry

**文件路径**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/permission/CleanXPermissionRegistry.kt`

注册中心负责将所有权限相关的规格说明集中管理。

### 10.1 Action Spec 注册

```kotlin
object CleanXPermissionRegistry {
    val actionSpecs: Map<CleanXProtectedAction, ActionSpec> = mapOf(
        JunkStartScan to ActionSpec(requiredPermissions = listOf(StorageFiles)),
        JunkCleanSelected to ActionSpec(requiredPermissions = listOf(StorageFiles)),
        FileManagerLoadFiles to ActionSpec(requiredPermissions = listOf(StorageFiles)),
        FileManagerDeleteFiles to ActionSpec(requiredPermissions = listOf(StorageFiles)),
        WhatsAppStartScan to ActionSpec(requiredPermissions = listOf(StorageFiles)),
        WhatsAppCleanSelected to ActionSpec(requiredPermissions = listOf(StorageFiles)),
        VirusDeepScanStart to ActionSpec(requiredPermissions = listOf(StorageFiles)),
        NetworkScanStart to ActionSpec(requiredPermissions = listOf(Location)),
        AppUsageLoadStats to ActionSpec(requiredPermissions = listOf(UsageAccess)),
        NetworkUsageLoadStats to ActionSpec(requiredPermissions = listOf(UsageAccess)),
        NotificationCleanerEnable to ActionSpec(requiredPermissions = listOf(NotificationListener)),
        AppLockOpenProtectedArea to ActionSpec(requiredPermissions = listOf(UsageAccess)),
        AppLockEnableMonitoring to ActionSpec(requiredPermissions = listOf(UsageAccess, Overlay)),
        AppLockRequestOverlay to ActionSpec(requiredPermissions = listOf(Overlay)),
        PostNotificationsEnable to ActionSpec(requiredPermissions = listOf(PostNotifications)),
    )

    val itemSpecs: Map<PermissionType, ItemSpec> = mapOf(
        StorageFiles to ItemSpec(handlerFactory = { StorageFilesHandler() }),
        MediaImages to ItemSpec(handlerFactory = { MediaImagesHandler() }),
        MediaImagesWithLocation to ItemSpec(handlerFactory = { MediaImagesWithLocationHandler() }),
        MediaVideo to ItemSpec(handlerFactory = { MediaVideoHandler() }),
        MediaAudio to ItemSpec(handlerFactory = { MediaAudioHandler() }),
        Location to ItemSpec(handlerFactory = { LocationHandler() }),
        UsageAccess to ItemSpec(handlerFactory = { UsageAccessHandler() }),
        NotificationListener to ItemSpec(handlerFactory = { NotificationListenerHandler() }),
        Overlay to ItemSpec(handlerFactory = { OverlayHandler() }),
        PostNotifications to ItemSpec(handlerFactory = { PostNotificationsHandler() }),
    )
}
```

### 10.2 Manager 工厂方法

```kotlin
fun actionManager(): PermissionManager<CleanXProtectedAction> {
    return PermissionManager(actionSpecs, itemSpecs)
}

fun itemManager(): PermissionManager<PermissionType> {
    return PermissionManager(itemSpecs)
}
```

---

## 11. 架构总览

```
┌──────────────────────────────────────────────────────────┐
│                     AppPermissionHost                      │
│  ┌──────────────────────────────────────────────────────┐ │
│  │            CompositionLocalProvider                   │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │           CleanXPermissionPromptHost              │ │ │
│  │  │  ┌─────────────┐  ┌──────────────┐              │ │ │
│  │  │  │ runtime     │  │ settings     │              │ │ │
│  │  │  │ Launcher    │  │ Launcher     │              │ │ │
│  │  │  └─────────────┘  └──────────────┘              │ │ │
│  │  │  ┌──────────────────────────────────────────────┐│ │ │
│  │  │  │          PermissionPrompt (Dialog)           ││ │ │
│  │  │  └──────────────────────────────────────────────┘│ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │   CleanXPermissionCoordinatorState               │ │ │
│  │  │   ┌────────────────┐  ┌──────────────────┐       │ │ │
│  │  │   │ Permission     │  │ Permission       │       │ │ │
│  │  │   │ Manager<Action>│  │ Manager<Item>    │       │ │ │
│  │  │   └────────────────┘  └──────────────────┘       │ │ │
│  │  │   ┌──────────────────────────────────────────────┐│ │ │
│  │  │   │      CleanXPermissionRegistry                ││ │ │
│  │  │   │  actionSpecs  +  itemSpecs                   ││ │ │
│  │  │   └──────────────────────────────────────────────┘│ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                         ↓                                  │
│  ┌──────────────────────────────────────────────────────┐ │
│  │               Feature Modules (NavGraph)              │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │ │
│  │  │ Junk     │ │ File     │ │ AppLock /            │ │ │
│  │  │ Cleaner  │ │ Manager  │ │ NotificationCleaner  │ │ │
│  │  └──────────┘ └──────────┘ └──────────────────────┘ │ │
│  │         ↓              ↓              ↓              │ │
│  │  coordinator.guard(action, onRejected, onGranted)    │ │
│  └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

## 12. 使用示例

### 示例 1：保护单个功能入口

```kotlin
@Composable
fun JunkCleanerEntry(
    onNavigateToScanner: () -> Unit,
    coordinator: CleanXPermissionCoordinator = LocalPermissionCoordinator.current
) {
    val context = LocalContext.current

    Button(onClick = {
        coordinator.guard(
            target = PermissionTarget.Action(CleanXProtectedAction.JunkStartScan),
            onRejected = {
                Toast.makeText(context, "需要存储权限才能扫描垃圾文件", Toast.LENGTH_SHORT).show()
            },
            onGranted = onNavigateToScanner
        )
    }) {
        Text("垃圾清理")
    }
}
```

### 示例 2：直接请求指定权限项

```kotlin
fun enableNotificationCleaning(coordinator: CleanXPermissionCoordinator) {
    coordinator.request(
        item = PermissionType.NotificationListener,
        onRejected = { /* 用户未授予通知监听权限 */ },
        onGranted = { /* 开始清理通知 */ }
    )
}
```

### 示例 3：检查权限状态

```kotlin
if (coordinator.isGranted(PermissionTarget.Item(PermissionType.UsageAccess))) {
    // 已有使用情况访问权限，直接加载数据
    loadUsageStats()
} else {
    // 需要先请求权限
    coordinator.guardDirect(
        target = PermissionTarget.Item(PermissionType.UsageAccess),
        onRejected = { /* fallback */ },
        onGranted = { loadUsageStats() }
    )
}
```

---

## 13. 关键设计决策

| 决策 | 理由 |
|------|------|
| 泛型 `PermissionManager<F>` | 允许 Action (CleanXProtectedAction) 和 Item (PermissionType) 共用同一套检查/请求逻辑，避免代码重复 |
| `PermissionSession` 状态机 | 将异步流程（弹窗 → 用户响应 → 系统回调 → 重检）转化为可追踪的状态序列，便于测试和调试 |
| `CompositionLocal` 注入 | 避免 Prop Drilling，各功能模块无需通过层层参数传递协调器 |
| 多权限链式处理 | `AppLockEnableMonitoring` 需要 `UsageAccess` + `Overlay`，框架在授予第一个后自动推进到下一个，无需业务层处理复杂性 |
| 通知权限独立子系统 | `POST_NOTIFICATIONS` 需要在 Splash/Home 两个不同时机触发，且有冷却和频控需求，独立 MVI 架构更清晰 |
| `RuntimePermissionDenialStore` | 追踪用户拒绝行为，避免重复弹出运行时权限对话框骚扰用户 |
