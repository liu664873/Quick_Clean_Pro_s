# Quick Clean Pro 广告系统全链路

> 本文档覆盖 Quick Clean Pro 广告系统的完整架构，从底层 SDK 配置到上层功能集成，共五层体系。

---

## 1. 五层架构总览

```
第五层: 功能集成    FeatureFlowRuntime / FeatureEntryRouter / AppNavigator.openNotificationTarget
第四层: 运行时引擎  AdRuntime (队列/去重/超时/门控/冷启动)
第三层: 策略路由    AdNavigationPolicy / AdPlacementRegistry
第二层: 关键定义    AdAreaKeys / AdScene / AdCallbacks
第一层: SDK 配置    AdvertiseConfigFactory / AdvertiseSdkAdapter
```

架构自下而上：底层封装第三方 SDK 配置，中间层定义广告位与路由策略，顶层将广告能力嵌入功能流程。

---

## 2. 第一层：SDK 配置

### 2.1 AdvertiseConfigFactory

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdvertiseConfigFactory.kt`
- **类型**：单例对象（`object`）

负责构建 `AdvertiseSdkConfig`，聚合所有第三方 SDK 及平台服务的配置项。

| 配置模块 | 启用状态 | 说明 |
|---|---|---|
| `adMob` | `enabled = true` | App ID + 5 种广告位 ID（banner / interstitial / native / open / rewarded） |
| `facebook` | `enabled = true` | Facebook Audience Network |
| `tiktok` | `enabled = true` | TikTok SDK |
| `firebase` | `enabled = true` | Analytics + Cloud Messaging |
| `remoteConfig` | `enabled = true` | 加密远程配置 |
| `thinking` | `enabled = true` | ThinkingData 分析 |
| `singular` | `enabled = true` | 归因服务 |
| `safe` | `enabled = true` | 包名 / 签名 / 调试器 安全验证 |
| `playIntegrity` | `enabled = true` | Google Play Integrity 验证 |
| `push` | `enabled = true` | FCM + 常驻通知 |
| `server` | — | 区分 releaseHost / testingHost 服务端地址 |

---

### 2.2 AdvertiseSdkAdapter

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdvertiseSdkAdapter.kt`
- **类型**：`AdvertiseSdk` 的薄封装层

对外提供统一的广告操作接口：

| 方法 | 说明 |
|---|---|
| `initialize(context, isTest): suspend` | 异步初始化 SDK |
| `initConsent()` | 初始化隐私同意状态 |
| `showSplashConsent()` | 展示启动隐私同意弹窗 |
| `showOpenAd()` | 展示开屏广告 |
| `showInterstitial()` | 展示插屏广告 |
| `preloadStartup()` | 启动阶段预加载 |
| `preloadMainPage()` | 进入主页时预加载 |
| `preloadAfterInterstitial()` | 插屏关闭后预加载 |
| `isAppOpenEnabled()` | 查询开屏广告启用状态 |
| `setAppOpenEnabled()` | 设置开屏广告启用状态 |
| `suppressNextAppOpen()` | 抑制下一次开屏广告 |

> **预加载时机**：Startup → MainPage → InterstitialClosed，三层递进确保广告素材就绪。

---

## 3. 第二层：关键定义

