# Quick Clean Pro 功能模块详解

## 1. 功能模板系统

项目使用模板系统来标准化新功能的开发流程。所有模板位于 `templates/feature/` 目录下。

| 模板文件 | 用途 |
|----------|------|
| `FeatureContract.kt.template` | 定义 UiState、Phase（Idle / Loading / Content / Error）、Action、Effect |
| `FeatureReducer.kt.template` | 纯函数 `reduce(state, action) → state` |
| `FeatureViewModel.kt.template` | `MutableStateFlow` + `Channel` + 业务逻辑 |
| `FeatureScreen.kt.template` | 无状态 Composable，接收 `state` 和 `onAction` |
| `FeatureRoute.kt.template` | ViewModel 注入 + 效果收集 + Screen 渲染 |
| `FeatureRepository.kt.template` | 领域层接口定义 |
| `FeatureReducerTest.kt.template` | 基础单元测试模板 |
| `README.md` | 使用说明与验收清单 |

### Phase 状态机

```kotlin
enum class Phase {
    Idle,      // 初始状态
    Loading,   // 数据加载中
    Content,   // 内容已就绪
    Error      // 错误状态
}
```

### MVI 架构映射

```
┌──────────┐     Action     ┌──────────┐     State     ┌──────────┐
│  Screen  │ ──────────────> │ ViewModel│ ─────────────>│  Screen  │
│ (Composable) │              │  + Flow  │              │ (重组)   │
└──────────┘                └────┬─────┘              └──────────┘
      ▲                          │
      │       Effect             │
      └──────────────────────────┘
            (一次性事件)
```

---

## 2. FeatureKey 与 FeatureGroup

### FeatureKey 枚举（21 个功能）

```kotlin
enum class FeatureKey {
    // HOME 组
    JUNK_CLEAN,
    ANTI_VIRUS,
    APP_LOCK,

    // TOOLBOX 组
    DEVICE_INFO,
    BATTERY_INFO,
    APP_USAGE,
    NOTIFICATION_CLEANER,
    WHATSAPP_CLEANER,
    NETWORK_USAGE,
    NETWORK_SCAN,
    NETWORK_SPEED,

    // FILES 组
    PHOTOS,
    SIMILAR_PHOTOS,
    PHOTO_PRIVACY,
    SCREENSHOTS,
    VIDEOS,
    AUDIOS,
    LARGE_FILES,
    DUPLICATE_FILES,
    DOCUMENTS
}
```

### FeatureGroup 分组（3 组）

```kotlin
enum class FeatureGroup(val features: Set<FeatureKey>) {
    HOME(setOf(
        FeatureKey.JUNK_CLEAN,
        FeatureKey.ANTI_VIRUS,
        FeatureKey.APP_LOCK
    )),
    TOOLBOX(setOf(
        FeatureKey.DEVICE_INFO,
        FeatureKey.BATTERY_INFO,
        FeatureKey.APP_USAGE,
        FeatureKey.NOTIFICATION_CLEANER,
        FeatureKey.WHATSAPP_CLEANER,
        FeatureKey.NETWORK_USAGE,
        FeatureKey.NETWORK_SCAN,
        FeatureKey.NETWORK_SPEED
    )),
    FILES(setOf(
        FeatureKey.PHOTOS,
        FeatureKey.SIMILAR_PHOTOS,
        FeatureKey.PHOTO_PRIVACY,
        FeatureKey.SCREENSHOTS,
        FeatureKey.VIDEOS,
        FeatureKey.AUDIOS,
        FeatureKey.LARGE_FILES,
        FeatureKey.DUPLICATE_FILES,
        FeatureKey.DOCUMENTS
    ))
}
```

### 功能分组概览

```
HOME (3)                    TOOLBOX (8)                  FILES (9)
├─ JUNK_CLEAN               ├─ DEVICE_INFO               ├─ PHOTOS
├─ ANTI_VIRUS               ├─ BATTERY_INFO              ├─ SIMILAR_PHOTOS
└─ APP_LOCK                 ├─ APP_USAGE                 ├─ PHOTO_PRIVACY
                            ├─ NOTIFICATION_CLEANER      ├─ SCREENSHOTS
                            ├─ WHATSAPP_CLEANER          ├─ VIDEOS
                            ├─ NETWORK_USAGE             ├─ AUDIOS
                            ├─ NETWORK_SCAN              ├─ LARGE_FILES
                            └─ NETWORK_SPEED             ├─ DUPLICATE_FILES
                                                         └─ DOCUMENTS
```

