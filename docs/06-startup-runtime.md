# Quick Clean Pro 启动与运行时

## 1. 完整冷启动流程

Quick Clean Pro 的冷启动遵循严格的 12 步流程，从系统创建进程到应用完全可交互：

| 步骤 | 阶段 | 描述 |
|------|------|------|
| 1 | 系统 | 系统创建进程 |
| 2 | Application | `MyApp.onCreate()` |
| 3 | Activity | `MainActivity.onCreate()` |
| 4 | Compose | `AppRoot()` 组合 |
| 5 | ViewModel | `SplashViewModel` 创建 |
| 6 | UI | `SplashScreen` 渲染（动画开始） |
| 7 | SDK | Advertise 初始化完成 |
| 8 | 协调 | `advancePreparing()` 触发 |
| 9 | 广告 | 冷启动广告完成 |
| 10 | UI | 视觉动画完成 |
| 11 | 导航 | 导航到目标页面 |
| 12 | 完成 | App 完全可交互 |

---

## 2. MyApp — Application

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/MyApp.kt`

`MyApp` 是应用的入口 Application 类，负责全局初始化和依赖注入。

### onCreate() 执行序列

1. **存储自身引用** — 将 `this` 保存到 companion object 的单例中，供全局访问
2. **启动 Koin DI** — 注册 `dataModule` 和 `presentationModule`，构建依赖注入容器
3. **初始化 AnalyticsTracker** — 实例化分析追踪器
4. **构建 SdkInitializationCoordinator** — 注册 3 个初始化器：
   - `ADVERTISE`: `AdvertiseSdkAdapter.initialize()`（在 Main 线程上运行）
   - `ANALYTICS`: `AppAnalyticsLifecycleObserver`（前台/后台生命周期监听）
   - `NOTIFICATION_DEFAULTS`: 通知内容默认值设置
5. **调用 `start()`** — 启动 SDK 初始化编排

```kotlin
// MyApp.kt 伪代码结构
class MyApp : Application() {
    companion object {
        lateinit var instance: MyApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        startKoin { modules(dataModule, presentationModule) }
        AnalyticsTracker.init()
        SdkInitializationCoordinator(
            advertise = AdvertiseSdkAdapter.initialize(),
            analytics = AppAnalyticsLifecycleObserver(),
            notification = NotificationDefaults()
        ).start()
    }
}
```

---

## 3. MainActivity — 唯一 Activity

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/MainActivity.kt`

Quick Clean Pro 采用 **单 Activity 架构**，所有页面通过 Compose Navigation 在单个 Activity 内切换。

### 生命周期回调

| 方法 | 职责 |
|------|------|
| `onCreate()` | 设置 edge-to-edge 显示 → 处理启动 Intent → 初始化启动协调器 → `setContent(AppRoot)` |
| `onNewIntent()` | 将新 Intent 转发给 `launchCoordinator.onNewIntent()` |
| `onResume()` | 调用 `ensureSdkPersistentNotificationRunning()` 确保常驻通知运行 |

```kotlin
// MainActivity.kt 伪代码结构
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLaunchIntent(intent)
        launchCoordinator.onCreate()
        setContent { AppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchCoordinator.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        ensureSdkPersistentNotificationRunning()
    }
}
```

---

## 4. AppRoot — Compose 根节点

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/AppRoot.kt`

`AppRoot` 是 Compose 树的根组合函数，创建所有全局运行时组件并连接导航图。

### 创建与连接顺序

```
1. NavController          ── 导航控制器
2. AppSessionCoordinator  ── 忙状态管理（Permission / FeatureOperation / Interstitial）
3. AppRuntimeBindings     ── AdRuntime + FeatureFlowRuntime + ExternalActivityLauncher
4. 副作用                 ── AdRuntimeLifecycleEffect, RouteAnalyticsEffect,
                              NotificationPermissionHost, NotificationLaunchEffect
5. 覆盖层                 ── InterstitialInteractionBlocker（广告时阻断触摸和返回键）
6. AppPermissionHost      ── 包裹 NavGraph 的权限宿主
7. AppNavGraph            ── startDestination = Splash
```

### 组件层次结构

```
AppRoot
├── AdRuntimeLifecycleEffect        // 生命周期 → 广告运行时
├── RouteAnalyticsEffect            // 路由变化 → 分析追踪
├── NotificationPermissionHost      // 通知权限请求
├── NotificationLaunchEffect        // 通知点击 → 导航
├── InterstitialInteractionBlocker  // 广告期间阻断交互
├── AppPermissionHost               // 权限请求包裹器
│   └── AppNavGraph
│       └── Splash → Home → FeaturePages...
```

---

## 5. SdkInitializationCoordinator — 并发编排

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/SdkInitializationCoordinator.kt`

