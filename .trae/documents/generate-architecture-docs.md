# 生成项目架构文档计划

## 摘要

基于对代码库的全面探索，生成 **7 份** 架构文档到 `docs/` 目录，覆盖路由导航、权限框架、广告系统、埋点追踪、功能模块、启动运行时六大核心体系。

---

## 文档清单

| 序号 | 文件名 | 内容 | 预计行数 |
|---|---|---|---|
| 1 | `docs/00-architecture-overview.md` | 项目总览：架构分层、技术栈、模块关系图 | ~200 |
| 2 | `docs/01-routing-navigation.md` | 路由导航框架：AppDestination、AppNavigator、FeatureEntryRouter、AppNavigation、AppRouteTransitions、各 Routes 模块 | ~300 |
| 3 | `docs/02-permission-framework.md` | 权限框架：PermissionType、PermissionManager、CleanXPermissionCoordinator、UI 层、通知权限子系统 | ~350 |
| 4 | `docs/03-advertising-system.md` | 广告系统全链路：SDK 配置、AdRuntime、AdScene、AdNavigationPolicy、功能集成、冷启动广告流程 | ~400 |
| 5 | `docs/04-analytics-tracking.md` | 数据追踪体系：AnalyticsTracker、AnalyticsDispatcher、SdkAnalyticsSink、事件清单、数据流 | ~250 |
| 6 | `docs/05-feature-modules.md` | 功能模块详解：模板系统、FeatureKey/FeatureCatalog、22 个功能模块实现、DI 模块、服务层 | ~400 |
| 7 | `docs/06-startup-runtime.md` | 启动与运行时：MyApp、MainActivity、AppRoot、SdkInitializationCoordinator、Splash 状态机、AppSessionCoordinator | ~350 |

---

## 每份文档的详细提纲

### 1. `docs/00-architecture-overview.md`
- 项目简介与技术栈
- 整体分层架构图
- 源码目录结构
- 核心设计模式总览
- 文档导航（链接到其他 6 份文档）

### 2. `docs/01-routing-navigation.md`
- AppDestination 封闭类体系（38 个目的地）
- AppNavigator 导航器接口与实现
- FeatureEntryRouter 广告门控装饰器
- AppNavigation NavGraph 注册
- AppRouteTransitions 动画过渡
- 各 Routes 模块：Splash、Home、Clean、AntiVirus、Toolbox、FileManager、Settings
- 通知深链接处理（NotificationNavigation）
- FeatureCatalog 功能目录
- 子路由 ViewModel 共享机制

### 3. `docs/02-permission-framework.md`
- 10 种权限类型（PermissionType）
- 14 个受保护操作（CleanXProtectedAction）
- PermissionHandler 接口与具体实现
- PermissionManager 核心引擎
- CleanXPermissionCoordinator 会话状态机
- UI 层：PermissionPromptHost、对话框体系
- CompositionLocal 注入机制
- 通知权限专用子系统（MVI ViewModel + 状态机）
- 管理权限设置页
- 权限分析埋点

### 4. `docs/03-advertising-system.md`
- 五层架构总览
- 第一层：SDK 配置（AdvertiseConfigFactory、AdvertiseSdkAdapter）
- 第二层：关键定义（AdAreaKeys、AdScene、AdCallbacks）
- 第三层：策略与路由（AdNavigationPolicy、AdPlacementRegistry）
- 第四层：运行时引擎（AdRuntime 完整解析）
- 第五层：功能集成（FeatureFlowRuntime、FeatureEntryRouter）
- 广告位完整映射表（34 个广告位）
- 冷启动广告流程
- 外部 Activity 生命周期处理
- ad_policy.json 配置说明

### 5. `docs/04-analytics-tracking.md`
- 三层架构（Tracker → Dispatcher → Sink）
- AnalyticsTracker 所有事件清单
- 用户属性体系
- 流量来源追踪
- App 前后台生命周期追踪
- 功能操作事件（FeatureOperationEvent）
- 参数清理机制
- Thinking Analytics 配置
- Singular 归因集成

### 6. `docs/05-feature-modules.md`
- 功能模板系统（FeatureContract、Reducer、ViewModel、Screen、Route）
- FeatureKey 枚举（21 个功能）
- FeatureGroup 分组（HOME / FILES / TOOLBOX）
- 各功能模块详解：
  - JunkClean（最完整 MVI 实现）
  - AntiVirus（多扫描模式）
  - AppLock（服务导向的重量级功能）
  - FileManager 共享基础设施（10 个文件管理功能）
  - Toolbox 功能（9 个工具功能）
  - WhatsApp / Notification Cleaner
  - Onboarding / Home / Settings / ManagePermissions
- DI 模块：DataModule + PresentationModule
- Repository 服务层清单
- FeatureFlowRuntime 横切关注点

### 7. `docs/06-startup-runtime.md`
- 完整冷启动流程（12 步）
- MyApp Application 初始化
- MainActivity 入口
- AppRoot Compose 根节点
- SdkInitializationCoordinator 并发编排
- AppSessionCoordinator 忙状态追踪
- AppRuntimeBindings 运行时对象创建
- AppRuntimeEffects 副作用注册
- AppGlobalOverlays 全局覆盖层
- Splash 状态机详解（SplashContract → StateMachine → ViewModel → UI）
- 进度条动画暂停/恢复机制
- 通知启动快路径

---

## 实施步骤

按顺序生成 7 份文档，每份完成后标记进度。

## 验证

- 每份文档中引用的文件路径均为项目实际路径
- 所有架构图使用文本描述
- 关键代码示例使用 Kotlin 语法高亮