### 3.1 AdAreaKeys

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdAreaKeys.kt`
- **结构**：4 个子对象，共计 **60+ 个广告位常量**

#### Open（2 个）

| 常量 | 说明 |
|---|---|
| `OPEN_PAGE` | 冷启动开屏广告 |
| `FOREGROUND` | 回到前台开屏广告 |

#### Banner（1 个）

| 常量 | 说明 |
|---|---|
| `HOME_BOTTOM` | 首页底部横幅 |

#### Interstitial（30+ 个，按场景分组）

**引导场景：**
| 常量 | 说明 |
|---|---|
| `NEW_GUIDE_SCAN_FINISH` | 引导页扫描完成 |
| `NEW_GUIDE_SKIP` | 引导页跳过 |

**功能入口场景（12 个）：**
| 常量 | 说明 |
|---|---|
| `MAIN_JUNK_CLEAN` | 垃圾清理入口 |
| `MAIN_FILE_MANAGE` | 文件管理入口 |
| `MAIN_VIRUS_ANTI` | 病毒查杀入口 |
| `MAIN_APP_LOCK` | 应用锁入口 |
| `MAIN_DEVICE_INFO` | 设备信息入口 |
| `MAIN_BATTERY_INFO` | 电池信息入口 |
| `MAIN_APP_USAGE` | 应用使用入口 |
| `MAIN_NETWORK_USAGE` | 网络使用入口 |
| `MAIN_NETWORK_SCAN` | 网络扫描入口 |
| `MAIN_NETWORK_SPEED` | 网速测试入口 |
| `MAIN_NOTIFICATION_CLEAN` | 通知清理入口 |
| `MAIN_WHATSAPP_CLEANER` | WhatsApp 清理入口 |

**功能完成场景（5 个）：**
| 常量 | 说明 |
|---|---|
| `MAIN_JUNK_CLEAN_FINISH` | 垃圾清理完成 |
| `MAIN_FILE_MANAGE_FINISH` | 文件管理完成 |
| `MAIN_NETWORK_SPEED_FINISH` | 网速测试完成 |
| `MAIN_NOTIFICATION_CLEAN_FINISH` | 通知清理完成 |
| `MAIN_WHATSAPP_CLEANER_FINISH` | WhatsApp 清理完成 |

**返回首页场景（12 个）：**
| 常量 | 说明 |
|---|---|
| `RETURN_FROM_JUNK_CLEAN` | 从垃圾清理返回 |
| `RETURN_FROM_FILE_MANAGE` | 从文件管理返回 |
| `RETURN_FROM_VIRUS_ANTI` | 从病毒查杀返回 |
| `RETURN_FROM_APP_LOCK` | 从应用锁返回 |
| `RETURN_FROM_DEVICE_INFO` | 从设备信息返回 |
| `RETURN_FROM_BATTERY_INFO` | 从电池信息返回 |
| `RETURN_FROM_APP_USAGE` | 从应用使用返回 |
| `RETURN_FROM_NETWORK_USAGE` | 从网络使用返回 |
| `RETURN_FROM_NETWORK_SCAN` | 从网络扫描返回 |
| `RETURN_FROM_NETWORK_SPEED` | 从网速测试返回 |
| `RETURN_FROM_NOTIFICATION_CLEAN` | 从通知清理返回 |
| `RETURN_FROM_WHATSAPP_CLEANER` | 从 WhatsApp 清理返回 |

#### Native（28 个）

各功能的结果页、对话框等位置的原生广告位。

---

### 3.2 AdScene（广告场景）

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdScene.kt`
- **类型**：`sealed interface`

定义所有广告触发时刻：

| 场景 | 说明 |
|---|---|
| `OnboardingScanFinished` | 引导页扫描完成 |
| `OnboardingSkipped` | 引导页跳过 |
| `EnterFeature(feature, route)` | 进入某功能 |
| `OperationFinished(feature, action, success)` | 某项操作完成 |
| `ReturnHome(feature)` | 返回首页 |
| `PermissionRejected(feature)` | 权限被拒绝 |

#### 扩展函数

| 函数 | 说明 |
|---|---|
| `FeatureOperationEvent.toAdScene()` | 将操作事件映射为 `AdScene`（仅 `OperationFinished`） |
| `FeatureOperationEvent.adRequestId()` | 生成去重请求 ID |

---

### 3.3 AdCallbacks

工具函数 `once(block)`：通过 `AtomicBoolean` 保证回调最多调用一次，防止广告回调重复触发。

---

## 4. 第三层：策略与路由

### 4.1 AdNavigationPolicy

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdNavigationPolicy.kt`

决定在导航过程中是否展示入口插屏广告。决策逻辑：

1. 目标路由在黑名单中 → **不展示**
   - 黑名单：`Splash`、`Onboarding`、`Home`、`Settings`、`Tab`
2. 来源路由在黑名单中 → **不展示**
3. 目标路由不在白名单中 → **不展示**
   - 白名单由 `FeatureCatalog.specs` 动态构建
4. 来源功能与目标功能相同（功能内导航）→ **不展示**

---

### 4.2 AdPlacementRegistry

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdPlacementRegistry.kt`

建立 `AdScene` → `AdAreaKeys` 的映射关系：

| 映射类型 | 说明 |
|---|---|
| `enterArea` | 每个功能 → 对应的入口插屏键（9 个文件相关功能共享 `MAIN_FILE_MANAGE`） |
| `finishArea` | 仅部分功能有完成插屏键 |
| `returnArea` | 每个功能 → 对应的返回插屏键（`PermissionRejected` 场景复用返回键） |

---

## 5. 第四层：运行时引擎（AdRuntime）

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdRuntime.kt`

广告系统的核心调度引擎。

### 5.1 请求排队与去重

```
run(scene, requestId, onContinue)
  → AdPlacementRegistry 解析区域键
  → knownRequestIds 去重检查
  → ArrayDeque 队列入队
  → 串行调度执行
```

- `knownRequestIds`：`Set` 结构，相同 ID 永不重复入队
- `ArrayDeque`：FIFO 队列，保证串行

### 5.2 顺序执行

- 同一时刻只播放一个广告
- 当前广告调用 `complete()` 后，才从队列取出下一个
- `canShow()` 屏蔽条件：
  - 权限流程进行中
  - 外部返回冷却期（1.2s）
  - 功能操作进行中（`PermissionRejected` 场景豁免）

### 5.3 冷启动流程（`runColdStart`）

```
1. AppOpenGuard 获取开屏广告抑制锁
2. 显示隐私同意弹窗（双 6.5s 超时保护）
3. 加载开屏广告
   ├── 开始超时: 6.5s
   ├── 总超时: 30s
   └── 关闭后稳定期: 800ms
