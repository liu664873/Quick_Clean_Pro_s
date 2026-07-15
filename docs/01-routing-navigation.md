# 路由导航框架

## 1. 概述

Quick Clean Pro 使用 **Jetpack Navigation Compose** 作为底层路由框架，在此基础上构建了一套自定义的**类型安全路由层**。该路由层通过 sealed class 枚举所有目的地，提供编译期安全的路由查找，并与广告门控、通知深链接、ViewModel 共享等机制深度集成。

核心设计目标：

- **类型安全**: 所有路由通过 `AppDestination` sealed class 定义，避免字符串路由的运行时错误
- **广告门控**: 功能入口点自动插入插屏广告流程
- **通知深链接**: 支持从系统通知直接跳转到任意页面
- **子路由 ViewModel 共享**: 详情页与列表页共享同一个 ViewModel

---

## 2. AppDestination — 路由注册中心

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/AppDestination.kt`

`AppDestination` 是一个 **sealed class**，每个 `data object` 代表应用中的一个页面。共定义 **38 个目的地**。

### 2.1 关键属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `route` | `String` | 导航路由字符串，如 `"home"`、`"scan"`、`"virus_result"` |
| `featureKey` | `FeatureKey?` | 关联的功能枚举，非功能页面为 `null` |
| `featureGroup` | `FeatureGroup?` | 功能分组：`HOME` / `FILES` / `TOOLBOX` |
| `featureEntry` | `Boolean` | 是否为功能入口点（共 22 个入口） |
| `notificationAliases` | `Set<String>` | 通知深链接的替代路由名 |
| `notificationTarget` | `Boolean` | 是否可从通知直接到达 |

### 2.2 Companion Object — 静态查找索引

```kotlin
companion object {
    val byRoute: Map<String, AppDestination>      // O(1) 路由查找
    val byFeature: Map<FeatureKey, AppDestination> // 功能 → 入口映射
    val startupRoutes: Set<String>                 // {"splash", "onboarding_scan"}
    val homeRoutes: Set<String>                    // {"home", "home_file_manager", "home_toolbox"}
    val rootRoutes: Set<String>                    // startupRoutes + homeRoutes
    val notificationTargets: Set<AppDestination>
    val notificationAliasToRoute: Map<String, String>
}
```

- **`byRoute`**: 所有目的地的 `route → AppDestination` 映射，支持 O(1) 查找
- **`byFeature`**: 通过 `FeatureKey` 快速定位到对应的功能入口 `AppDestination`
- **`rootRoutes`**: 根路由集合，用于判断 `back()` 时是否应退出应用

### 2.3 关键方法

#### `forRoute(route: String): AppDestination?`

支持详情子路由匹配。例如路由字符串 `"photos_detail/3"` 会匹配到 `PhotosDetail`（提取基础路由进行查找）。

```kotlin
fun forRoute(route: String): AppDestination? {
    val baseRoute = route.substringBefore("/")
    return byRoute[baseRoute] ?: notificationAliasToRoute[route]?.let { byRoute[it] }
}
```

#### `normalizeNotificationRoute(rawRoute: String): String?`

对通知传入的原始路由字符串进行规范化处理：
- 去除首尾 `/`
- 统一为小写
- 匹配别名映射表

---

## 3. AppNavigator — 导航器接口

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/AppNavigator.kt`

### 3.1 接口定义

```kotlin
interface AppNavigator {
    val currentRoute: String?

    fun open(destination: AppDestination, args: Map<String, String> = emptyMap())
    fun openRoute(route: String)
    fun replace(destination: AppDestination)
    fun resetTo(destination: AppDestination)
    fun home(): Boolean
    fun back(): Boolean
}
```