负责并发协调 3 个 SDK 初始化器的执行，具有失败隔离和超时保护。

### 启动模型

- `start()` 是**幂等**的，由原子布尔标志保护
- 启动 3 个协程：

| 协程 | 启动条件 | 超时 | 运行 |
|------|----------|------|------|
| `ADVERTISE` | 立即运行 | — | Main 线程初始化广告 SDK |
| `ANALYTICS` | 等待 ADVERTISE 完成 | 6.5s | 注册生命周期观察者 |
| `NOTIFICATION_DEFAULTS` | 等待 ADVERTISE 完成 | 6.5s | 设置通知默认值 |

### 失败隔离

每个初始化器独立失败，不会阻塞其他初始化器的执行。

### 状态追踪

核心类型：

- `AppSdkInitializationState` — 包含 `advertise`、`analytics`、`notification` 三个子状态
- `ComponentInitializationState` — 枚举：`NOT_STARTED` / `RUNNING` / `SUCCEEDED` / `FAILED`
- 通过 `StateFlow` 发布状态变化

### 就绪屏障

```kotlin
suspend fun awaitAdvertiseReady(timeoutMillis: Long = 6500): Boolean
```

`SplashViewModel` 使用此方法轮询等待广告 SDK 就绪，最长等待 6.5 秒后超时。

---

## 6. AppSessionCoordinator — 忙状态管理

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/AppSessionCoordinator.kt`

使用 `mutableStateMapOf` 追踪当前会话中的"忙"原因，对 Compose 可观察，自动触发 recomposition。

### 忙状态类型

| 原因 | 描述 |
|------|------|
| `RuntimeBusyReason.Permission` | 权限弹窗正在显示 |
| `RuntimeBusyReason.FeatureOperation` | 功能操作（如扫描、清理）进行中 |
| `RuntimeBusyReason.Interstitial` | 插屏广告正在播放 |

### 使用场景

- 广告运行时在显示广告前检查是否有其他忙状态
- 忙状态阻止多个广告同时播放
- 忙状态阻止在功能操作期间弹出广告

---

## 7. AppRuntimeBindings

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/AppRuntimeBindings.kt`

在 `AppRoot` 中创建 3 个全局运行时单例，绑定到 Compose 生命周期：

### 1. AdRuntime

- `activityProvider` — 提供当前 Activity 引用
- `stateProvider` — 查询忙状态（连接 `AppSessionCoordinator`）
- `onInterstitialChanged` — 插屏广告状态变化回调

### 2. ExternalActivityLauncher

- 管理外部浏览器/应用商店跳转
- 跳转前调用 `AdRuntime.markLaunch()` 标记外部启动
- 返回时调用 `AdRuntime.cancelLaunch()` 清除标记

### 3. DefaultFeatureFlowRuntime

- 组合 `AdRuntime` 和 `AppSessionCoordinator`
- 为功能页面提供统一的运行时接口

---

## 8. AppRuntimeEffects

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/AppRuntimeEffects.kt`

3 个 Compose 副作用，通过 `LaunchedEffect` 连接生命周期与运行时：

### AdRuntimeLifecycleEffect

```kotlin
// ON_RESUME → onHostResumed()
// dispose (离开组合) → dispose()
```

管理广告运行时的生命周期绑定。

### RouteAnalyticsEffect

```kotlin
// 路由变化事件：
// - trackHomeEntered()      — 进入首页
// - trackCoreFeatureEntered() — 进入核心功能
// - preload(MainPage)        — 预加载主页面广告
```

监听导航路由变化，上报分析事件和触发广告预加载。

### NotificationLaunchEffect

```kotlin
// 如果存在 pending notification → 导航到 Splash
```

处理通知点击启动场景，将待处理的通知目标转发给启动流程。

---

## 9. AppGlobalOverlays

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/AppGlobalOverlays.kt`

### InterstitialInteractionBlocker

当插屏广告活跃时，渲染一个全屏透明的 `Box`，拦截所有触摸事件和返回键。

```kotlin
if (isAdActive) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* 消费点击 */ }
    )
}
```

这确保用户在广告展示期间无法与应用交互。

---

## 10. Splash 状态机

