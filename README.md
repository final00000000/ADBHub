# ADBHub

<div align="center">

![Version](https://img.shields.io/badge/version-1.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

**专为车机系统调试设计的 ADB 管理工具**

基于 Compose Multiplatform 打造的现代化桌面应用

[下载最新版本](https://github.com/final00000000/ADBHub/releases/latest) | [功能演示](#功能演示) | [快速开始](#快速开始)

</div>

---

## ✨ 功能特性

### 📱 设备管理
- 🔍 自动检测 ADB 设备
- 📊 实时显示设备信息（序列号、型号、状态）
- 🔄 一键刷新设备列表
- 🎯 多设备快速切换

### 🔧 设备操作命令
- 🔑 **Root 权限** - 以 root 权限重启 ADB 守护进程
- 💾 **重新挂载分区** - 将系统分区挂载为可写模式
- 🏠 **回到桌面** - 双重保障策略，完美适配车机系统
- 🔒 **启用 dm-verity** - 重新启用系统完整性验证
- 🔄 **设备重启** - 支持普通重启、Recovery、Bootloader

### 📦 APK 推送
- 📂 可视化文件选择器
- 🎯 自定义目标路径
- ⚡ 常用路径快捷选择
- 📊 实时推送进度显示

### 🎮 应用管理
- ▶️ 启动应用（支持包名 + Activity）
- ⏹️ 停止应用
- 🗑️ 清除应用数据
- 📋 查看应用详细信息

### 📂 文件管理
- 🗂️ 浏览设备文件系统
- ⬇️ 下载文件到本地
- ⬆️ 上传文件到设备
- 🗑️ 删除设备文件

### 📊 日志功能
- 📡 实时 Logcat 日志流（20K 条缓存）
- 🔍 关键词筛选
- 💾 导出设备日志
- 🗑️ 清空设备日志（带二次确认）
- 📜 操作历史记录
- 💾 标签选择记忆
- 🖥️ 全屏模式支持（字体自动放大）

## 🚀 快速开始

### 下载安装

#### 方式一：下载预编译版本（推荐）
1. 前往 [Releases](https://github.com/final00000000/ADBHub/releases/latest) 页面
2. 下载 `ADBHub-windows-x64-1.0.0.jar`
3. 确保已安装 Java 11 或更高版本
4. 双击运行或使用命令：
   ```bash
   java -jar ADBHub-windows-x64-1.0.0.jar
   ```

#### 方式二：从源码构建
```bash
# 克隆仓库
git clone https://github.com/final00000000/ADBHub.git
cd ADBHub

# 构建并运行
./gradlew desktop:run
```

### 环境要求

✅ **必需**：
- Java 11 或更高版本

✅ **可选**：
- ADB（Android SDK Platform Tools）
  - 应用会自动检测系统中的 ADB
  - 未检测到时会显示设置引导界面

### ADB 配置

**自动检测路径**（无需手动配置）：
- Windows: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`
- macOS: `~/Library/Android/sdk/platform-tools/adb`
- Linux: `~/Android/Sdk/platform-tools/adb`
- 系统 PATH 环境变量

**手动安装 ADB**：
- Windows: [下载 Platform Tools](https://developer.android.com/tools/releases/platform-tools)
- macOS: `brew install android-platform-tools`
- Linux: `sudo apt install adb`

**首次运行配置**：
1. 应用会自动显示设置引导
2. 选择检测到的 ADB 路径或手动浏览
3. 点击"测试连接"验证
4. 保存后自动开始检测设备

## 📖 使用指南

### 1. 连接设备
1. USB 连接 Android 设备/车机
2. 确保设备已开启 USB 调试
3. 在应用中点击"刷新"
4. 选择目标设备

**常见问题**：
- **unauthorized**: 在设备上允许 USB 调试授权
- **offline**: 重新插拔 USB 或重启 ADB Server
- **未显示设备**: 检查 USB 调试和驱动

### 2. Push APK
1. 切换到"Push APK"标签
2. 选择本地 APK 文件
3. 选择目标路径（或使用快捷路径）
4. 点击"推送到设备"

### 3. 设备操作
1. 切换到"设备操作命令"标签
2. 根据需要执行命令：
   - Root / Remount（需要设备支持）
   - 回到桌面（适配车机）
   - 启用 dm-verity
   - 重启设备

### 4. 应用管理
1. 切换到"应用管理"标签
2. 输入包名（如 `com.android.settings`）
3. 执行启动、停止、清除数据等操作

### 5. 文件管理
1. 切换到"文件管理"标签
2. 浏览设备文件系统
3. 上传/下载/删除文件

### 6. 查看日志
1. 在日志面板输入筛选关键词（可选）
2. 点击"开始"启动日志流
3. 实时查看过滤后的日志
4. 支持导出、清空、全屏查看

## 🏗️ 项目架构

```
ADBHub/
├── common/                    # 共享业务逻辑（Kotlin JVM）
│   ├── model/                 # 数据模型
│   │   ├── Device.kt
│   │   ├── AdbResult.kt
│   │   └── FileInfo.kt
│   ├── adb/                   # ADB 操作
│   │   ├── AdbManager.kt      # 接口定义
│   │   └── DadbManager.kt     # Dadb 实现
│   └── config/                # 配置管理
│       └── AdbConfig.kt
│
├── desktop/                   # 桌面应用（Compose Desktop）
│   ├── ui/                    # UI 组件
│   │   ├── MainScreen.kt      # 主界面（三栏布局）
│   │   ├── DeviceListPanel.kt # 设备列表
│   │   ├── OperationPanel.kt  # 操作面板
│   │   ├── LogPanel.kt        # 日志面板
│   │   └── FileManagerPanel.kt# 文件管理
│   ├── viewmodel/             # 状态管理
│   │   └── MainViewModel.kt
│   └── Main.kt                # 应用入口
│
└── app/                       # Android 应用（保留）
```

## 🛠️ 技术栈

- **语言**: Kotlin 2.1.0
- **UI 框架**: Compose Multiplatform 1.7.1
- **构建工具**: Gradle 9.4.1
- **ADB 库**: Dadb 1.2.9
- **异步处理**: Kotlin Coroutines + Flow

## 📦 开发构建

### 构建项目
```bash
# 构建所有模块
./gradlew build

# 仅构建桌面应用
./gradlew desktop:build
```

### 运行开发版本
```bash
./gradlew desktop:run
```

### 打包分发
```bash
# 打包为可执行 JAR
./gradlew desktop:packageUberJarForCurrentOS

# 生成位置：desktop/build/compose/jars/

# 打包为原生安装包（需要配置图标）
./gradlew desktop:packageDistributionForCurrentOS
```

## 🎯 路线图

- [ ] 日志语法高亮
- [ ] 多设备并发操作
- [ ] Shell 命令执行
- [ ] 自定义 ADB 命令
- [ ] 性能监控面板

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 🔗 相关链接

- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Dadb](https://github.com/mobile-dev-inc/dadb)
- [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools)

---

<div align="center">

**如果觉得有用，请给个 ⭐ Star！**

Made with ❤️ using Compose Multiplatform

</div>
