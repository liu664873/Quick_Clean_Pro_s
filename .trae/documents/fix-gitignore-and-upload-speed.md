# 修复 .gitignore 及上传速度优化方案

## 摘要

分析发现项目工作目录总大小 **408 MB**（5154 个文件），但 Git 实际追踪仅 344 个文件（pack 大小 ~6.1 MB）。上传慢的核心原因是大量未追踪的构建产物和缺失的 .gitignore 规则导致 Git 操作扫描成本极高，同时存在一些本不该追踪的文件已被提交。

## 当前状态分析

### 工作目录大小分布
| 目录/文件 | 大小 | 文件数 | Git 追踪状态 |
|---|---|---|---|
| `app/build/` | ~350 MB | 4430 文件 / 1000 目录 | 未追踪（`**/build/` 已生效）|
| `.gradle/` | ~8 MB | 14 文件 | 未追踪（`.gradle/` 已生效）|
| `.idea/caches/` | 94 KB | 1 文件 | **已追踪**（不应追踪）|
| `docs/*.xlsx` | - | 2 二进制文件 | **已追踪**（不应追踪）|
| 源代码+资源 | ~45 MB | ~344 文件 | 正常追踪 |

### 当前 .gitignore 已覆盖
- `**/build/` - Android 构建输出
- `.gradle/` - Gradle 缓存
- `local.properties` + `config/*.local.properties` - 本地配置
- `.idea/workspace.xml`, `.idea/tasks.xml` 等 - IDE 用户态文件
- `*.iml`, `*.apk`, `*.aab`, `*.class`, `*.jar`, `*.log` 等
- `.DS_Store`, `Thumbs.db`

### 当前 .gitignore 缺失项（问题所在）
1. **`.codex/`** - Trae IDE 环境配置目录，未被忽略
2. **`.idea/caches/`** - IDE 缓存目录，`deviceStreaming.xml`（94KB）已被追踪
3. **`docs/*.xlsx`** - 二进制 Excel 文件已被追踪
4. **`*.hprof`** - Java 内存 dump 文件未忽略
5. **缺少标准 Android/Gradle 补充项** - 如 `captures/`、`.externalNativeBuild/`、`*.ap_` 等

### 为什么上传慢
1. `app/build/` 有 **4430 个文件 + 1000 个目录**，即使被 .gitignore 忽略，Git 仍需要遍历扫描
2. 如果误执行 `git add .`，会尝试暂存数百 MB 的构建产物
3. `.idea/caches/` 和 `docs/*.xlsx` 已追踪，每次 push 都包含这些不必要的二进制文件

## 方案

### 步骤 1：完善 .gitignore

在现有 [.gitignore](file:///d:/pros/my/Quick_Clean_Pro_s/.gitignore) 基础上补充以下规则：

```gitignore
# Trae IDE / Codex environment
.codex/

# IDE caches (already tracked files need cleanup)
.idea/caches/
.idea/modules.xml
.idea/jarRepositories.xml
.idea/navEditor.xml

# Gradle additional
.gradle/
**/build/
!gradle/wrapper/gradle-wrapper.jar
local.properties
**/local.properties

# Android additional
*.ap_
*.aab
*.dex
*.hprof
.externalNativeBuild/
.cxx/
*.jks
*.keystore

# OS generated
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db

# Docs binary files
docs/*.xlsx
docs/*.xls
docs/*.pdf

# Logs
*.log
*.log.*

# Firebase / Google Services (if contains secrets - keep if public)
# google-services.json is project-specific, but should be reviewed

# Misc
*.swp
*.swo
*~
```

### 步骤 2：清理已被 Git 追踪的不必要文件

对于已追踪但应被忽略的文件，需要用 `git rm --cached` 移除追踪（文件保留在本地）：

```bash
# 移除 IDE 缓存
git rm --cached .idea/caches/deviceStreaming.xml

# 移除二进制文档
git rm --cached "docs/新清理埋点cleanDataTrack.xlsx"
git rm --cached "docs/新清理广告位areakey表-初版.xlsx"
```

### 步骤 3：清理本地构建产物（可选，释放磁盘空间）

```bash
# 清理 Gradle 构建缓存
./gradlew clean

# 或手动删除
Remove-Item -Recurse -Force app/build
```

### 步骤 4：验证

```bash
# 确认 .gitignore 生效
git status

# 确认 app/build/ 和 .codex/ 不再出现在 untracked 中
git status --short

# 确认清理后仓库大小
git count-objects -vH
```

## 修改文件清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `.gitignore` | 编辑 | 补充缺失的忽略规则 |
| `.idea/caches/deviceStreaming.xml` | `git rm --cached` | 移除 IDE 缓存追踪 |
| `docs/新清理埋点cleanDataTrack.xlsx` | `git rm --cached` | 移除二进制文档追踪 |
| `docs/新清理广告位areakey表-初版.xlsx` | `git rm --cached` | 移除二进制文档追踪 |

## 假设与决策
- `google-services.json` 保留追踪（Firebase 项目配置，通常不含敏感密钥）
- `app/libs/*.aar` 保留追踪（本地 SDK 依赖，构建必需）
- `docs/*.md` 保留追踪（项目文档）
- 不在 `.gitignore` 中添加 `gradlew` 或 `gradle/wrapper/`（构建工具链必需）

## 验证步骤
1. 修改 `.gitignore` 后运行 `git status`，确认 `.codex/` 不再出现在 untracked 列表
2. 执行 `git rm --cached` 后确认文件从 Git 索引移除但本地仍保留
3. 运行 `./gradlew clean` 清理构建产物后，确认工作目录大小显著减小
4. 提交并 push，观察上传速度改善
