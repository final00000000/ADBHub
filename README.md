# ADB Hub - 车机调试工具

Compose Desktop 多平台 ADB 管理工具，专为车机系统调试设计。

## 项目结构

```
ADBHub/
├── common/          # 共享业务逻辑模块（Kotlin JVM）
│   └── src/main/kotlin/com/zhang/adbhub/common/
│       ├── model/       # 数据模型
│       │   ├── Device.kt      # 设备模型
│       │   └── AdbResult.kt   # 结果封装
│       └── adb/         # ADB 操作
│           ├── AdbManager.kt      # ADB 管理接口
│           └── DadbManager.kt     # 实现类
│
├── desktop/         # 桌面应用模块（Compose Desktop）
│   └── src/main/kotlin/com/zhang/adbhub/desktop/
│       ├── Main.kt              # 应用入口
│       ├── viewmodel/
│       │   └── MainViewModel.kt # 状态管理
│       └── ui/                  # UI 组件
│           ├── MainScreen.kt         # 主界面（三栏布局）
│           ├── DeviceListPanel.kt    # 设备列表面板
│           ├── OperationPanel.kt     # 操作面板
│           └── LogPanel.kt           # 日志面板
│
└── app/             # Android 应用模块（保持原样）
```

## 功能特性

### ✅ 已实现

1. **设备管理**
   - 自动检测已连接的 ADB 设备
   - 显示设备序列号、型号、状态
   - 支持设备选择和切换
   - 一键刷新设备列表

2. **Push APK**
   - 文件选择器选择本地 APK
   - 推送到选中设备
   - 实时反馈推送状态

3. **设备操作命令**
   - **Root**: 以 root 权限重启 ADB 守护进程
   - **Remount**: 重新挂载系统分区为读写模式
   - **Reboot**: 重启设备
   - **Reboot Recovery**: 重启到 Recovery 模式
   - **Reboot Bootloader**: 重启到 Bootloader 模式

4. **应用管理**
   - **启动应用**: 通过包名和 Activity 启动指定 APP
   - **查看应用信息**: 获取应用的详细状态（dumpsys package）
   - **停止应用**: 强制停止正在运行的应用
   - **清除应用数据**: 清除应用数据和缓存

5. **实时日志查看**
   - 启动/停止 logcat 流
   - 实时显示日志并自动滚动
   - Grep 关键词筛选
   - 导出日志到本地文件（.txt）

### 🚧 后续计划

- 日志语法高亮
- 多设备并发操作
- 配置持久化
- Shell 命令执行

## 技术栈

- **语言**: Kotlin 2.1.0
- **UI 框架**: Compose Multiplatform 1.7.1
- **构建工具**: Gradle 9.4.1
- **ADB 交互**: Dadb 1.2.9 + adb 命令行
- **异步处理**: Kotlin Coroutines + Flow

## 环境要求

- **JDK**: 11 或更高版本
- **ADB**: Android SDK Platform Tools
  - 应用启动时会自动检测 ADB
  - 如果未检测到，会显示设置引导界面
  - 支持自定义 ADB 路径配置

### ADB 安装方式

**自动检测路径**（应用会自动扫描）：
- Windows: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`
- Windows: `C:\Android\Sdk\platform-tools\adb.exe`
- macOS: `~/Library/Android/sdk/platform-tools/adb`
- Linux: `~/Android/Sdk/platform-tools/adb`
- 系统 PATH 环境变量中的 `adb`

**手动安装**：
- Windows: 下载 [Platform Tools](https://developer.android.com/tools/releases/platform-tools)
- macOS: `brew install android-platform-tools`
- Linux: `sudo apt install adb` 或 `sudo yum install android-tools`

**如果你已有 Android SDK**：
- 在应用的设置界面中，选择 SDK 目录下的 `platform-tools/adb.exe`
- 例如：`E:\AndroidSDK\platform-tools\adb.exe`

## 快速开始

### 1. 克隆项目

```bash
cd ADBHub
```

### 2. 构建项目

```bash
# 构建所有模块
./gradlew build

