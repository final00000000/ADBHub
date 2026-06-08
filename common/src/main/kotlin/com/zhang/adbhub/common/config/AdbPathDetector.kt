package com.zhang.adbhub.common.config

import java.io.File

/**
 * ADB 路径检测器
 */
object AdbPathDetector {
    /**
     * 检测 ADB 是否可用
     */
    fun isAdbAvailable(adbPath: String): Boolean {
        return try {
            val process = ProcessBuilder(adbPath, "version").start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取 ADB 版本信息
     */
    fun getAdbVersion(adbPath: String): String? {
        return try {
            val process = ProcessBuilder(adbPath, "version").start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            output.lines().firstOrNull()?.trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检测可能的 ADB 路径（按优先级排序）
     */
    fun detectPossiblePaths(): List<String> {
        val paths = mutableListOf<String>()

        // 1. 系统 PATH 中的 adb
        paths.add("adb")

        // 2. Windows 常见路径
        if (isWindows()) {
            val localAppData = System.getenv("LOCALAPPDATA")
            if (localAppData != null) {
                paths.add("$localAppData\\Android\\Sdk\\platform-tools\\adb.exe")
            }
            paths.add("C:\\Android\\Sdk\\platform-tools\\adb.exe")
            paths.add("E:\\AndroidSDK\\platform-tools\\adb.exe")
            paths.add("D:\\Android\\Sdk\\platform-tools\\adb.exe")
        }

        // 3. macOS 常见路径
        if (isMac()) {
            val userHome = System.getProperty("user.home")
            paths.add("$userHome/Library/Android/sdk/platform-tools/adb")
            paths.add("/usr/local/bin/adb")
        }

        // 4. Linux 常见路径
        if (isLinux()) {
            val userHome = System.getProperty("user.home")
            paths.add("$userHome/Android/Sdk/platform-tools/adb")
            paths.add("/usr/bin/adb")
            paths.add("/usr/local/bin/adb")
        }

        return paths
    }

    /**
     * 自动检测可用的 ADB 路径
     */
    fun autoDetect(): String? {
        return detectPossiblePaths().firstOrNull { path ->
            // 对于系统命令（如 "adb"），直接测试可用性
            // 对于完整路径，先检查文件是否存在
            if (path == "adb") {
                isAdbAvailable(path)
            } else {
                File(path).exists() && isAdbAvailable(path)
            }
        }
    }

    /**
     * 获取有效的 ADB 路径（优先级：自定义 > 自动检测 > 系统 PATH）
     */
    fun getValidAdbPath(customPath: String?): String? {
        // 1. 尝试用户自定义路径
        if (!customPath.isNullOrBlank()) {
            if (File(customPath).exists() && isAdbAvailable(customPath)) {
                return customPath
            }
        }

        // 2. 自动检测
        return autoDetect()
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }

    private fun isMac(): Boolean {
        val osName = System.getProperty("os.name").lowercase()
        return osName.contains("mac") || osName.contains("darwin")
    }

    private fun isLinux(): Boolean {
        return System.getProperty("os.name").lowercase().contains("linux")
    }
}
