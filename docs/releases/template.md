# ADBHub v{VERSION}

## Highlights

- 主要功能点 1
- 主要功能点 2
- 主要功能点 3

## 新增功能

- **功能名称**：功能描述

## 改进优化

- **改进点 1**：改进描述
- **改进点 2**：改进描述

## 问题修复

- **修复问题 1**：问题描述
- **修复问题 2**：问题描述

## 验证步骤

- `:desktop:compileKotlin`
- `:desktop:packageMsi`
- `:desktop:packageExe`
- `:desktop:packageDistributionForCurrentOS`

## 发布资源

- `ADBHub-{VERSION}.msi`
  - SHA256: `{SHA256_HASH}`
- `ADBHub-{VERSION}.exe`
  - SHA256: `{SHA256_HASH}`
- `ADBHub-windows-x64-{VERSION}.jar`
  - SHA256: `{SHA256_HASH}`

## 技术栈

- Kotlin
- Compose Multiplatform
- Dadb (Pure Kotlin ADB Client)
- Coroutines + Flow

## 使用方法

### 前置要求
- Java 11 或更高版本
- ADB 工具（可选，应用会自动检测系统中的 ADB）

### Windows 安装
- 下载并运行 `ADBHub-{VERSION}.msi` 或 `ADBHub-{VERSION}.exe`

### JAR 运行
```bash
java -jar ADBHub-windows-x64-{VERSION}.jar
```

## 项目地址
https://github.com/final00000000/ADBHub
