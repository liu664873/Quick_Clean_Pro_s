# 更新权限框架文档

## 摘要

当前 [02-permission-framework.md](file:///d:/pros/my/Quick_Clean_Pro_s/docs/02-permission-framework.md) 与代码实际情况存在 **10 类不匹配**，需要全面修正类名、方法签名、文件路径和架构描述。

---

## 核心发现：文档 vs 代码对照

| # | 文档描述 | 实际代码 | 严重程度 |
|---|---------|---------|---------|
| 1 | `CleanXPermissionCoordinator` 接口 | `AppPermissionCoordinator` 接口 | **高** |
| 2 | `PermissionManager` 类 | `PermissionEngine` 类 | **高** |
| 3 | `CleanXProtectedAction` 枚举 | `ProtectedAction` 枚举 | **高** |
| 4 | `CleanXPermissionRegistry` 独立文件 | 不存在；`ProtectedAction` 枚举内联 action specs | **高** |
| 5 | `guard(target, onRejected, onGranted)` | `ensure(target, mode, onDenied, onGranted)` | **高** |
| 6 | `isGranted(target: PermissionTarget)` | `isGranted(permission: PermissionType)` | **高** |
| 7 | `PermissionRequestPlan` sealed class | `PermissionDecision` sealed interface | **中** |
| 8 | `PermissionManager<F>` 泛型设计 | `PermissionEngine` 非泛型，接收 `List<PermissionType>` | **中** |
| 9 | `CleanXPermissionCoordinatorState` 状态实现 | `PermissionCoordinator` 直接在 `app/runtime/permission/` | **中** |
| 10 | 文件路径大量不准确 | `handler/` 子目录不存在；`PermissionPromptHost` 位置不对等 | **中** |

---

## 修改方案

### 1. 类名/接口名全局替换

| 旧（文档） | 新（代码） |
|-----------|-----------|
| `CleanXPermissionCoordinator` | `AppPermissionCoordinator` (+ `PermissionCoordinator` 实现) |
| `PermissionManager` | `PermissionEngine` |
| `CleanXProtectedAction` | `ProtectedAction` |
| `PermissionRequestPlan` | `PermissionDecision` |
| `CleanXPermissionCoordinatorState` | `PermissionCoordinator` |
| `CleanXPermissionRegistry` | 移除，合并到 `ProtectedAction` 枚举描述中 |
| `RuntimePermissionDenialStore` | `RuntimePermissionDenialStore` 接口 + `AppRuntimePermissionDenialStore` 实现 |

### 2. 方法签名修正

`AppPermissionCoordinator` 接口：
```kotlin
fun isGranted(permission: PermissionType): Boolean
fun ensure(action: ProtectedAction, mode: PermissionPromptMode, onDenied: () -> Unit, onGranted: () -> Unit)
fun ensure(permission: PermissionType, mode: PermissionPromptMode, onDenied: () -> Unit, onGranted: () -> Unit)
fun openSettings(permission: PermissionType, onReturn: () -> Unit)
```

`PermissionPromptMode` 枚举：`Explained`（弹窗解释） / `Direct`（直接系统请求）

### 3. 文件路径修正

| 章节 | 旧路径 | 新路径 |
|------|-------|-------|
| PermissionEngine | `common/permission/PermissionManager.kt` | `common/permission/PermissionEngine.kt` |
| AppPermissionCoordinator | `common/permission/CleanXPermissionCoordinator.kt` | `common/permission/AppPermissionCoordinator.kt` |
| PermissionCoordinator 实现 | `common/permission/...` | `app/runtime/permission/PermissionCoordinator.kt` |
| PermissionPromptHost | `common/permission/ui/PermissionPromptHost.kt` | `app/runtime/permission/PermissionPromptHost.kt` |
| AppPermissionHost | `app/runtime/permission/AppPermissionHost.kt` | 路径正确，但内部逻辑需修正 |
| CompositionLocal | `common/permission/ui/...` | `common/permission/ui/PermissionCompositionLocal.kt` |
| PermissionAnalytics | `common/permission/analytics/PermissionAnalytics.kt` | `app/runtime/permission/PermissionAnalytics.kt` |
| PermissionModels | `common/permission/...` | `common/permission/PermissionModels.kt` |

### 4. 新增内容

- **NotificationPermissionPolicy.kt** (`app/runtime/permission/NotificationPermissionPolicy.kt`): 通知权限的 MVI 完整类型定义（`UiState`(12字段)、`Action`、`Effect`、`SideEffect`、reducer）+ 状态机逻辑
- **NotificationPermissionViewModel.kt** (`app/runtime/permission/NotificationPermissionViewModel.kt`): ViewModel 实现（snapshot 刷新 + reducer 驱动 + 副作用处理 + noticeFlag 计算）
- **PermissionObservation.kt** (`common/permission/ui/PermissionObservation.kt`): `rememberPermissionGranted` Composable 辅助函数
- **AppRuntimePermissionDenialStore**: `PermissionEngine` 使用的具体实现，基于 `PermissionPreferences` 持久化

### 5. 删除/修正的错误描述

- 删除"CleanXPermissionRegistry actionSpecs/itemSpecs"独立注册章节 → 改为 `ProtectedAction` 枚举内联描述
- 修正 `PermissionStatus` 从 sealed class 改为 `data class(granted: Boolean, missing: List<PermissionType>)`
- 修正 `PermissionHandler` 接口增加 `val permission: PermissionType` 属性
- 修正 PermissionPromptHost 参数：接收 `PermissionCoordinator` + `ExternalActivityLauncher` + `permissionPrompt` composable lambda
- 修正 AppPermissionHost 内部：只创建 1 个 `PermissionEngine` + 1 个 `PermissionCoordinator`
- 修正 `PermissionTarget` sealed interface（`Action(action)` / `Permission(permission)`）
- 新增 `PermissionPromptMode` 枚举说明

---

## 实施步骤

1. 用 [Write](file:///d:/pros/my/Quick_Clean_Pro_s/docs/02-permission-framework.md) 工具**覆写**整个文档
2. 保持现有文档结构（1-13 章），每章内容根据实际代码修正
3. 验证所有文件路径可以使用 IDE 跳转

## 修改文件

| 文件 | 操作 |
|------|------|
| `docs/02-permission-framework.md` | 覆写 |
