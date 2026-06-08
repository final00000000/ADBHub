# Compose Desktop ADB Hub 基础框架

## Goal

使用 Compose Multiplatform 创建一个桌面应用（Windows/macOS/Linux），用于车机系统的 ADB 管理操作。这是一个从 Android 项目扩展到桌面端的跨平台应用。

## What I already know

* 现有项目是 Android 应用（`com.zhang.adbhub`）
* 使用 Gradle + Kotlin，Android API 36，minSdk 28
* 目标功能：
  - Root 权限管理
  - Remount 分区操作
  - 推送 APK 到设备
  - 导出设备日志
  - 实时 grep 筛选日志流
* 需要设计桌面端 UI
* 需要建立基础框架（项目结构 + UI 骨架）

## Assumptions (temporary)

* 桌面端和 Android 端可能共享部分业务逻辑
* ADB 操作通过本地 adb 命令行工具执行
* 需要检测并连接多个设备（车机可能通过 USB/网络连接）
* 日志实时过滤需要高性能文本处理

## Decision (Architecture)

**选择：多模块架构 - Desktop + Android + Shared Common**

* **结构**：
  - `app/` - 现有 Android 应用模块（保持不变）
  - `desktop/` - 新增桌面应用模块（独立可运行）
  - `common/` - 共享业务逻辑模块（ADB 交互、数据模型、工具类）
* **依赖关系**：`app` 和 `desktop` 都依赖 `common`
* **Why**：渐进式架构，最大化代码复用，两端可独立开发和运行
* **Consequences**：需要配置 Gradle 多模块，抽取共享逻辑需要设计清晰的接口边界

## Decision (ADB 交互)

**选择：使用 Dadb (Kotlin ADB 库)**

* **库**：`com.mobile-dev-inc:dadb` (纯 Kotlin ADB 协议实现)
* **Why**：
  - 纯 Kotlin，与项目技术栈一致
  - 支持 Kotlin 协程，适合异步操作
  - 跨平台 JVM，Desktop 和 Android 通用
  - API 清晰，类型安全
* **Consequences**：
  - 需要添加 Gradle 依赖
  - 少数极定制命令可能需要回退到 shell 方式
  - 学习库的 API（文档：https://github.com/mobile-dev-inc/dadb）

## Decision (UI 布局)

**选择：经典三栏布局**

```
┌─────────────────────────────────────────┐
│  [设备列表]  │   [操作区]   │  [日志区]  │
│   左侧边栏   │   选项卡式   │   右侧面板  │
│              │   - Root    │   实时滚动  │
│  Device 1 ✓  │   - Remount │   + 筛选框  │
│  Device 2    │   - Push    │   + 导出    │
│              │   - Logs    │             │
└─────────────────────────────────────────┘
```

* **Why**：
  - 适合专业调试工具，信息密度高
  - 设备列表常驻可见，快速切换
  - 日志和操作区并排，支持边操作边观察
* **Layout Details**：
  - 左栏：设备列表（宽度 ~200-250dp，可调节）
  - 中栏：操作选项卡（Root/Remount/Push/Export/Logs）
  - 右栏：日志实时查看器（宽度 ~400-500dp，可调节）
* **Consequences**：
  - 最小窗口宽度建议 1200px+
  - 需实现分栏拖拽调节（可选 MVP 后）

## Open Questions

### Technical Choices
* **ADB 交互方式**：
  - A. 直接调用本地 adb 可执行文件（ProcessBuilder/Runtime.exec）
  - B. 使用 ADB 库（如 dadb、adb-lib-ddmlib）
  - C. 混合方式（库为主，命令行为辅）

### UI Design
* **界面布局风格**：需要什么样的 UI 结构？
  - 设备列表位置（左侧边栏 / 顶部工具栏 / 下拉选择器）
  - 主功能区布局（选项卡 / 多面板 / 单页面多操作按钮）
  - 日志查看器设计（独立窗口 / 内嵌面板 / 浮动面板）

### MVP Scope
* **首批实现的功能优先级**？
  - 设备连接管理（必需）
  - Root/Remount 操作
  - APK 推送
  - 日志导出
  - 实时日志筛选

## Requirements (MVP Scope)

**本次任务实现完整 MVP**，包含：

### 1. 项目结构
- ✅ 创建 `common/` 模块（共享逻辑）
- ✅ 创建 `desktop/` 模块（桌面应用）
- ✅ 配置 Gradle 多模块依赖
- ✅ 添加 Compose Multiplatform + Dadb 依赖

### 2. UI 框架（三栏布局）
- ✅ 主窗口搭建（最小尺寸 1200x800）
- ✅ 左栏：设备列表面板（支持选择/切换）
- ✅ 中栏：操作选项卡（Push/Logs）
- ✅ 右栏：日志查看面板（实时文本展示）

### 3. 设备管理
- ✅ 检测已连接 ADB 设备
- ✅ 显示设备列表（序列号、型号、状态）
- ✅ 支持选择当前操作设备
- ✅ 设备连接状态实时更新