4. 进入主流程
```

> 所有广告操作均设有超时保护，确保流程不会卡死。

### 5.4 外部 Activity 生命周期

| 方法 | 说明 |
|---|---|
| `markExternalActivityLaunch()` | 标记外部 Activity 启动，抑制下一次开屏广告 |
| `onHostResumed()` | 宿主恢复，设置 1.2s 冷却期 |
| `dispose()` | 清理状态，恢复开屏广告 |

### 5.5 预加载

```
preload(stage, context)
├── Startup        → 启动时预加载
├── MainPage       → 进入主页时预加载
└── InterstitialClosed → 插屏关闭后预加载
```

### 5.6 内部组件

| 组件 | 说明 |
|---|---|
| `AdRuntimeDriver` 接口 | 定义广告运行驱动的抽象协议 |
| `AdvertiseAdRuntimeDriver` | 基于 AdvertiseSdk 的驱动实现 |
| `HandlerAdRuntimeScheduler` | 主线程延迟调度器 |
| `AppOpenGuard` | 引用计数门控，防止开屏广告与其他全屏内容冲突 |

---

## 6. 第五层：功能集成

### 6.1 FeatureFlowRuntime

- **文件**：`app/src/main/java/com/quickcleanpro/phonecleaner/app/runtime/featureflow/FeatureFlowRuntime.kt`

功能模块与广告系统的桥梁：

| 方法 | 说明 |
|---|---|
| `handleOperation(event)` | 追踪事件 + 设置忙碌状态 + 映射 `AdScene` + 运行插屏广告 |
| `exit(feature, reason)` | 映射 `FeatureExitReason` → `AdScene` → 运行插屏广告 |

> **注意**：`ScanStarted` 和 `ScanFinished` 事件不触发广告。

### 6.2 功能触发插屏的三种路径

#### 路径一：入口广告

```
用户点击功能
  → FeatureEntryRouter.open()
    → AdRuntime.runRouteEntry(fromRoute, targetRoute)
      → AdNavigationPolicy.entryAdDecision()
        → AdRuntime.run(EnterFeature)
          → AdvertiseSdkAdapter.showInterstitial()
```

#### 路径二：完成广告

```
功能操作完成
  → FeatureFlowRuntime.handleOperation()
    → event.toAdScene()
      → AdRuntime.run(OperationFinished)
```

#### 路径三：退出广告

```
功能退出
  → FeatureFlowRuntime.exit()
    → AdRuntime.run(ReturnHome / PermissionRejected)