# 仅构建桌面应用
./gradlew :desktop:build
```

### 3. 运行桌面应用

```bash
./gradlew :desktop:run
```

应用将启动并显示主窗口（1200x800 px）。

### 4. 首次运行配置

**如果应用显示"未检测到 ADB 工具"**：

1. 应用会自动显示设置引导界面
2. 查看自动检测到的 ADB 路径列表
3. 选择正确的路径，或点击"浏览"手动选择
4. 如果你的 Android SDK 在 `E:\AndroidSDK`，选择：
   ```
   E:\AndroidSDK\platform-tools\adb.exe
   ```
5. 点击"测试连接"验证配置
6. 保存后应用会自动开始检测设备

**后续修改配置**：
- 点击右上角的"设置"图标（⚙️）
- 修改 ADB 路径或重置为默认

### 5. 连接设备

1. 通过 USB 连接车机或 Android 设备
2. 确保设备已开启 USB 调试
3. 在应用中点击"刷新"按钮
4. 选择目标设备

**常见问题**：
- **设备显示"unauthorized"**: 在设备上允许 USB 调试授权
- **设备显示"offline"**: 拔插 USB 线或执行 `adb kill-server && adb start-server`
- **未显示设备**: 检查 USB 调试是否开启，驱动是否正常

### 5. 使用功能

#### Push APK
1. 切换到 "Push APK" 选项卡
2. 点击 "选择 APK 文件"
3. 选择本地 APK 文件
4. 点击 "推送到设备"

#### 设备操作
1. 切换到 "设备操作" 选项卡
2. 选择需要执行的命令：
   - **Root**: 获取 root 权限（需要设备支持）
   - **Remount**: 挂载系统分区为读写（需要 root）
   - **Reboot**: 重启设备
   - **Reboot Recovery**: 进入 Recovery 恢复模式
   - **Reboot Bootloader**: 进入 Fastboot/Bootloader 模式
3. 点击对应按钮执行，结果会显示在下方

#### 应用管理
1. 切换到 "应用管理" 选项卡
2. 输入应用包名（如 `com.android.settings`）
3. 执行操作：
   - **启动应用**: 需要同时输入 Activity 名称（如 `.Settings`）
   - **查看应用信息**: 查看应用详细状态（包括权限、组件、版本等）
   - **停止应用**: 强制停止正在运行的应用
   - **清除数据**: 清除应用的所有数据和缓存（⚠️ 不可恢复）

#### 查看日志
1. 在右侧日志面板输入筛选关键词（可选）
2. 点击 "启动日志流"
3. 实时查看过滤后的日志
4. 点击 "停止" 停止日志流
5. 点击 "保存" 图标导出日志到文件

## 打包分发

### 创建可执行文件

```bash
# 打包为原生安装包
./gradlew :desktop:packageDistributionForCurrentOS

# 生成的安装包位置：
# Windows: desktop/build/compose/binaries/main/msi/
# macOS: desktop/build/compose/binaries/main/dmg/
# Linux: desktop/build/compose/binaries/main/deb/
```

### 创建可运行 JAR

```bash
./gradlew :desktop:jar

# 运行 JAR
java -jar desktop/build/libs/desktop-1.0.0.jar
```

## 开发指南

### 添加新功能

1. **Common 层**：在 `common/src/main/kotlin/` 添加业务逻辑
2. **Desktop UI**：在 `desktop/src/main/kotlin/ui/` 添加 UI 组件
3. **ViewModel**：更新 `MainViewModel.kt` 添加状态和操作

### 目录约定

- `common/model/` - 数据模型和实体
- `common/adb/` - ADB 交互逻辑
- `desktop/ui/` - Compose UI 组件
- `desktop/viewmodel/` - 状态管理和业务协调

## 故障排查

### 找不到设备

- 确认 adb 命令可用：`adb devices`
- 检查 USB 调试是否开启
- 尝试 `adb kill-server && adb start-server`

### 推送 APK 失败

- 检查 APK 文件路径是否正确
- 确认设备有足够存储空间
- 查看错误提示信息

### 日志流无响应

- 停止后重新启动日志流
- 检查设备连接状态
- 重新选择设备

## 许可证

本项目仅供学习和内部使用。

## 联系方式

问题反馈请联系项目维护者。