### 4. Push APK 功能
- ✅ 文件选择器（选择本地 APK）
- ✅ 上传 APK 到选中设备
- ✅ 显示上传进度
- ✅ 成功/失败提示

### 5. 实时日志查看
- ✅ 启动 `adb logcat` 流
- ✅ 实时显示日志文本（滚动到底部）
- ✅ 基础 grep 筛选（关键词输入框）
- ✅ 启动/停止日志流控制
- ✅ 日志导出到本地文件（TXT）

**UI Requirements**:
* 清晰的设备选择/切换机制
* 操作按钮组（功能入口）
* 日志查看器（支持实时滚动、搜索、高亮）

## Acceptance Criteria (MVP)

* [ ] Compose Desktop 项目可成功编译并启动
* [ ] 三栏 UI 布局完整显示（设备列表/操作区/日志区）
* [ ] 能检测并列出已连接的 ADB 设备
* [ ] 选择设备后可正常切换操作目标
* [ ] Push APK 功能：
  - [ ] 文件选择器可选择 .apk 文件
  - [ ] APK 可成功上传到设备
  - [ ] 显示上传进度和结果提示
* [ ] 日志查看功能：
  - [ ] 可启动/停止 logcat 流
  - [ ] 日志实时显示并自动滚动
  - [ ] grep 筛选可正常过滤日志行
  - [ ] 可导出日志到本地 TXT 文件

## Definition of Done (team quality bar)

* 代码通过 Kotlin lint/格式检查
* 基础单元测试覆盖（ADB 交互逻辑）
* README 更新项目结构和启动方式
* 截图或演示 GIF 展示 UI 基础布局

## Out of Scope (explicit)

* Android 端的改造或迁移（保持现有 Android 项目不变）
* Root 权限管理（后续任务）
* Remount 分区操作（后续任务）
* 高级 ADB 功能（如性能监控、抓包、屏幕录制）
* 设备固件刷写、recovery 模式操作
* 日志语法高亮（首版仅纯文本）
* 日志行号、时间戳解析（仅展示原始文本）
* 多语言国际化（首版仅中文或英文）
* 用户配置持久化（首版使用默认配置）
* 分栏拖拽调节宽度（固定布局）
* 多设备并发操作（仅单设备操作）

## Technical Notes

* 项目根目录：`E:\myProject\ADBHub`
* 现有 Android 模块：`app/`
* Gradle 配置：`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`
* Compose Multiplatform 官方文档：https://www.jetbrains.com/lp/compose-multiplatform/
* Dadb 库文档：https://github.com/mobile-dev-inc/dadb
* 需要检查 Gradle 版本是否支持 Compose Multiplatform（通常需要 Gradle 8.0+）

### 技术栈
- **语言**：Kotlin 1.9+
- **构建工具**：Gradle 8.x
- **UI 框架**：Compose Multiplatform (Desktop target)
- **ADB 库**：Dadb (com.mobile-dev-inc:dadb)
- **协程**：kotlinx.coroutines（异步操作）
- **文件选择器**：Compose Desktop FileDialog

### 模块依赖图
```
common/  (Kotlin Multiplatform)
  ├─ Dadb
  └─ Kotlin Coroutines

desktop/ (JVM)
  ├─ depends on: common
  ├─ Compose Desktop
  └─ JVM Desktop runtime

app/ (Android)
  ├─ depends on: common
  └─ Android SDK
```

## Implementation Plan

### Phase 1: 项目结构搭建
1. 创建 `common/` 模块（Kotlin Multiplatform Library）
   - `build.gradle.kts` 配置 JVM target
   - 添加 Dadb 依赖
2. 创建 `desktop/` 模块（Compose Desktop Application）
   - 配置 Compose Desktop plugin
   - 依赖 `common` 模块
3. 更新根 `settings.gradle.kts` 包含新模块
4. 验证编译通过

### Phase 2: Common 层 - ADB 业务逻辑
1. `AdbManager` 接口/类（设备检测、连接管理）
2. `Device` 数据模型（序列号、型号、状态）
3. `AdbOperations` 接口/类（Push APK、Logcat）
4. 基础单元测试

### Phase 3: Desktop UI - 主框架
1. `MainWindow.kt` - 应用入口和主窗口
2. `DeviceListPanel.kt` - 左侧设备列表组件
3. `OperationPanel.kt` - 中间操作选项卡
4. `LogPanel.kt` - 右侧日志面板
5. 三栏布局组合

### Phase 4: 功能实现 - Push APK
1. 文件选择器集成
2. UI 进度条组件
3. 调用 `AdbOperations.pushApk()`
4. 错误处理和提示

### Phase 5: 功能实现 - 实时日志
1. Logcat 流接口（Kotlin Flow）
2. 日志文本展示（LazyColumn + 自动滚动）
3. Grep 筛选逻辑
4. 启动/停止控制
5. 导出到文件功能

### Phase 6: 测试和优化
1. 端到端功能测试
2. UI 响应性优化
3. 错误处理完善
4. README 文档更新