```

---

## 7. 广告位完整映射

### 7.1 开屏广告（2 个）

| 广告位键 | 描述 |
|---|---|
| `OPEN_PAGE` | 冷启动开屏 |
| `FOREGROUND` | 回到前台开屏 |

### 7.2 Banner 广告（1 个）

| 广告位键 | 描述 |
|---|---|
| `HOME_BOTTOM` | 首页底部横幅 |

### 7.3 插屏广告（34 个）

#### 引导场景（2 个）

| 广告位键 | 描述 |
|---|---|
| `NEW_GUIDE_SCAN_FINISH` | 引导扫描完成 |
| `NEW_GUIDE_SKIP` | 引导跳过 |

#### 功能入口场景（12 个）

| 广告位键 | 描述 |
|---|---|
| `MAIN_JUNK_CLEAN` | 垃圾清理入口 |
| `MAIN_FILE_MANAGE` | 文件管理入口 |
| `MAIN_VIRUS_ANTI` | 病毒查杀入口 |
| `MAIN_APP_LOCK` | 应用锁入口 |
| `MAIN_DEVICE_INFO` | 设备信息入口 |
| `MAIN_BATTERY_INFO` | 电池信息入口 |
| `MAIN_APP_USAGE` | 应用使用入口 |
| `MAIN_NETWORK_USAGE` | 网络使用入口 |
| `MAIN_NETWORK_SCAN` | 网络扫描入口 |
| `MAIN_NETWORK_SPEED` | 网速测试入口 |
| `MAIN_NOTIFICATION_CLEAN` | 通知清理入口 |
| `MAIN_WHATSAPP_CLEANER` | WhatsApp 清理入口 |

#### 功能完成场景（5 个）

| 广告位键 | 描述 |
|---|---|
| `MAIN_JUNK_CLEAN_FINISH` | 垃圾清理完成 |
| `MAIN_FILE_MANAGE_FINISH` | 文件管理完成 |
| `MAIN_NETWORK_SPEED_FINISH` | 网速测试完成 |
| `MAIN_NOTIFICATION_CLEAN_FINISH` | 通知清理完成 |
| `MAIN_WHATSAPP_CLEANER_FINISH` | WhatsApp 清理完成 |

#### 返回首页场景（12 个）

| 广告位键 | 描述 |
|---|---|
| `RETURN_FROM_JUNK_CLEAN` | 从垃圾清理返回 |
| `RETURN_FROM_FILE_MANAGE` | 从文件管理返回 |
| `RETURN_FROM_VIRUS_ANTI` | 从病毒查杀返回 |
| `RETURN_FROM_APP_LOCK` | 从应用锁返回 |
| `RETURN_FROM_DEVICE_INFO` | 从设备信息返回 |
| `RETURN_FROM_BATTERY_INFO` | 从电池信息返回 |
| `RETURN_FROM_APP_USAGE` | 从应用使用返回 |
| `RETURN_FROM_NETWORK_USAGE` | 从网络使用返回 |
| `RETURN_FROM_NETWORK_SCAN` | 从网络扫描返回 |
| `RETURN_FROM_NETWORK_SPEED` | 从网速测试返回 |
| `RETURN_FROM_NOTIFICATION_CLEAN` | 从通知清理返回 |
| `RETURN_FROM_WHATSAPP_CLEANER` | 从 WhatsApp 清理返回 |

### 7.4 原生广告（28 个）

各功能的结果页、对话框等位置（具体键名由 `AdAreaKeys.Native` 子对象定义）。

---

## 8. 关键设计模式

| 设计模式 | 实现方式 | 目的 |
|---|---|---|
| **去重 + 串行** | `knownRequestIds` Set + `ArrayDeque` 队列 | 同一请求不重复入队，广告逐一展示 |
| **超时安全** | 所有广告操作均设超时 | 防止广告加载/展示卡死主流程 |
| **门控 + 抑制** | `AppOpenGuard` 引用计数 | 防止开屏广告与其他全屏内容冲突 |
| **忙状态门控** | `canShow()` 检查 | 权限流程、冷却期、操作中不展示广告 |
| **Once 保证** | `once()` + `AtomicBoolean` | 回调仅触发一次 |
| **功能分组** | 9 个文件相关功能共享同一套插屏键 | 减少广告位配置成本 |
| **配置驱动** | `AdPlacementRegistry` 统一管理映射 | 集中配置，通过测试验证一致性 |

---

## 9. ad_policy.json

- **路径**：`config/ad_policy.json`

### 全局开关

| 配置项 | 值 |
|---|---|
| `global_ad_switch` | `true` |

### 广告单元配置

共 **34 个广告单元**，每个单元包含：

| 字段 | 说明 |
|---|---|
| `areakey` | 广告位键（对应 `AdAreaKeys`） |
| `rate` | 展示率（0 = 关闭，1 = 开启） |
| `frequency_caps` | 频次限制 |
| `frequency_caps.max_per_hour` | 每小时最大展示次数 |
| `frequency_caps.max_per_day` | 每日最大展示次数 |
| `frequency_caps.interval_seconds` | 最小展示间隔 |
| `show_delay_seconds` | 展示前延迟 |

### 特殊配置

| 广告单元 | rate | 说明 |
|---|---|---|
| `openMainBottomAdv`（首页底部 Banner） | `0` | **关闭** |
| 其余所有广告单元 | `1` | **开启** |

### 全局限制

| 配置项 | 值 |
|---|---|
| 每日上限（daily limit） | `50` |
| 计数窗口 | 24 小时 |

---

## 10. 全链路数据流总结

```
┌─────────────────────────────────────────────────────┐
│                    功能集成层                         │
│  FeatureFlowRuntime.handleOperation() / exit()       │
│  FeatureEntryRouter.open()                          │
└──────────────────────┬──────────────────────────────┘
                       │ AdScene
┌──────────────────────▼──────────────────────────────┐
│                    策略路由层                         │
│  AdNavigationPolicy.entryAdDecision()               │
│  AdPlacementRegistry: AdScene → AdAreaKeys          │
└──────────────────────┬──────────────────────────────┘
                       │ AdAreaKeys
┌──────────────────────▼──────────────────────────────┐
│                    运行时引擎                         │
│  AdRuntime.run() → 去重检查 → 队列入队               │
│  → canShow() 门控 → 执行展示 → complete()            │
│  超时保护 / 冷却期 / AppOpenGuard 抑制               │
└──────────────────────┬──────────────────────────────┘
                       │ showInterstitial() / showOpenAd()
┌──────────────────────▼──────────────────────────────┐
│                    SDK 适配层                         │
│  AdvertiseSdkAdapter                                │
│    → AdMob / Facebook / TikTok                      │
└─────────────────────────────────────────────────────┘
```