---

## 3. HOME 组功能

### 3.1 JunkClean（垃圾清理）— 最完整的 MVI 实现

**目录**: `feature/junkclean/`

垃圾清理是项目中 MVI 架构最完整的实现，涵盖扫描、分类展示、清理和多阶段状态流转。

#### 核心组件

| 文件 | 职责 |
|------|------|
| `JunkCleanContract` | 定义 `UiState`、`Phase`、`Action`、`UiAction`、`Effect` |
| `JunkCleanReducer` | 复杂多阶段 reducer（未开始 → 数据加载 → 垃圾分类 → 清理 → 完成） |
| `JunkCleanViewModel` | 扫描/清理生命周期管理，授权流程，`FeatureOperationEvent` 追踪 |
| `JunkCleanRoute` | 权限处理 + `FeatureFlowRuntime` + 导航 |

#### 扫描器子系统（`scanner/` 子包）

| 扫描器 | 职责 |
|--------|------|
| `CacheScanner` | 扫描应用缓存文件 |
| `AdJunkScanner` | 扫描广告产生的垃圾文件 |
| `ApkScanner` | 扫描残留 APK 安装包 |
| `ResidualScanner` | 扫描卸载应用的残留文件 |
| `TempFileScanner` | 扫描临时文件 |
| `DuplicateFileScanner` | 扫描重复文件 |
| `MemoryCleaner` | 内存清理（非文件扫描） |

#### 数据层

| 组件 | 职责 |
|------|------|
| `CleanRepository` | 清理数据接口 |
| `CleanRepositoryImpl` | 清理数据实现 |
| `CleanSessionStore` | 清理会话状态持久化 |
| `SharedScanState` | 跨进程扫描状态共享 |

#### 多阶段状态流转

```kotlin
// JunkCleanReducer 中的阶段管理
sealed class Phase {
    object Idle : Phase()                         // 未开始
    object Loading : Phase()                      // 数据加载中
    data class Categories(val items: List<JunkCategory>) : Phase()  // 垃圾分类展示
    object Cleaning : Phase()                     // 清理进行中
    data class Done(val stats: CleanStats) : Phase()  // 清理完成
}
```

#### UiState 结构

```kotlin
data class UiState(
    val phase: Phase = Phase.Idle,
    val selectedCategories: Set<JunkCategory> = emptySet(),
    val totalJunkSize: Long = 0L,
    val cleanedSize: Long = 0L,
    val progress: Float = 0f,
    val error: String? = null
)
```

#### Action / Effect

```kotlin
sealed interface Action {
    data class ToggleCategory(val category: JunkCategory) : Action
    object StartScan : Action
    object StartClean : Action
    object StopClean : Action
}

sealed interface Effect {
    data class ShowSnackbar(val message: String) : Effect
    data class NavigateToResult(val stats: CleanStats) : Effect
    object RequestPermission : Effect
}
```

---

### 3.2 AntiVirus（病毒扫描）

**目录**: `feature/antivirus/`

#### 核心组件

| 文件 | 职责 |
|------|------|
| `VirusHomeContract` | 定义 `UiState`、`Action`、`Effect` |
| `VirusScanViewModel` | 共享给子路由的扫描 ViewModel |
| `VirusScanEngine` | 扫描引擎接口 |
| `TrustlookVirusScanEngine` | Trustlook 引擎实现 |
| `VirusSecurityRepository` | 安全数据接口 |
| `VirusSecurityRepositoryImpl` | 安全数据实现 |
| `PackageRemovedReceiver` | 监听应用卸载事件 |

#### 双模式扫描

```kotlin
enum class ScanMode {
    QuickScan,  // 快速扫描：仅检查已安装 APK 的签名/哈希
    DeepScan    // 深度扫描：完整文件系统遍历
}
```

#### UI 页面

