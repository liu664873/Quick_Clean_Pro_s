# 重构行为基线

本文件是重构第 0 步的行为冻结记录。它描述重构前必须保持的用户可见行为、外部契约和验收证据。重构期间只允许改变实现位置；除非先更新本文件并得到明确评审，不能改变这里列出的 route、广告位、埋点名、权限结果或配置语义。

基线建立日期：2026-07-12  
基线版本：当前工作区 `QuickCleanPRO`  1.0.8 / versionCode 9

## 使用方式

1. 在每个重构阶段开始前运行 `scripts/check-variant-boundaries.ps1`、`scripts/check-dependencies.ps1` 和 `scripts/check-brand-residue.ps1`。
2. 把脚本输出和本阶段人工验收结果保存到变更说明或 CI artifact；脚本只读，不会替换配置、移动文件或修复残留。
3. 对行为有疑问时，以当前发布包和本文件中的“待确认”项为准，先补证据再改代码。
4. 测试设备、系统版本、权限初始状态和广告开关必须写入验收记录，避免把环境差异误判为重构回归。

脚本退出码约定：

- `0`：检查通过。
- `1`：发现边界、依赖或品牌残留问题。
- `2`：输入路径、PowerShell 运行时或外部工具不可用，检查没有完成。

## 不可改变的契约

| ID | 场景 | 当前实现/证据 | 重构后的要求 |
| --- | --- | --- | --- |
| STARTUP-001 | 冷启动进入 Splash、协议/引导流程并最终进入首页 | `use/app/AppRoot.kt`、`use/skin/navigation/StartupRoutes.kt`、`use/skin/startup/SplashScreen.kt` | 启动请求只能被一个协调器消费；重复 recomposition 不得重复导航或重复初始化广告。 |
| STARTUP-002 | 通知点击启动并打开目标功能 | `use/core/startup/AppLaunchCoordinator.kt`、`use/skin/navigation/NotificationNavigation.kt`、`AppNotificationTargetRoutingTest.kt` | notification target 必须保留；同一个 intent 只消费一次；没有目标时回退到默认首页。 |
| STARTUP-003 | Splash 可见期间收到新的 Activity intent | `use/app/MainActivity.kt`、`use/core/startup/AppLaunchCoordinator.kt`、`AppLaunchCoordinatorTest.kt` | 新 intent 不能丢失、覆盖错误目标或产生第二次导航；外部 Activity 返回后继续原流程。 |
| PERMISSION-001 | 受保护功能缺权限时显示解释/授权入口 | `use/core/permission/*`、`use/skin/permission/*`、`PermissionController.kt` | 拒绝、取消、已授权和系统设置返回必须有明确结果；不能静默转换成成功。 |
| AD-001 | 冷启动/返回功能的 App Open 或插屏机会 | `use/core/ads/StartupAdCoordinator.kt`、`AdvertisePreloader.kt`、`AdPolicyGate.kt` | 广告策略、area key、频控和 suppress 状态保持；广告关闭后的继续动作 exactly-once。 |
| AD-002 | 权限、外部设置页或 Consent 期间抑制不合适的广告 | `use/core/ads/AppOpenAdSuppression.kt`、`use/app/AppRoot.kt` | 抑制范围按生命周期/场景结束；异常和取消不能留下永久 suppress。 |
| AD-003 | 广告策略与代码 area key 一致 | `config/ad_policy.json`、`native_ad_policy.json`、`use/core/ads/AdAreaKeys.kt`、`QuickCleanAdConfigTest.kt` | 不重命名或合并现有 area key；新增 key 必须同时更新 canonical 策略、测试和产品表；APK raw policy 由构建生成。 |
| NAV-001 | 页面 route 和通知 route alias 映射 | `app/navigation/AppDestination.kt`、通知路由映射、`NotificationRouteMappingTest.kt` | 页面、通知、广告回跳使用一个规范 destination；未知 route 有可观测的安全回退。 |
| ANALYTICS-001 | 启动、页面、权限和广告埋点 | `use/core/analytics/AnalyticsTracker.kt`、`docs/埋点上报代码位置说明.md` | 事件名、参数 key、area key 和触发时机保持兼容；SDK adapter 迁移不得导致重复上报。 |
| CONFIG-001 | applicationId、namespace、Firebase 包名一致 | `config/product.json`、`app/google-services.json`、`scripts/check-product-config.ps1` | 母包/马甲只通过配置替换；构建期发现不一致，不在运行时兜底。 |
| NOTIFY-001 | 常驻通知和工具通知快捷入口 | `use/service/notification/PersistentNotificationService.kt`、`ToolNotificationIntentFactory.kt`、`ToolNotificationSpecs.kt` | channel、requestCode、icon、route 和点击行为保持兼容；拆 Service 不得改变通知入口。 |
| FILE-001 | 文件扫描、选择、删除授权和结果页 | `use/feature/files/*`、`use/feature/junkclean/*` | 扫描可取消；用户选择只影响选中项；系统授权返回后继续或显示失败，不重复删除。 |