### 10.1 SplashContract（类型定义）

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/feature/startup/SplashContract.kt`

#### SplashStage（4 个阶段）

| 阶段 | 含义 |
|------|------|
| `Preparing` | 等待 SDK 就绪和视觉动画完成 |
| `WaitingForOpenAd` | SDK 已就绪，等待冷启动广告展示/完成 |
| `Finishing` | 广告已完成，视觉收尾中 |
| `Completed` | 启动完成，准备导航 |

#### SplashPauseReason（3 个暂停原因）

| 原因 | 触发条件 |
|------|----------|
| `Permission` | 通知权限弹窗显示中 |
| `OpenAd` | 冷启动广告正在显示 |
| `ExternalLink` | 用户点击打开外部链接（浏览器/商店） |

---

### 10.2 SplashStateMachine（Reducer）

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/feature/startup/SplashStateMachine.kt`

纯函数 `reduceSplashState(state, action, normalDestination)` 实现状态转换逻辑：

#### 关键转换规则

```
LaunchRequestChanged (NotificationTarget from NewIntent)
  → 立即 OpenNotificationTarget
  → Completed （通知快路径）

SdkBarrierFinished + VisualReady
  → advancePreparing()
    ├── Normal          → WaitingForOpenAd + RunColdStartAd
    └── NotificationTarget → Finishing （跳过广告）

OpenAdFinished (WaitingForOpenAd)
  → Finishing

VisualFinished (Finishing)
  → Navigate(destination)
  → Completed
```

#### 暂停动作

- **添加暂停原因**: 将 `pauseReason` 加入集合，`stage` 不变
- **移除暂停原因**: 从集合中移除，`stage` 不变
- 暂停期间进度条冻结但状态机不改变阶段

---

### 10.3 SplashViewModel

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/feature/startup/SplashViewModel.kt`

#### 初始化

```kotlin
init {
    viewModelScope.launch {
        sdkInitialization.awaitAdvertiseReady()  // 轮询等待，最长 6.5s
        dispatch(SdkBarrierFinished)             // 通知状态机
    }
}
```

#### 核心方法

```
onAction(action)       // 管穿状态机 → 更新 uiState → emit effects
startupDestination     // HasCompletedOnboarding ? Home : OnboardingScan
```

#### 数据流

```
用户操作 → onAction(action)
  → reduceSplashState(state, action, destination)  // 纯函数
  → 新的 uiState (StateFlow)
  → 新的 effects (SharedFlow)
  → UI 层收集并执行
```

---

### 10.4 SplashRoutes（导航注册）

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/navigation/SplashRoutes.kt`

包含 5 个 `LaunchedEffect`，每个负责一个横切关注点：

| # | LaunchedEffect | 职责 |
|---|----------------|------|
| 1 | 观察 `pendingRequest` | 消费待处理启动请求 → dispatch 到 ViewModel |
| 2 | 观察 `splashPermissionPaused` | 权限弹窗暂停状态 → 转发 ViewModel |
| 3 | 收集 effects | 处理 3 种 effect：`RunColdStartAd` / `OpenNotificationTarget` / `Navigate` |
| 4 | 追踪 `splash_display` | 记录启动显示分析事件 |
| 5 | 管理 external link | 外部链接打开/返回的状态同步 |

#### Effect 处理

```kotlin
when (effect) {
    is RunColdStartAd   → adRuntime.runColdStart(...)
    is OpenNotificationTarget → navigator.openNotificationTarget(...)
    is Navigate         → navigator.replace(destination)
}
```

---

