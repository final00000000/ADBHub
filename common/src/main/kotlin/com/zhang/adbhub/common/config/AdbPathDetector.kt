package com.zhang.adbhub.common.config

import java.io.File

data class AdbPathCandidate(
    val path: String,
    val displayPath: String
)

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
    fun detectPossiblePathCandidates(): List<AdbPathCandidate> {
        val candidates = mutableListOf<AdbPathCandidate>()

        // 1. 系统 PATH 中的 adb
        candidates.add(
            AdbPathCandidate(
                path = "adb",
                displayPath = "系统 PATH: adb"
            )
        )

        // 2. Windows 常见路径
        if (isWindows()) {
            val localAppData = System.getenv("LOCALAPPDATA")
            if (localAppData != null) {
                candidates.add(
                    AdbPathCandidate(
                        path = "$localAppData\\Android\\Sdk\\platform-tools\\adb.exe",
                        displayPath = "%LOCALAPPDATA%\\Android\\Sdk\\platform-tools\\adb.exe"
                    )
                )
            }

            // 自动搜索所有盘符
            candidates.addAll(searchWindowsDrives())
        }

        // 3. macOS 常见路径
        if (isMac()) {
            val userHome = System.getProperty("user.home")
            candidates.add(
                AdbPathCandidate(
                    path = "$userHome/Library/Android/sdk/platform-tools/adb",
                    displayPath = "~/Library/Android/sdk/platform-tools/adb"
                )
            )
            candidates.add(AdbPathCandidate("/usr/local/bin/adb", "/usr/local/bin/adb"))
        }

        // 4. Linux 常见路径
        if (isLinux()) {
            val userHome = System.getProperty("user.home")
            candidates.add(
                AdbPathCandidate(
                    path = "$userHome/Android/Sdk/platform-tools/adb",
                    displayPath = "~/Android/Sdk/platform-tools/adb"
                )
            )
            candidates.add(AdbPathCandidate("/usr/bin/adb", "/usr/bin/adb"))
            candidates.add(AdbPathCandidate("/usr/local/bin/adb", "/usr/local/bin/adb"))
        }

        return candidates
    }

    /**
     * 检测可能的 ADB 路径（按优先级排序）
     */
    fun detectPossiblePaths(): List<String> {
        return detectPossiblePathCandidates().map { it.path }
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
            val file = File(customPath)
            if (file.exists() && isAdbAvailable(customPath)) {
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

    /**
     * 搜索 Windows 所有盘符下的 Android SDK
     */
    private fun searchWindowsDrives(): List<AdbPathCandidate> {
        val candidates = mutableListOf<AdbPathCandidate>()
        val roots = File.listRoots()

        for (root in roots) {
            val paths = listOf(
                "Android\\Sdk\\platform-tools\\adb.exe",
                "AndroidSDK\\platform-tools\\adb.exe",
                "SDK\\Android\\platform-tools\\adb.exe"
            )

            for (path in paths) {
                val fullPath = File(root, path)
                if (fullPath.exists()) {
                    candidates.add(
                        AdbPathCandidate(
                            path = fullPath.absolutePath,
                            displayPath = fullPath.absolutePath
                        )
                    )
                }
            }
        }

        return candidates
    }
}