| Composable | 职责 |
|------------|------|
| `AntiVirusHome` | 主页，选择扫描模式 |
| `VirusScanning` | 扫描进度页，实时动画反馈 |
| `VirusThreatResult` | 发现威胁时的结果展示 |
| `NoVirusResult` | 无威胁时的安全结果展示 |
| `InstalledAppsAccessDialog` | 请求应用列表权限对话框 |

---

### 3.3 AppLock（应用锁）

**目录**: `feature/applock/`

#### 核心组件

| 文件 | 职责 |
|------|------|
| `AppLockViewModel` | 应用锁主 ViewModel |
| `AppLockRepository` | 锁定数据接口 |
| `AppLockRepositoryImpl` | 锁定数据实现 |
| `AppLockManager` | 锁定/解锁核心逻辑 |
| `AppLockMonitoringController` | 应用前后台监控控制 |
| `AppLockMonitoringControllerImpl` | 监控实现 |
| `AppLockServiceCoordinator` | 前后台服务协调器 |

#### 系统服务

| 服务 | 职责 |
|------|------|
| `LockScreenOverlayService` | 锁屏悬浮窗覆盖层，拦截被锁应用 |
| `AppLockMonitoringService` | 后台服务，持续监控前台应用切换 |

#### UI 页面

| Composable | 职责 |
|------------|------|
| PIN 设置页 | 创建/修改 PIN 码 |
| 应用选择页 | 勾选需要锁定的应用 |
| 应用管理页 | 已锁定应用列表管理 |
| 应用搜索页 | 搜索并快速添加锁定应用 |

---

## 4. FILES 组功能（9 个）

### 4.1 共享基础设施

**目录**: `feature/files/shared/`

所有文件管理类功能共享一套基础设施，减少重复代码。

| 组件 | 职责 |
|------|------|
| `BaseFileManagerViewModel` | 抽象基类，提供扫描/删除/追踪的公共逻辑 |
| `FileOperationExecutor` | 协程执行器：扫描 → 删除 → 追踪固定流水线 |
| `FileManagerRouteSupport` | 权限处理、完成广告、停止弹窗、删除确认等通用 UI 逻辑 |

### 4.2 FileOperationExecutor 流水线

```kotlin
class FileOperationExecutor(
    private val repository: FileRepository,
    private val flowRuntime: FeatureFlowRuntime
) {
    suspend fun execute(
        fileType: FileType,
        featureKey: FeatureKey
    ): Flow<OperationState> {
        // 1. 扫描阶段
        emit(OperationState.Scanning)
        flowRuntime.onScanStarted(featureKey)
        val files = repository.scanFiles(fileType)
        flowRuntime.onScanFinished(featureKey, files.isNotEmpty())

        // 2. 删除确认阶段
        emit(OperationState.ReadyToDelete(files))

        // 3. 删除执行阶段
        emit(OperationState.Deleting)
        val result = repository.deleteFiles(files)
        flowRuntime.onOperationFinished(featureKey, OperationAction.DELETE, result.isSuccess)

        // 4. 追踪
        emit(OperationState.Done(result))
    }
}
```

### 4.3 FILES 组功能清单

每个文件管理功能遵循统一模式：

```
*ManagerRoute → *ManagerViewModel → *ManagerScreen
     ↓ (部分功能)
   *Detail + *FileDetailRoute
```

| 功能 | FeatureKey | 主要职责 |
|------|------------|----------|
| Photos | `PHOTOS` | 扫描并管理照片类文件 |
| SimilarPhotos | `SIMILAR_PHOTOS` | 检测并清理相似/重复照片 |
| PhotoPrivacy | `PHOTO_PRIVACY` | 照片隐私保护（隐藏/加密） |
| Screenshots | `SCREENSHOTS` | 扫描并清理截图文件 |
| Videos | `VIDEOS` | 扫描并管理视频文件 |
| Audios | `AUDIOS` | 扫描并管理音频文件 |
| LargeFiles | `LARGE_FILES` | 检测和清理大文件 |
| DuplicateFiles | `DUPLICATE_FILES` | 检测和清理重复文件 |
| Documents | `DOCUMENTS` | 扫描并管理文档文件 |

#### 详情页模式

部分文件管理功能包含详情页，通过 `NavBackStackEntry` 查找父级 ViewModel：