| 方法 | 说明 |
|------|------|
| `open()` | 导航到指定目的地，支持传递路径参数 |
| `openRoute()` | 通过路由字符串直接导航 |
| `replace()` | 替换当前页面（pop + navigate） |
| `resetTo()` | 清空栈后导航到目标页面 |
| `home()` | 返回首页；查找栈中已有 Home 路由并 pop 到该位置 |
| `back()` | 返回上一页；若当前为 root 路由则返回 `false` |

### 3.2 NavHostControllerAppNavigator 实现

```kotlin
class NavHostControllerAppNavigator(
    private val navController: NavHostController
) : AppNavigator {

    override val currentRoute: String?
        get() = navController.currentBackStackEntry?.destination?.route

    override fun open(destination: AppDestination, args: Map<String, String>) {
        navController.navigate(destination.route) {
            launchSingleTop = true
        }
    }

    override fun openRoute(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    override fun replace(destination: AppDestination) {
        navController.popBackStack()
        navController.navigate(destination.route)
    }

    override fun resetTo(destination: AppDestination) {
        navController.navigateHomeClearingStack()
        navController.navigate(destination.route)
    }

    override fun home(): Boolean {
        // 查找栈中已有的 Home 路由，pop 到该位置
        val homeEntry = navController.backQueue
            .find { AppDestination.homeRoutes.contains(it.destination.route) }
        return if (homeEntry != null) {
            navController.popBackStack(homeEntry.destination.route, inclusive = false)
            true
        } else {
            navController.navigateHomeClearingStack()
            true
        }
    }

    override fun back(): Boolean {
        return if (currentRoute in AppDestination.rootRoutes) {
            false // 根路由不让回退，交由 Activity 处理
        } else {
            navController.popBackStack()
            true
        }
    }
}
```

关键设计点：
- `launchSingleTop = true` 防止重复导航到同一页面
- `navigateHomeClearingStack()` 是一个扩展函数，清空整个回退栈后导航到 Home
- `back()` 在根路由时返回 `false`，由宿主 Activity 决定是否退出应用

---

## 4. FeatureEntryRouter — 广告门控装饰器

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/FeatureEntryRouter.kt`

`FeatureEntryRouter` 使用 Kotlin **委托（`by navigator`）** 模式包装 `AppNavigator`，在导航到功能入口前自动检查是否需要展示插屏广告。

### 4.1 工作原理

```kotlin
class FeatureEntryRouter(
    private val navigator: AppNavigator,
    private val interstitialAds: InterstitialAds
) : AppNavigator by navigator {

    override fun open(destination: AppDestination, args: Map<String, String>) {
        val fromRoute = navigator.currentRoute ?: ""
        val targetRoute = destination.route

        interstitialAds.runRouteEntry(fromRoute, targetRoute) {
            // 广告流程的 onContinue 回调
            navigator.open(destination, args)
        }
    }

    override fun openRoute(route: String) {
        val fromRoute = navigator.currentRoute ?: ""
        interstitialAds.runRouteEntry(fromRoute, route) {
            navigator.openRoute(route)
        }
    }
}
```

### 4.2 门控流程

1. 用户点击功能入口
2. `FeatureEntryRouter.open()` 被调用
3. `interstitialAds.runRouteEntry(fromRoute, targetRoute)` 根据来源和目标路由判断是否需要展示广告
4. 若需要展示广告，先展示广告，广告关闭后在 `onContinue` 回调中执行实际导航
5. 若不需要展示广告，直接执行 `onContinue` 回调

`back()`、`home()`、`replace()` 等方法直接委托给真实导航器，不走广告门控。

---

## 5. AppNavigation — NavGraph 组合

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/AppNavigation.kt`

### 5.1 AppNavGraph Composable

