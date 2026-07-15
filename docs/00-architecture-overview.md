# Quick Clean Pro 架构总览

## 项目简介
Quick Clean Pro 是一款 Android 手机清理应用，采用 Kotlin + Jetpack Compose 构建。

## 技术栈
- 语言: Kotlin
- UI: Jetpack Compose + Material 3
- 导航: Jetpack Navigation Compose
- DI: Koin
- 架构模式: MVI (Contract + State + Reducer + ViewModel)
- 广告 SDK: com.pdffox.adv (AdvertiseSdk)，聚合 AdMob + Facebook + TikTok
- 数据追踪: Thinking Analytics (通过 AdvertiseSdk 桥接)
- 归因: Singular
- 推送: Firebase Cloud Messaging
- 扫描引擎: Trustlook (病毒扫描)
- 最低 SDK: 29 (Android 10), 目标 SDK: 36

## 整体分层架构

```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)          │
│  Screen / Route / Components / Theme     │
├─────────────────────────────────────────┤
│         Presentation Layer (MVI)         │
│  ViewModel / Reducer / Contract / Effect │
├─────────────────────────────────────────┤
│          Domain Layer (Service)          │
│  Repository / UseCase / FeatureFlow      │
├─────────────────────────────────────────┤
│           Data Layer (Source)            │
│  Scanner / DataSource / Preferences      │
├─────────────────────────────────────────┤
│       Platform / Infrastructure          │
│  Ad SDK / Analytics / Permission / DI    │
└─────────────────────────────────────────┘
```

## 源码目录结构
- app/src/main/java/com/quickcleanpro/phonecleaner/
  - app/ - Application, MainActivity, AppRoot, DI, Navigation, Runtime
  - common/ - 共享基础设施
    - ads/ - 广告系统
    - analytics/ - 数据追踪
    - permission/ - 权限框架
    - ui/ - 通用 UI 组件
  - feature/ - 功能模块（每个功能一个子目录）
    - junkclean/ - 垃圾清理
    - antivirus/ - 病毒扫描
    - applock/ - 应用锁
    - files/ - 文件管理（10 个子功能共用基础设施）
    - toolbox/ - 工具箱（9 个工具）
    - startup/ - 启动页
    - home/ - 首页
    - settings/ - 设置
    - onboarding/ - 引导页
- config/ - 产品配置 (product.json, ad_policy.json)
- gradle/ - Gradle 配置 (libs.versions.toml)
- scripts/ - CI 脚本

## 核心设计模式

1. **MVI 架构**: 大部分功能遵循 Contract (UiState/Action/Effect) + Reducer (纯函数) + ViewModel (状态管理) + Route (Compose 入口) + Screen (无状态 UI)
2. **Sealed Class 路由注册**: AppDestination 封闭类统一管理所有 38 个页面路由
3. **Decorator 广告门控**: FeatureEntryRouter 装饰 AppNavigator，在导航前注入插屏广告检查
4. **泛型权限引擎**: PermissionManager<F> 同时服务于 CleanXProtectedAction 和 PermissionType
5. **Plan-then-Execute**: 权限管理器先计算计划 (requestPlan)，再由 UI Host 执行系统调用
6. **Once 保证**: 所有回调通过 once() 工具函数保证最多执行一次
7. **Sub-VM 共享**: 反病毒和文件管理的子路由通过 NavBackStackEntry 查找父级共享 ViewModel

## 核心数据流

### 广告流
```
功能操作 → FeatureFlowRuntime → AdScene → AdPlacementRegistry → areaKey
  → AdRuntime (队列+去重) → AdvertiseSdkAdapter → AdvertiseSdk → AdMob/Facebook/TikTok
```

### 埋点流
```
业务事件 → AnalyticsTracker → AnalyticsDispatcher (异常隔离) → SdkAnalyticsSink → AdvertiseSdk.logEvent → Thinking Analytics
```

### 权限流
```
功能调用 guard(action) → CleanXPermissionCoordinator → PermissionManager.requestPlan
  → 弹窗 / 系统请求 / 设置跳转 → 结果回传 → 重检 → onGranted / onRejected
```

## 功能清单 (22 个功能, 3 个分组)

**HOME 组**: JunkClean(垃圾清理), AntiVirus(病毒扫描), AppLock(应用锁)
**TOOLBOX 组**: DeviceInfo, BatteryInfo, AppUsage, NotificationCleaner, WhatsAppCleaner, NetworkUsage, NetworkScan, NetworkSpeed
**FILES 组**: Photos, SimilarPhotos, PhotoPrivacy, Screenshots, Videos, Audios, LargeFiles, DuplicateFiles, Documents

## 语言支持
11 种语言: en, de, es, fr, id, ja, ko, pt, th, vi, zh

## 文档导航
- [01-路由导航框架](./01-routing-navigation.md)
- [02-权限框架](./02-permission-framework.md)
- [03-广告系统](./03-advertising-system.md)
- [04-数据追踪体系](./04-analytics-tracking.md)
- [05-功能模块详解](./05-feature-modules.md)
- [06-启动与运行时](./06-startup-runtime.md)
