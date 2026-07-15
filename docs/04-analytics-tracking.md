# Quick Clean Pro 数据追踪体系

## 1. 概述

Quick Clean Pro 采用三层架构进行数据追踪，通过 SDK 桥接层将业务事件最终上报至 Thinking Analytics 服务端。同时集成 Singular 归因服务，用于广告归因分析。

---

## 2. 三层架构

```
AnalyticsTracker (业务层)
  → AnalyticsDispatcher (异常隔离层)
    → SdkAnalyticsSink (SDK 桥接层)
      → AdvertiseSdk.logEvent() → Thinking Analytics 服务端
```

### 2.1 AnalyticsSink 接口

**文件:** `app/src/main/java/com/quickcleanpro/phonecleaner/common/analytics/AnalyticsSink.kt`

定义了 6 个抽象操作，作为所有数据上报的底层契约：

| 方法 | 说明 |
|------|------|
| `track(eventName, properties)` | 上报事件及其属性 |
| `setSuperProperties(properties)` | 设置公共属性，会附加到之后所有事件上 |
| `deviceId()` | 获取设备 ID |
| `setUserOnceProperty(key, value)` | 设置一次性用户属性，仅首次设置生效 |
| `setUserProperty(key, value)` | 设置用户属性（可覆盖） |
| `addUserProperty(key, value)` | 累加型用户属性（数值递增） |

### 2.2 SdkAnalyticsSink

**文件:** `app/src/main/java/com/quickcleanpro/phonecleaner/common/analytics/SdkAnalyticsSink.kt`

实现 `AnalyticsSink` 接口，将所有操作委托给 `AdvertiseSdk`：

| 接口方法 | SDK 调用 |
|----------|----------|
| `track(eventName, properties)` | `AdvertiseSdk.logEvent(eventName, properties)` |
| `setSuperProperties(properties)` | `AdvertiseSdk.setSuperProperties(properties)` |
| `deviceId()` | `AdvertiseSdk.getThinkingDeviceId()` |
| `setUserOnceProperty(key, value)` | `AdvertiseSdk.setUserOnceAttr(key, value)` |
| `setUserProperty(key, value)` | `AdvertiseSdk.setUserAttr(key, value)` |
| `addUserProperty(key, value)` | `ThinkingAttr.setUserAddAttr(key, value)` |

### 2.3 AnalyticsDispatcher

**文件:** `app/src/main/java/com/quickcleanpro/phonecleaner/common/analytics/AnalyticsDispatcher.kt`

异常隔离层，所有对底层 Sink 的调用均包裹在 `try/catch` 中。当底层调用发生异常时：

- 异常不会向上传播到业务层，保证 App 正常运行不受影响
- 异常通过 `onFailure` 回调上报，便于排查数据上报链路问题

---

## 3. AnalyticsTracker - 业务层

**文件:** `app/src/main/java/com/quickcleanpro/phonecleaner/common/analytics/AnalyticsTracker.kt`

单例对象，内部使用 `AnalyticsDispatcher(SdkAnalyticsSink)` 组合。

### 3.1 应用生命周期追踪

#### onAppForeground()

App 进入前台时触发：

1. 记录前台开始时间（用于后续使用时长的计算）
2. 写入一次性用户属性：`DEVICE_ID`、`FIRST_OPEN_TIME`
3. 设置用户属性 `LATEST_OPEN_TIME`
4. 递增用户属性 `TOTAL_OPEN_NUM`

#### onAppBackground()

App 进入后台时触发：

1. 计算本次在前台停留的时长
2. 将时长累加到用户属性 `TOTAL_USE_TIME`

#### handleLaunchIntent(intent)

处理启动 Intent：

1. 提取 Intent 中的 `AppOpenFrom` extra
2. 映射为 `traffic_source` 超级属性：

| AppOpenFrom 值 | traffic_source |
|---------------|----------------|
| `"Push"` | `"fcm_push"` |
| `"app_push"` | `"app_push"` |
| `"persistent"` | `"persistent"` |
| `"app_shortcuts"` | `"app_shortcuts"` |

---

### 3.2 用户属性

| 属性 | 写入方式 | 类型 | 说明 |
|------|---------|------|------|
| `device_id` | Once | String | 设备唯一标识 |
| `first_open_time` | Once | Timestamp | 首次打开应用时间 |
| `latest_open_time` | Set | Timestamp | 最近一次打开时间 |
| `total_open_num` | Add | Number | 累计打开次数 |
| `total_use_time` | Add | Number | 累计使用时长（毫秒） |

---

### 3.3 业务事件清单

#### 启动/引导事件

| 事件名 | 触发时机 | 参数 |
|--------|----------|------|
| `splash_display` | Splash 广告展示 | `duration_time` |
| `enter_guide_page_scanning` | 进入引导页扫描动画 | — |
| `enter_guide_page_scan_result` | 进入引导页扫描结果 | — |
| `click_continue_guidepage` | 用户点击「继续」按钮 | — |
| `click_skip_guidepage` | 用户点击「跳过」按钮 | — |

#### 权限事件

| 事件名 | 触发时机 | 参数 |
|--------|----------|------|
| `agree_filemanage_permission` | 用户同意文件管理权限 | — |
| `agree_push_permission` | 用户同意推送权限 | `noticeflag` |
| `disagree_filemanage_permission` | 用户拒绝文件管理权限 | — |
| `disagree_push_permission` | 用户拒绝推送权限 | `noticeflag` |

#### 首页事件

| 事件名 | 触发时机 | 参数 |
|--------|----------|------|
| `enter_homepage` | 进入首页 | — |

#### 功能事件