```kotlin
// 示例：PhotoDetailRoute
@Composable
fun PhotoFileDetailRoute(
    navBackStackEntry: NavBackStackEntry,
    parentViewModel: PhotoManagerViewModel = navBackStackEntry.parentEntry
        ?.let { hiltViewModel(it) } ?: return
) {
    val state by parentViewModel.state.collectAsStateWithLifecycle()
    PhotoDetail(
        state = state.selectedPhoto,
        onAction = parentViewModel::onAction
    )
}
```

---

## 5. TOOLBOX 组功能（8 个）

TOOLBOX 组主要提供设备和系统信息展示类功能，架构上普遍采用 ViewModel + Repository 模式，部分功能也采用了 MVI 的 Phase/UiState/Action 模式。

### 功能清单

#### 5.1 DeviceInfo（设备信息）

| 组件 | 职责 |
|------|------|
| `DeviceInfoViewModel` | 设备信息状态管理 |
| `DeviceInfoRepository` | 设备数据接口 |
| `StorageDataSource` | 存储空间数据源 |
| `CpuTemperatureResolver` | CPU 温度解析 |

展示内容：CPU 型号/核心数/温度、GPU 型号、RAM 总量/可用、存储空间使用情况、屏幕分辨率、Android 版本、传感器等。

#### 5.2 BatteryInfo（电池信息）

| 组件 | 职责 |
|------|------|
| `BatteryInfoViewModel` | 电池状态管理 |
| `BatteryHistoryRepository` | 电池历史数据接口 |
| `BatteryHistorySampler` | 电池数据采样器 |
| `BatterySamplingCoordinator` | 采样调度协调器 |

展示内容：当前电量、电池温度、电池健康状态、充电状态、历史使用曲线。

#### 5.3 AppUsage（应用使用统计）

| 组件 | 职责 |
|------|------|
| `AppUsageViewModel` | 应用使用统计状态管理 |
| `AppUsageRepository` | 使用统计数据接口 |
| `AppUsageDataSource` | 系统 `UsageStatsManager` 数据源 |

展示内容：应用使用时长排名、前台唤醒次数、最近使用应用列表、按日/周的使用分布。

#### 5.4 NetworkUsage（网络流量）

| 组件 | 职责 |
|------|------|
| `NetworkUsageViewModel` | 网络流量状态管理 |
| `NetworkRepository` | 网络数据接口 |
| `AndroidNetworkInfoReader` | 系统 `TrafficStats` 数据读取 |

展示内容：移动数据/Wi-Fi 使用量、应用流量排行、流量包使用进度。

#### 5.5 NetworkScan（网络扫描）

| 组件 | 职责 |
|------|------|
| `NetworkScanViewModel` | 网络扫描主 ViewModel |
| `NetworkScanDevicesViewModel` | 设备列表子 ViewModel |
| `NetworkScanSessionStore` | 扫描会话状态 |
| `NetworkRepository` | 网络数据接口 |

展示内容：当前 WiFi 信息、已连接设备列表、设备 IP/MAC/厂商、网络安全性检测。

#### 5.6 NetworkSpeed（网速监控）

| 组件 | 职责 |
|------|------|
| `NetworkSpeedViewModel` | 实时网速状态管理 |
| `NetworkRepository` | 网络数据接口 |

展示内容：实时上传/下载速度、速度图表、悬浮窗显示。

#### 5.7 WhatsAppCleaner（WhatsApp 清理）

采用 MVI 模式（Phase / UiState / Action）：

| 组件 | 职责 |
|------|------|
| `WhatsAppCleanerViewModel` | MVI 状态管理（Idle → Loading → Content → Cleaning → Done） |
| `FileRepository` | 文件数据接口 |

展示内容：WhatsApp 图片/视频/音频/文档分类，支持预览和选择性清理。

#### 5.8 NotificationCleaner（通知清理）

| 组件 | 职责 |
|------|------|
| `NotificationCleanerViewModel` | 通知管理状态 |
| `NotificationRepository` | 通知数据接口 |
| `NotificationSettingsGateway` | 通知设置网关（读取/修改通知权限） |

展示内容：通知列表（按应用分组）、通知内容预览、一键清理/选择性清理。

---

## 6. 其他功能

### 6.1 Onboarding（引导页）

**目录**: `feature/onboarding/`