## 人工验收矩阵

每次重构至少记录下列场景。`结果/证据`列由执行人填写，未执行时保留“待确认”，不要用推测代替结果。

| 编号 | 前置条件 | 操作 | 预期结果 | 结果/证据 |
| --- | --- | --- | --- | --- |
| M-01 | 清除应用数据，网络可用 | 冷启动，完成 Splash/协议流程 | 只出现一个启动流程，最终到首页；无重复广告关闭回调 | 待确认 |
| M-02 | 应用已运行或在 Splash | 点击通知快捷入口/发送 notification intent | 打开对应功能；重复 intent 不重复 push 同一页面 | 待确认 |
| M-03 | 目标权限未授予 | 从首页进入需要权限的功能，拒绝一次，再允许 | 先显示说明和授权入口；拒绝显示失败/可重试；允许后继续原操作 | 待确认 |
| M-04 | Usage Access 或 Overlay 未授予 | 点击功能授权，进入系统设置，再返回 | 返回后重新读取真实权限；不依赖固定 delay 判断成功 | 待确认 |
| M-05 | 广告可用与不可用各一次 | 进入、返回并完成一个核心功能 | 广告不可用时业务仍可继续；可用时关闭后只触发一次后续动作 | 待确认 |
| M-06 | 文件库包含可删除和受保护文件 | 扫描、取消、选择部分文件、删除并处理系统授权 | 取消可结束扫描；只删除选择项；授权后结果计数准确 | 待确认 |
| M-07 | 首次安装与已有安装各一次 | 启动并观察埋点日志 | 事件名和参数与现有埋点表一致；同一用户动作不重复上报 | 待确认 |
| M-08 | `applicationId` 与 Firebase 配置匹配/不匹配两种构建 | 运行配置校验 | 匹配通过；不匹配在构建/脚本阶段明确失败 | 待确认 |

## 当前已知基线问题

这些是第 0 步需要留下证据的现状，不在基线建立阶段修复：

- `app/src/main/res/values-night/themes.xml` 仍包含 `Base.Theme.Cleanx`，属于旧主题命名残留。
- 法律链接双源已在第 1 步收口到 `config/product.json`，资源值和 BuildConfig 由构建插件生成。
- release 已移除显式 debug signing；正式构建必须由 CI 或本地发布配置提供签名。
- 旧 `use/` 包结构和部分 domain Android 类型属于迁移前状态；依赖检查默认只阻止新目录继续扩大违规，使用 `-Strict` 才扫描全部 legacy domain。

检查脚本因此可能在当前工作区报告已知问题。不要为了让检查变绿而在本阶段直接改生产代码；应把修复归入对应重构阶段。

## 阶段门禁

每个阶段完成前必须满足：

- 行为矩阵中受影响场景有设备记录或自动化测试证据。
- `check-variant-boundaries.ps1` 没有新增违规。
- `check-dependencies.ps1` 默认模式通过；若使用 `-Strict`，所有新增违规均已解释或修复。
- `check-brand-residue.ps1` 的输出只剩本文件列出的已知问题，且已登记后续步骤。
- 没有修改 area key、埋点事件名、通知 route、权限结果语义或产品配置来源而未更新本文档。

## 推荐命令

从仓库根目录执行（Windows PowerShell）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-variant-boundaries.ps1 -Root (Get-Location).Path
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-dependencies.ps1 -Root (Get-Location).Path
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-brand-residue.ps1 -Root (Get-Location).Path
```

重构完成后再运行严格模式：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-dependencies.ps1 -Root (Get-Location).Path -Strict
```