### 10.5 SplashScreen（UI）

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/feature/startup/ui/SplashScreen.kt`

#### 动画时间线（3 个串行协程）

```
┌─ 入场动画 ─────────────────────────────────────────────┐
│ Logo + App Name                                         │
│ • scale: 800ms                                          │
│ • alpha: 600ms                                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─ 进度阶段 ──────────────────────────────────────────────┐
│ Phase 1: 0% → 72%    (2800ms)     → dispatch VisualReady│
│ Phase 2: 72% → 94%   (max 6500ms) 等待广告完成          │
│ Phase 3: 94% → 100%  (650ms)      finishing             │
│ hold: 300ms                       → dispatch VisualFinished│
└─────────────────────────────────────────────────────────┘
```

#### 暂停感知

当 `state.paused == true` 时：
- 协程体跳过（不推进动画）
- 进度条冻结在当前值
- 恢复时 `remainingDurationMillis` 按比例计算

#### UI 元素

| 元素 | 描述 |
|------|------|
| 全屏背景图 | 底层品牌背景 |
| 渐变色叠加 | 半透明渐变遮罩 |
| 圆形 Logo | 应用图标圆形裁剪 |
| App 名称 | 品牌文字 |
| RoundedProgressBar | 圆角进度条，带阶段动画 |
| 条款/隐私链接 | 底部用户协议与隐私政策入口 |

---

## 11. 关键设计决策

### 11.1 SDK 初始化与 Splash 隔离

`SdkInitializationCoordinator` 在 `Dispatchers.Default` 上运行协程，与主线程的 Splash UI 完全解耦。`SplashViewModel` 通过 `awaitAdvertiseReady()` 轮询等待，超时设为 6.5 秒，确保即使广告 SDK 初始化失败，应用也不会无限等待。

### 11.2 双重屏障（SDK + Visual）

`Preparing` 阶段需要**同时满足**两个条件才会推进：
- **SDK 屏障**: `awaitAdvertiseReady()` 返回（广告 SDK 就绪或超时）
- **视觉屏障**: 进度条 Phase 1 完成，`VisualReady` 已 dispatch

这种设计确保启动画面在内容和视觉上都有完整的过渡。

### 11.3 暂停感知进度条

进度条在暂停期间冻结但**不重置**，恢复时根据 `remainingDurationMillis` 按比例继续。这确保了：
- 权限弹窗不消耗进度时间
- 广告播放期间进度保持同步
- 外部链接打开后恢复时体验连贯

### 11.4 通知快路径

当启动来源为 `NewIntent`（通知点击）时：
1. 状态机立即转换到 `Completed`
2. 直接发出 `OpenNotificationTarget` effect
3. **完全跳过广告和动画**，直达目标页面

这确保从通知进入应用时用户体验即时响应。

### 11.5 通知启动免广告

当 `LaunchRequestChanged` 的类型为 `NotificationTarget` 时：
- `advancePreparing()` 从 `Preparing` 直接跳到 `Finishing`
- **跳过 `WaitingForOpenAd` 阶段**
- 不触发 `RunColdStartAd` effect

设计理由：用户通过通知主动进入应用，不应被冷启动广告打断。

### 11.6 外部链接集成

外部链接场景的完整流程：

```
Splash Screen
  → 用户点击条款/隐私链接
  → ExternalActivityLauncher 打开浏览器
  → AdRuntime.markLaunch() 标记外部启动
  → splash 检测到 ExternalLink pauseReason → 暂停进度
  → 用户从浏览器返回
  → onResume → AdRuntime.cancelLaunch()
  → 移除 ExternalLink pauseReason → 恢复进度
```

---

## 12. 架构总览图

```
┌─────────────────────────────────────────────────────────┐
│                      MyApp.onCreate()                    │
│  Koin DI → AnalyticsTracker → SdkInitializationCoordinator│
└──────────────────────────┬──────────────────────────────┘
                           │ start()
                           ▼
┌─────────────────────────────────────────────────────────┐
│            SdkInitializationCoordinator                   │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────┐     │
│  │ADVERTISE │  │ANALYTICS │  │NOTIFICATION_DEFAULTS│     │
│  │(immediate)│←│(after adv)│←│  (after adv)       │     │
│  └──────────┘  └──────────┘  └────────────────────┘     │
│                     │ StateFlow                          │
└─────────────────────┼───────────────────────────────────┘
                      │ awaitAdvertiseReady()
                      ▼
┌─────────────────────────────────────────────────────────┐
│                 MainActivity.onCreate()                   │
│              setContent { AppRoot() }                     │
└──────────────────────────┬──────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                 ▼
   AppSessionCoordinator  AppRuntimeBindings  AppRuntimeEffects
          │                │                  │
          ▼                ▼                  ▼
   ┌──────────────────────────────────────────────────────┐
   │                   SplashScreen                        │
   │  ┌─────────────────────────────────────────────────┐ │
   │  │          SplashViewModel                         │ │
   │  │  init: awaitAdvertiseReady() → SdkBarrierFinished│ │
   │  │  onAction() → SplashStateMachine → effects       │ │
   │  └─────────────────────────────────────────────────┘ │
   │  ┌─────────────────────────────────────────────────┐ │
   │  │          动画时间线                               │ │
   │  │  Phase1(0→72%) → Phase2(72→94%) → Phase3(94→100%)│ │
   │  │  2800ms           max 6500ms        650ms        │ │
   │  └─────────────────────────────────────────────────┘ │
   └──────────────────────┬───────────────────────────────┘
                          │ Navigate(destination)
                          ▼
                   Home / OnboardingScan
```