| 组件 | 职责 |
|------|------|
| `OnboardingScanViewModel` | 引导页扫描动画和状态管理 |
| `OnboardingPreferences` | 引导状态持久化（是否首次启动） |

引导页是安装后首次启动时展示的页面，包含扫描动画和功能介绍。首页通过 `OnboardingPreferences` 判断是否从引导页进入。

### 6.2 Home（首页）

**目录**: `feature/home/`

| 组件 | 职责 |
|------|------|
| `HomeViewModel` | 首页状态管理 |
| `HomeTabContent` | 首页 Tab（JUNK_CLEAN、ANTI_VIRUS、APP_LOCK） |
| `ToolBoxTabContent` | 工具箱 Tab（TOOLBOX 组功能入口） |
| `FilesManagerTabContent` | 文件管理 Tab（FILES 组功能入口） |

首页采用三 Tab 布局，对应三个 FeatureGroup，每个 Tab 展示对应组的功能入口卡片。

### 6.3 Settings（设置）

**目录**: `feature/settings/`

| 组件 | 职责 |
|------|------|
| `SettingsViewModel` | 设置项状态管理 |
| `SettingsRepository` | 设置数据持久化 |
| `ManagePermissionsViewModel` | 权限管理 ViewModel |
| `ManagePermissionsScreen` | 权限管理页面 |

### 6.4 Splash（启动页）

详见 [06-启动与运行时](./06-startup-runtime.md)

---

## 7. DI 模块

项目使用依赖注入框架管理所有对象的生命周期和作用域。

