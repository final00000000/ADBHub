# 更新日志

所有重要的项目变更都会记录在此文件中。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [1.1.0] - 2026-06-12

### ✨ 新增

- **设备诊断功能**：为空设备状态添加完整的诊断流程
  - ADB 路径检测
  - ADB 命令查询
  - 设备连接状态检查
  - 设备授权检查
  - 离线设备检测
  - 未知设备检测
- **统一设备必需提示**：在 Push APK、设备命令、应用管理、文件管理和日志视图中统一提示
- **Windows 原生安装包**：提供 MSI 和 EXE 安装程序

### 🔧 改进

- 重构设备操作命令页面，按功能分组：
  - 车机快捷操作
  - 诊断工具
  - 维护操作
  - 重启选项
- 添加通用 ADB 设备命令执行器，支持操作日志记录
- 在设备连接前隐藏命令操作控件

### 📦 发布资源

- `ADBHub-1.1.0.msi` (Windows MSI 安装包)
  - SHA256: `338DED15597A41B674C47D3433F594EF51608C28BA95F8512FF5F1FA80E1E31D`
- `ADBHub-1.1.0.exe` (Windows EXE 安装包)
  - SHA256: `3443154423126E7932A9B015BD22F54851CA6B344D5B85D8A7F98A78EFED9733`

详细发布说明：[docs/releases/v1.1.md](docs/releases/v1.1.md)

---

## [1.0.0] - 2026-06-11

### 🎉 首次发布

#### 核心功能

##### 📱 设备管理
- 自动检测 ADB 设备
- 设备信息展示
- 多设备切换

##### 🔧 设备操作命令
- Root 权限获取
- 重新挂载分区
- 回到桌面（双重保障，适配车机）
- 启用 dm-verity
- 设备重启（普通/Recovery/Bootloader）

##### 📦 APK 推送
- 可视化 APK 文件选择
- 自定义目标路径
- 常用路径快捷选择
- 推送进度显示

##### 📱 应用管理
- 启动应用
- 停止应用
- 清除应用数据
- 查看应用信息

##### 📂 文件管理
- 浏览设备文件系统
- 下载文件到本地
- 上传文件到设备
- 删除设备文件

##### 📊 日志功能
- 实时 Logcat 日志流（20K 条缓存）
- 日志筛选
- 导出设备日志
- 清空设备日志（二次确认）
- 操作历史记录
- 标签选择记忆
- 全屏模式支持

### ✨ 特性亮点

- ✅ 标签选择记忆功能（默认设备日志）
- ✅ 按钮布局使用 FlowRow 自适应
- ✅ 全屏模式支持（字体放大）
- ✅ 清空设备日志二次确认对话框
- ✅ 新增设备操作命令：回到桌面（双重保障）、启用 dm-verity
- ✅ 修复 Push APK 双重日志问题
- ✅ 日志缓存从 5K 提升到 20K 条
- ✅ 批量更新优化减少 90% UI 刷新
- ✅ 完整国际化支持

### 📦 发布资源

- `ADBHub-windows-x64-1.0.0.jar` (约 74 MB)

### 📝 技术栈

- Kotlin
- Compose Multiplatform
- Dadb (Pure Kotlin ADB Client)
- Coroutines + Flow

详细发布说明：[docs/releases/v1.0.md](docs/releases/v1.0.md)

---

## 链接

- [项目仓库](https://github.com/final00000000/ADBHub)
- [发布页面](https://github.com/final00000000/ADBHub/releases)
- [问题反馈](https://github.com/final00000000/ADBHub/issues)