| 事件名 | 触发时机 | 参数 |
|--------|----------|------|
| `click_corefeatures_btn` | 点击核心功能入口按钮 | `details` |
| `enter_corefeatures_page` | 进入核心功能页面 | — |

#### 弹窗事件

| 事件名 | 触发时机 | 参数 |
|--------|----------|------|
| `rate_popup` | 评分弹窗展示 | `referrer_name`, `if_ok` |
| `filemanager_popup` | 文件管理弹窗展示 | `referrer_name`, `if_ok` |
| `notification_popup` | 通知权限弹窗展示 | `referrer_name`, `if_ok` |

#### 合规事件

| 事件名 | 触发时机 | 参数 |
|--------|----------|------|
| `check_privacy` | 用户查看隐私政策 | — |
| `check_terms` | 用户查看服务条款 | — |

---

### 3.4 事件参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| `duration_time` | Number | 持续时间（毫秒） |
| `referrer_name` | String | 来源页面名称，如 `guidepage`、`corefeatures` |
| `details` | String | 功能详情，如 `junkClean`、`fileManage` 等 |
| `if_ok` | Boolean | 用户在弹窗中的选择（`true` / `false`） |
| `noticeflag` | Number | 通知标记 |
| `traffic_source` | String | 流量来源（超级属性，见 3.1） |

---

### 3.5 功能操作追踪

`trackFeatureOperation(event)` 方法对特定事件进行额外处理：

- **ScanFinished 无结果时：** 调用 `incrementCleanCompleted()` 递增完成计数
- **OperationFinished 成功且属于 `cleanCompletionActions` 时：** 调用 `incrementCleanCompleted()` 递增完成计数

---

### 3.6 参数清理

`Map<String, Any?>.clean()` 扩展函数在事件上报前过滤无效值：

- 过滤 `null` 值
- 过滤键为空的条目
- 过滤值为空白字符串的条目

该函数在 `AnalyticsTracker.track()` 内部调用，确保上报数据的整洁性。

---

## 4. Thinking Analytics 配置

**配置来源:** `config/product.json`

### 基础配置

| 配置项 | 值 |
|--------|-----|
| `thinkingAppKey` | 从配置文件读取 |
| `thinkingServerUrl` | `https://mar2.top` |

### SDK 自动追踪

#### 已启用

| 自动事件 | 说明 |
|----------|------|
| `APP_START` | 应用启动时自动上报 |
| `APP_END` | 应用结束时自动上报 |
| `APP_INSTALL` | 应用安装时自动上报 |

#### 未启用（已注释）

| 自动事件 | 说明 |
|----------|------|
| `APP_VIEW_SCREEN` | 页面浏览自动追踪 |
| `APP_CLICK` | 点击事件自动追踪 |
| `APP_CRASH` | 崩溃自动追踪 |

---

## 5. Singular 归因集成

**文件:** `app/src/main/java/com/quickcleanpro/phonecleaner/common/ads/AdvertiseConfigFactory.kt`

### 配置

| 配置项 | 来源 |
|--------|------|
| `enabled` | `true` |
| `apiKey` | 本地配置 |
| `secret` | 本地配置 |

### 归因回调

Singular 归因回调触发后，将以下归因信息写入 ThinkingData 的 User 属性（`userSet`）：

| 属性 | 说明 |
|------|------|
| `network` | 广告投放网络 |
| `campaign_name` | 广告活动名称 |
| `fromNature` | 归因自然量标识 |

---

## 6. 数据流完整路径

```
业务触发
  → AnalyticsTracker.track(eventName, params)
    → Map.clean() 参数清理
      → AnalyticsDispatcher.track(eventName, cleanParams) [包裹在 try/catch 中]
        → SdkAnalyticsSink.track(eventName, cleanParams)
          → AdvertiseSdk.logEvent(eventName, cleanParams)
            → Thinking Analytics SDK
              → 网络请求
                → Thinking Analytics 服务端
```

### 异常处理流程

```
SdkAnalyticsSink.track() 抛出异常
  → AnalyticsDispatcher.track() 中 try/catch 捕获
    → 调用 onFailure(exception) 回调
      → 异常被记录，不传播到业务层
```

---

## 7. SDK 内部自动事件

以下事件由 **PDFFox AdvertiseSdk 内部自动上报**，非 App 业务代码直接触发：

### 广告 SDK 状态

| 事件名 | 说明 |
|--------|------|
| `adv_sdk_initcomplete` | 广告 SDK 初始化完成 |

### 广告聚合

| 事件名 | 说明 |
|--------|------|
| `ad_mediation_display` | 广告聚合展示 |

### 广告生命周期

| 事件名 | 说明 |
|--------|------|
| `ad_occur` | 广告请求发生 |
| `ad_start_loading` | 广告开始加载 |
| `ad_finish_loading` | 广告加载完成 |
| `ad_impression` | 广告曝光 |
| `ad_click` | 广告点击 |
| `ad_close` | 广告关闭 |

> **注意：** 这些事件由 SDK 内部管理，与 App 业务事件（第 3.3 节）属于不同的上报通道，但数据最终均汇入同一 Thinking Analytics 项目。

---

## 8. 架构总结

| 层 | 职责 | 关键设计 |
|----|------|----------|
| **AnalyticsTracker** | 业务语义封装，生命周期管理，参数清洗 | 单例，业务方唯一入口 |
| **AnalyticsDispatcher** | 异常隔离，防止底层崩溃影响业务 | try/catch + onFailure 回调 |
| **SdkAnalyticsSink** | SDK API 桥接，接口适配 | 纯委托，无业务逻辑 |
| **AdvertiseSdk** | 底层 SDK 封装 | 与 Thinking Analytics 服务端通信 |