```kotlin
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    interstitialAds: InterstitialAds,
    featureFlow: FeatureFlow,
    externalActivities: ExternalActivities,
    // ... 其他依赖
) {
    val appNavigator = remember {
        FeatureEntryRouter(
            NavHostControllerAppNavigator(navController),
            interstitialAds
        )
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { AppRouteTransitions.enterTransition(it) },
        exitTransition = { AppRouteTransitions.exitTransition(it) },
        popEnterTransition = { AppRouteTransitions.popEnterTransition(it) },
        popExitTransition = { AppRouteTransitions.popExitTransition(it) }
    ) {
        registerSplashRoute(appNavigator, featureFlow)
        registerOnboardingRoute(appNavigator, featureFlow)
        registerHomeRoutes(appNavigator, featureFlow, externalActivities)
        registerCleanRoutes(appNavigator, featureFlow)
        registerAntiVirusRoutes(appNavigator, featureFlow)
        registerAppLockRoutes(appNavigator)
        registerToolboxRoutes(appNavigator, featureFlow)
        registerFileManagerRoutes(appNavigator, featureFlow)
        registerSettingsRoutes(appNavigator, featureFlow)
    }
}
```

### 5.2 路由组一览

| 路由组 | 包含路由数 | 说明 |
|--------|-----------|------|
| `registerSplashRoute` | 1 | 启动/闪屏页 |
| `registerOnboardingRoute` | 1 | 引导页 |
| `registerHomeRoutes` | 3 | 3 个 tab（home / home_file_manager / home_toolbox） |
| `registerCleanRoutes` | 1 | 垃圾清理入口 |
| `registerAntiVirusRoutes` | 5 | 反病毒：入口、快速扫描、深度扫描、有病毒/无病毒结果 |
| `registerAppLockRoutes` | 1 | 应用锁 |
| `registerToolboxRoutes` | 9 | 工具箱：设备信息、电池、应用用量、网络扫描等 |
| `registerFileManagerRoutes` | 18 | 11 个管理器 + 7 个详情页 |
| `registerSettingsRoutes` | 2 | 设置、权限管理 |

每个路由注册函数接收 `appNavigator`、`featureFlow` 等能力注入，确保各页面的 Composable 可以获取所需的导航和业务能力。

---

## 6. AppRouteTransitions — 过渡动画

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/AppRouteTransitions.kt`

### 6.1 动画配置

所有过渡动画时长统一为 **280ms**。

```kotlin
object AppRouteTransitions {
    private const val DURATION = 280

    // 进入动画：Splash 无动画或淡入；其他页面从右侧滑入 + 淡入
    fun enterTransition(initial: NavBackStackEntry): AnimatedContentTransitionScope<*>.() -> EnterTransition

    // 退出动画：向左滑出 + 淡出
    fun exitTransition(initial: NavBackStackEntry): AnimatedContentTransitionScope<*>.() -> ExitTransition

    // 回退进入动画：从左侧滑入 + 淡入
    fun popEnterTransition(initial: NavBackStackEntry): AnimatedContentTransitionScope<*>.() -> EnterTransition

    // 回退退出动画：向右滑出 + 淡出
    fun popExitTransition(initial: NavBackStackEntry): AnimatedContentTransitionScope<*>.() -> ExitTransition
}
```

### 6.2 动画方向规则

| 操作 | 方向 | 动画 |
|------|------|------|
| Forward（前进导航） | ← 左滑 | 新页面从右滑入，旧页面向左滑出 |
| Back（回退导航） | → 右滑 | 新页面从左滑入，旧页面向右滑出 |

Splash 页面特殊处理：进入无动画或仅淡入，避免启动时有突兀的滑动效果。

---

## 7. AppNavigator.openNotificationTarget — 通知深链接

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/AppNavigator.kt`

### 7.1 openNotificationTarget

通知深链接已收敛到 `AppNavigator` 体系，不再保留独立的 `NotificationNavigation` 层。

```kotlin
fun openNotificationTarget(route: String) {
    resetToRoute(route)
}
```

`NavHostControllerAppNavigator.resetToRoute(route)` 负责清空当前回退栈，并重建 `Home -> target`：

- `home`：落到 Home。
- `home_file_manager` / `home_toolbox`：直接落到对应 Home 子路由。
- 非 Home 目标：先落到 `home`，再打开目标 route。

