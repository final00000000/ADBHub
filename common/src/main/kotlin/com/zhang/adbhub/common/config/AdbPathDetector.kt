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
     * 扫描 Windows 盘符查找 ADB。调用方必须在用户明确同意后再调用。
     */
    fun detectPossiblePathCandidates(): List<AdbPathCandidate> {
        return if (isWindows()) searchWindowsDrives() else emptyList()
    }

    /**
     * 扫描 Windows 盘符查找 ADB。调用方必须在用户明确同意后再调用。
     */
    fun detectPossiblePaths(): List<String> {
        return detectPossiblePathCandidates().map { it.path }
    }

    /**
     * 获取有效的 ADB 路径。只验证用户配置的路径，不做自动兜底。
     */
    fun getValidAdbPath(customPath: String?): String? {
        val path = customPath?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        if (isConfiguredPathRunnable(path) && isAdbAvailable(path)) {
            return path
        }
        return null
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }

    /**
     * 搜索 Windows 所有盘符下的 adb.exe
     */
    private fun searchWindowsDrives(): List<AdbPathCandidate> {
        val candidates = linkedMapOf<String, AdbPathCandidate>()

        for (root in File.listRoots()) {
            root.walkTopDown()
                .onEnter { directory -> shouldEnterDirectory(directory) }
                .onFail { _, _ -> }
                .filter { file -> file.isFile && file.name.equals("adb.exe", ignoreCase = true) }
                .forEach { file ->
                    val absolutePath = file.absolutePath
                    candidates.putIfAbsent(
                        absolutePath.lowercase(),
                        AdbPathCandidate(
                            path = absolutePath,
                            displayPath = absolutePath
                        )
                    )
                }
        }

        return candidates.values.toList()
    }

    private fun isConfiguredPathRunnable(path: String): Boolean {
        val file = File(path)
        if (file.exists() && file.isFile) {
            return true
        }

        return !path.contains('/') &&
            !path.contains('\\') &&
            (path.equals("adb", ignoreCase = true) || path.equals("adb.exe", ignoreCase = true))
    }

    private fun shouldEnterDirectory(directory: File): Boolean {
        val skippedDirectoryNames = setOf(
            "windows",
            "system volume information",
            "\$recycle.bin"
        )
        return directory.name.lowercase() !in skippedDirectoryNames
    }
}