### 7.1 DataModule

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/di/DataModule.kt`

提供所有数据层组件的单例实例：

```kotlin
val dataModule = module {
    // Repository
    single<CleanRepository> { CleanRepositoryImpl(get()) }
    single<VirusSecurityRepository> { VirusSecurityRepositoryImpl(get()) }
    single<AppLockRepository> { AppLockRepositoryImpl(get()) }
    single<FileRepository> { FileRepositoryImpl(get()) }
    single<DeviceInfoRepository> { DeviceInfoRepositoryImpl(get(), get(), get()) }
    single<BatteryHistoryRepository> { BatteryHistoryRepositoryImpl(get(), get()) }
    single<AppUsageRepository> { AppUsageRepositoryImpl(get()) }
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }

    // Data Sources
    single { StorageDataSource(get()) }
    single { AppUsageDataSource(get()) }
    single { AndroidNetworkInfoReader(get()) }
    single { CpuTemperatureResolver() }

    // Scanners (JunkClean)
    single { CacheScanner(get()) }
    single { AdJunkScanner(get()) }
    single { ApkScanner(get()) }
    single { ResidualScanner(get()) }
    single { TempFileScanner(get()) }
    single { DuplicateFileScanner(get()) }
    single { MemoryCleaner(get()) }

    // Services
    single { FeatureFlowRuntime(get(), get(), get()) }
    single { AppSessionCoordinator() }
    single { AnalyticsTracker() }
    single { AdRuntime(get()) }

    // Scan State
    single { CleanSessionStore(get()) }
    single { SharedScanState() }
}
```

### 7.2 PresentationModule

**文件**: `app/src/main/java/com/quickcleanpro/phonecleaner/app/di/PresentationModule.kt`

通过工厂模式提供所有 ViewModel：

```kotlin
val presentationModule = module {
    // HOME
    viewModel { JunkCleanViewModel(get(), get(), get()) }
    viewModel { VirusScanViewModel(get(), get()) }
    viewModel { AppLockViewModel(get(), get(), get()) }

    // TOOLBOX
    viewModel { DeviceInfoViewModel(get()) }
    viewModel { BatteryInfoViewModel(get(), get()) }
    viewModel { AppUsageViewModel(get()) }
    viewModel { NetworkUsageViewModel(get()) }
    viewModel { NetworkScanViewModel(get(), get()) }
    viewModel { NetworkSpeedViewModel(get()) }
    viewModel { WhatsAppCleanerViewModel(get(), get()) }
    viewModel { NotificationCleanerViewModel(get(), get()) }

    // FILES
    viewModel { (feature: FeatureKey) -> PhotosManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> SimilarPhotosManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> PhotoPrivacyManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> ScreenshotsManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> VideosManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> AudiosManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> LargeFilesManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> DuplicateFilesManagerViewModel(feature, get(), get()) }
    viewModel { (feature: FeatureKey) -> DocumentsManagerViewModel(feature, get(), get()) }

    // 其他
    viewModel { HomeViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { OnboardingScanViewModel(get()) }
    viewModel { ManagePermissionsViewModel(get()) }
}
```

> **注意**: FILES 组 ViewModel 使用参数化工厂，通过 `FeatureKey` 区分不同的文件类型功能。

---

## 8. Repository 服务层清单

所有 Repository 均遵循 **接口 + 实现** 模式，便于单元测试时 Mock。

| Repository 接口 | 实现类 | 所属功能 |
|------------------|--------|----------|
| `CleanRepository` | `CleanRepositoryImpl` | JunkClean |
| `VirusSecurityRepository` | `VirusSecurityRepositoryImpl` | AntiVirus |
| `AppLockRepository` | `AppLockRepositoryImpl` | AppLock |
| `FileRepository` | `FileRepositoryImpl` | FILES 组 + WhatsAppCleaner |
| `DeviceInfoRepository` | `DeviceInfoRepositoryImpl` | DeviceInfo |
| `BatteryHistoryRepository` | `BatteryHistoryRepositoryImpl` | BatteryInfo |
| `AppUsageRepository` | `AppUsageRepositoryImpl` | AppUsage |
| `NetworkRepository` | `NetworkRepositoryImpl` | NetworkUsage / NetworkScan / NetworkSpeed |
| `NotificationRepository` | `NotificationRepositoryImpl` | NotificationCleaner |
| `SettingsRepository` | `SettingsRepositoryImpl` | Settings |

### 接口设计示例

```kotlin
interface FileRepository {
    suspend fun scanFiles(fileType: FileType): List<FileItem>
    suspend fun deleteFiles(files: List<FileItem>): Result<Unit>
    suspend fun getFileDetail(fileId: String): FileDetail?
    fun observeFileChanges(): Flow<List<FileItem>>
}

class FileRepositoryImpl(
    private val contentResolver: ContentResolver,
    private val fileScanner: FileScanner
) : FileRepository {
    // 实现细节
}
```

---

## 9. FeatureFlowRuntime 横切关注点

`FeatureFlowRuntime` 是贯穿所有功能的横切关注点，负责统一的运行时行为编排。

### FeatureOperationEvent 密封接口

```kotlin
sealed interface FeatureOperationEvent {
    data class ScanStarted(val feature: FeatureKey) : FeatureOperationEvent
    data class ScanFinished(
        val feature: FeatureKey,
        val hasResult: Boolean
    ) : FeatureOperationEvent
    data class OperationStarted(
        val feature: FeatureKey,
        val action: OperationAction
    ) : FeatureOperationEvent
    data class OperationFinished(
        val feature: FeatureKey,
        val action: OperationAction,
        val success: Boolean
    ) : FeatureOperationEvent
}
```

### OperationAction 枚举

```kotlin
enum class OperationAction {
    CLEAN,           // 清理操作（JunkClean 等）
    DELETE,          // 删除操作（文件管理类功能）
    REMOVE_LOCATION, // 移除位置信息（PhotoPrivacy）
    TEST             // 测试操作
}
```

### DefaultFeatureFlowRuntime 职责

```kotlin
class DefaultFeatureFlowRuntime(
    private val analyticsTracker: AnalyticsTracker,
    private val appSessionCoordinator: AppSessionCoordinator,
    private val adRuntime: AdRuntime
) : FeatureFlowRuntime {

    override fun onScanStarted(feature: FeatureKey) {
        // 1. 追踪扫描事件
        analyticsTracker.trackFeatureOperationEvent(
            FeatureOperationEvent.ScanStarted(feature)
        )
        // 2. 设置忙状态
        appSessionCoordinator.setBusy(true)
    }

    override fun onScanFinished(feature: FeatureKey, hasResult: Boolean) {
        // 1. 追踪完成事件
        analyticsTracker.trackFeatureOperationEvent(
            FeatureOperationEvent.ScanFinished(feature, hasResult)
        )
        // 2. 清除忙状态
        appSessionCoordinator.setBusy(false)
        // 3. 无结果时可能触发广告
        if (!hasResult) {
            adRuntime.showInterstitialAd(AdScene.ScanNoResult)
        }
    }

    override fun onOperationStarted(feature: FeatureKey, action: OperationAction) {
        analyticsTracker.trackFeatureOperationEvent(
            FeatureOperationEvent.OperationStarted(feature, action)
        )
        appSessionCoordinator.setBusy(true)
    }

    override fun onOperationFinished(
        feature: FeatureKey,
        action: OperationAction,
        success: Boolean
    ) {
        analyticsTracker.trackFeatureOperationEvent(
            FeatureOperationEvent.OperationFinished(feature, action, success)
        )
        appSessionCoordinator.setBusy(false)
        // 操作成功后展示插屏广告
        if (success) {
            adRuntime.showInterstitialAd(AdScene.mapFromFeature(feature, action))
        }
    }

    override fun onFeatureExit(feature: FeatureKey, reason: FeatureExitReason) {
        // 映射退出原因到广告场景
        val adScene = AdScene.mapFromExitReason(feature, reason)
        adRuntime.showInterstitialAd(adScene)
    }
}
```

### 横切行为总结

| 关注点 | 触发时机 | 行为 |
|--------|----------|------|
| 分析追踪 | 所有 FeatureOperationEvent | 发送到 AnalyticsTracker |
| 忙状态 | ScanStarted / OperationStarted | 设置 AppSessionCoordinator 为忙 |
| 忙状态 | ScanFinished / OperationFinished | 清除 AppSessionCoordinator 忙状态 |
| 插屏广告 | ScanFinished (无结果) | 展示无结果广告 |
| 插屏广告 | OperationFinished (成功) | 展示操作成功广告 |
| 插屏广告 | FeatureExit | 根据退出原因展示广告 |

### AdScene 映射

```kotlin
enum class AdScene {
    ScanNoResult,
    CleanSuccess,
    DeleteSuccess,
    PhotoPrivacySuccess,
    FeatureExitNormal,
    FeatureExitWithResult,
    FeatureExitNoResult
}
```

`AdScene` 通过 `FeatureKey` + `OperationAction` 或 `FeatureKey` + `FeatureExitReason` 的组合映射生成。

---

## 10. 架构总结

### 全功能架构总览

```
┌─────────────────────────────────────────────────────────┐
│                      Presentation                        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────────┐  │
│  │  Screen  │ │  View   │ │ Router  │ │   Route      │  │
│  │(无状态UI) │ │ (State) │ │(导航)   │ │(VM+Effect)   │  │
│  └────┬─────┘ └────┬────┘ └────┬────┘ └──────┬───────┘  │
│       │           │          │             │            │
│       └───────────┴──────────┴─────────────┘            │
│                        │  (MVI)                          │
├────────────────────────┼────────────────────────────────┤
│                      Domain                             │
│  ┌─────────────────────┼─────────────────────────────┐  │
│  │     FeatureContract  │   FeatureReducer            │  │
│  │  (UiState/Action/    │   (reduce: S→A→S)          │  │
│  │   Effect)            │                             │  │
│  └──────────────────────┴─────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                        Data                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐            │
│  │Repository│ │DataSource│ │   Scanner    │            │
│  │ (接口)    │ │(系统API)  │ │ (文件系统)    │            │
│  └──────────┘ └──────────┘ └──────────────┘            │
├─────────────────────────────────────────────────────────┤
│                  Cross-Cutting                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │          FeatureFlowRuntime                       │   │
│  │  (Analytics + Session + Ads)                     │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │               DI (DataModule + PresentationModule)│   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 关键设计原则

1. **MVI 优先**：复杂功能（JunkClean、AntiVirus、WhatsAppCleaner）采用完整 MVI 架构，简单功能采用简化版 ViewModel + StateFlow
2. **共享基础设施**：FILES 组复用 `BaseFileManagerViewModel`、`FileOperationExecutor`、`FileManagerRouteSupport`
3. **横切关注点集中管理**：分析、会话状态、广告逻辑统一由 `FeatureFlowRuntime` 处理
4. **接口隔离**：所有 Repository 均定义接口，便于单元测试 Mock
5. **模板化开发**：新增功能从 `templates/feature/` 模板创建，确保架构一致性