### 7.2 通知入口广告策略

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/FeatureEntryRouter.kt`

通知入口的插屏广告策略由 `FeatureEntryRouter.openNotificationTarget(route)` 承担：

```kotlin
override fun openNotificationTarget(route: String) {
    if (route in AppDestination.homeRoutes) {
        navigator.openNotificationTarget(route)
        return
    }
    interstitialAds.runRouteEntry(fromRoute, route) {
        navigator.openNotificationTarget(route)
    }
}
```

Home 系通知目标不走插屏广告；非 Home 通知目标先经过 `runRouteEntry`，再执行底层栈重建。

### 7.3 通知路由规范化

从系统通知收到的路由字符串可能包含大小写不一致或冗余斜杠，通过 `AppDestination.normalizeNotificationRoute()` 进行规范化处理后再查找目标。

---

## 8. FeatureCatalog — 功能目录

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/feature/FeatureCatalog.kt`

### 8.1 FeatureSpec

```kotlin
data class FeatureSpec(
    val feature: FeatureKey,
    val route: String,
    val group: FeatureGroup
)
```

`FeatureCatalog` 维护所有功能的元数据注册表，提供按 `FeatureKey`、路由字符串、功能分组进行查找的能力。

### 8.2 用途

- Home 页面根据 `FeatureGroup` 渲染不同分组的功能卡片
- 功能入口点击时，通过 `FeatureKey` 查找对应的 `AppDestination` 进行导航
- 与 `FeatureEntryRouter` 配合，确保入口点的广告门控逻辑只对 `featureEntry = true` 的目的地生效

---

## 9. 子路由 ViewModel 共享

### 9.1 AntiVirus 子路由

AntiVirus 功能包含 4 个子路由（QuickScan、DeepScan、VirusResult、NoVirusResult），它们与入口页 `AntiVirus` 共享同一个 `VirusScanViewModel`。

```kotlin
fun NavController.antiVirusViewModelOwnerOr(
    backStackEntry: NavBackStackEntry
): ViewModelStoreOwner {
    // 查找父级 AntiVirus 的 NavBackStackEntry
    val parentEntry = backStackEntry.parentBackStackEntry ?: backStackEntry
    return if (parentEntry.destination.route == AppDestination.AntiVirus.route) {
        parentEntry
    } else {
        backStackEntry
    }
}
```

这确保了扫描状态（进度、结果等）在所有子页面间保持一致。

### 9.2 FileManager 详情路由

文件管理器包含 7 个详情路由（PhotosDetail、VideosDetail 等），它们通过 `{initialIndex}` 路径参数支持直接跳转到指定项。

```kotlin
fun NavController.fileManagerViewModelOwnerOr(
    parentDestination: AppDestination,
    fallback: () -> ViewModelStoreOwner
): ViewModelStoreOwner {
    // 查找对应父级管理器的 NavBackStackEntry
    val parentRoute = parentDestination.route
    backQueue.find { it.destination.route == parentRoute }
        ?.let { return it }
    return fallback()
}
```

详情页路由示例：

```
photos_detail/{index}       → PhotosDetail(initialIndex: Int)
videos_detail/{index}       → VideosDetail(initialIndex: Int)
large_files_detail/{index}  → LargeFilesDetail(initialIndex: Int)
```

---

## 10. 完整路由注册表

