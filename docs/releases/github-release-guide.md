## GitHub Release 创建指南

### 自动化脚本（推荐）

访问以下链接直接创建 Release：

https://github.com/final00000000/ADBHub/releases/new?tag=v1.0&title=ADBHub%20v1.0%20%E6%AD%A3%E5%BC%8F%E7%89%88

### 手动步骤

1. **访问 Release 页面**
   ```
   https://github.com/final00000000/ADBHub/releases/new
   ```

2. **填写表单**
   - **Choose a tag**: 选择 `v1.0`（已存在）
   - **Release title**: `ADBHub v1.0 正式版`
   - **Describe this release**: 复制下面的内容

3. **发布说明**（复制以下内容）：

---

# ADBHub v1.0 正式版

## 🎉 主要功能

### 📱 设备管理
- 自动检测 ADB 设备
- 设备信息展示
- 多设备切换

### 🔧 设备操作命令
- Root 权限获取
- 重新挂载分区
- 回到桌面（双重保障，适配车机）
- 启用 dm-verity
- 设备重启（普通/Recovery/Bootloader）

### 📦 APK 推送
- 可视化 APK 文件选择
- 自定义目标路径
- 常用路径快捷选择
- 推送进度显示

### 📱 应用管理
- 启动应用
- 停止应用
- 清除应用数据
- 查看应用信息

### 📂 文件管理
- 浏览设备文件系统
- 下载文件到本地
- 上传文件到设备
- 删除设备文件

### 📊 日志功能
- 实时 Logcat 日志流（20K 条缓存）
- 日志筛选
- 导出设备日志
- 清空设备日志（二次确认）
- 操作历史记录
- 标签选择记忆
- 全屏模式支持

## ✨ 本次更新

- ✅ 添加标签选择记忆功能（默认设备日志）
- ✅ 优化按钮布局使用 FlowRow 自适应
- ✅ 添加全屏模式支持（字体放大）
- ✅ 实现清空设备日志二次确认对话框
- ✅ 新增设备操作命令：回到桌面（双重保障）、启用 dm-verity
- ✅ 修复 Push APK 双重日志问题
- ✅ 日志缓存从 5K 提升到 20K 条
- ✅ 批量更新优化减少 90% UI 刷新
- ✅ 完整国际化支持

## 🚀 使用方法

### 前置要求
- Java 11 或更高版本
- ADB 工具（可选，应用会自动检测系统中的 ADB）

### 运行应用
```bash
java -jar ADBHub-windows-x64-1.0.0.jar
```

## 📦 下载
- **Windows x64**: `ADBHub-windows-x64-1.0.0.jar` (约 74 MB)

## 📝 技术栈
- Kotlin
- Compose Multiplatform
- Dadb (Pure Kotlin ADB Client)
- Coroutines + Flow

## 🔗 项目地址
https://github.com/final00000000/ADBHub

---

4. **上传文件**
   - 拖拽或选择文件：`desktop\build\compose\jars\ADBHub-windows-x64-1.0.0.jar`

5. **发布**
   - 确认信息无误
   - 点击 **Publish release**

### 文件位置

上传文件位于：
```
E:\myProject\ADBHub\desktop\build\compose\jars\ADBHub-windows-x64-1.0.0.jar
```

文件大小：约 74 MB (77,575,380 bytes)

### 快速链接

- 创建 Release: https://github.com/final00000000/ADBHub/releases/new
- 仓库首页: https://github.com/final00000000/ADBHub
- 已有 Releases: https://github.com/final00000000/ADBHub/releases