| Route | AppDestination | FeatureKey | FeatureGroup |
|-------|---------------|------------|--------------|
| `splash` | Splash | — | — |
| `onboarding_scan` | OnboardingScan | — | — |
| `home` | Home | — | HOME |
| `home_file_manager` | Home (FileManager tab) | — | HOME |
| `home_toolbox` | Home (Toolbox tab) | — | HOME |
| `scan` | JunkClean | JUNK_CLEAN | HOME |
| `anti_virus` | AntiVirus | ANTI_VIRUS | HOME |
| `virus_quick_scan` | VirusQuickScan | ANTI_VIRUS | HOME |
| `virus_deep_scan` | VirusDeepScan | ANTI_VIRUS | HOME |
| `virus_result` | VirusResult | ANTI_VIRUS | HOME |
| `no_virus_result` | NoVirusResult | ANTI_VIRUS | HOME |
| `app_lock` | AppLock | APP_LOCK | HOME |
| `device_info` | DeviceInfo | DEVICE_INFO | TOOLBOX |
| `battery_info` | BatteryInfo | BATTERY_INFO | TOOLBOX |
| `app_usage` | AppUsage | APP_USAGE | TOOLBOX |
| `network_usage` | NetworkUsage | NETWORK_USAGE | TOOLBOX |
| `network_scan` | NetworkScan | NETWORK_SCAN | TOOLBOX |
| `network_scan_devices` | NetworkScanDevices | NETWORK_SCAN | TOOLBOX |
| `network_speed` | NetworkSpeed | NETWORK_SPEED | TOOLBOX |
| `whatsapp_cleaner` | WhatsAppCleaner | WHATSAPP_CLEANER | TOOLBOX |
| `notification_cleaner` | NotificationCleaner | NOTIFICATION_CLEANER | TOOLBOX |
| `manage_photos` | PhotosManager | PHOTOS | FILES |
| `similar_photos` | SimilarPhotosManager | SIMILAR_PHOTOS | FILES |
| `photo_privacy` | PhotoPrivacyManager | PHOTO_PRIVACY | FILES |
| `screenshots` | ScreenshotsManager | SCREENSHOTS | FILES |
| `manage_videos` | VideosManager | VIDEOS | FILES |
| `manage_audios` | AudiosManager | AUDIOS | FILES |
| `large_files` | LargeFilesManager | LARGE_FILES | FILES |
| `duplicate_files` | DuplicateFilesManager | DUPLICATE_FILES | FILES |
| `manage_documents` | DocumentsManager | DOCUMENTS | FILES |
| `photos_detail/{index}` | PhotosDetail | PHOTOS | FILES |
| `screenshots_detail/{index}` | ScreenshotsDetail | SCREENSHOTS | FILES |
| `videos_detail/{index}` | VideosDetail | VIDEOS | FILES |
| `audios_detail/{index}` | AudiosDetail | AUDIOS | FILES |
| `large_files_detail/{index}` | LargeFilesDetail | LARGE_FILES | FILES |
| `documents_detail/{index}` | DocumentsDetail | DOCUMENTS | FILES |
| `settings` | Settings | — | — |
| `manage_permissions` | ManagePermissions | — | — |

> **共计 38 个目的地**，其中 22 个为功能入口点（`featureEntry = true`），7 个为详情子路由（支持 `{index}` 路径参数）。

---

## 11. 架构总览

```
┌─────────────────────────────────────────────────────────┐
│                    AppNavGraph                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │            FeatureEntryRouter                      │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │         NavHostControllerAppNavigator        │  │  │
│  │  │  ┌───────────────────────────────────────┐  │  │  │
│  │  │  │      Jetpack Navigation Compose        │  │  │  │
│  │  │  │  (NavHostController + NavHost)         │  │  │  │
│  │  │  └───────────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │              AppDestination (Sealed Class)        │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │   │
│  │  │  38 个    │  │ byRoute  │  │ byFeature    │   │   │
│  │  │ 目的地    │  │ 索引     │  │ 索引          │   │   │
│  │  └──────────┘  └──────────┘  └──────────────┘   │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  AppRouteTransitions  │  openNotificationTarget   │   │
│  │  (280ms 滑动+淡入淡出) │  (通知 → 深链接 → 页面)    │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  FeatureCatalog  │  ViewModel 共享               │   │
│  │  (功能元数据)     │  (AntiVirus / FileManager)    │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```
