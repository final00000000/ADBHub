package com.zhang.adbhub.common.config

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.DosFileAttributes
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

data class AdbPathCandidate(
    val path: String,
    val displayPath: String
)

/**
 * ADB 路径检测器
 */
object AdbPathDetector {
    private const val VALIDATION_CACHE_TTL_MS = 30_000L
    private const val ADB_COMMAND_TIMEOUT_SECONDS = 5L

    private data class PathFingerprint(
        val exists: Boolean,
        val isFile: Boolean,
        val lastModified: Long,
        val length: Long
    )

    private data class ValidationCacheEntry(
        val isValid: Boolean,
        val checkedAtMs: Long,
        val fingerprint: PathFingerprint
    )

    private val pathValidationCache = ConcurrentHashMap<String, ValidationCacheEntry>()

    /**
     * 检测 ADB 是否可用
     */
    fun isAdbAvailable(adbPath: String): Boolean {
        val path = adbPath.trim()
        if (path.isEmpty()) return false

        val now = System.currentTimeMillis()
        val fingerprint = pathFingerprint(path)
        val key = cacheKey(path)
        pathValidationCache[key]?.let { cached ->
            if (now - cached.checkedAtMs < VALIDATION_CACHE_TTL_MS && cached.fingerprint == fingerprint) {
                return cached.isValid
            }
        }

        val isValid = try {
            val process = ProcessBuilder(path, "version")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(ADB_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                false
            } else {
                process.exitValue() == 0
            }
        } catch (e: Exception) {
            false
        }

        pathValidationCache[key] = ValidationCacheEntry(isValid, now, fingerprint)
        return isValid
    }

    /**
     * 获取 ADB 版本信息
     */
    fun getAdbVersion(adbPath: String): String? {
        return try {
            val process = ProcessBuilder(adbPath.trim(), "version")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(ADB_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
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

    /**
     * 清除路径验证缓存（用于测试或强制重新验证）
     */
    fun clearCache() {
        pathValidationCache.clear()
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }

    /**
     * 搜索 Windows 当前可见盘符下的 adb.exe。
     */
    private fun searchWindowsDrives(): List<AdbPathCandidate> {
        val candidates = linkedMapOf<String, AdbPathCandidate>()

        for (root in File.listRoots()) {
            root.walkTopDown()
                .onEnter { directory -> shouldEnterDirectory(directory) }
                .onFail { _, _ -> }
                .filter { file -> file.isFile && file.name.equals("adb.exe", ignoreCase = true) }
                .forEach { file -> addCandidate(candidates, file) }
        }

        return candidates.values.toList()
    }

    private fun addCandidate(candidates: MutableMap<String, AdbPathCandidate>, file: File) {
        val absolutePath = file.absolutePath
        candidates.putIfAbsent(
            absolutePath.lowercase(),
            AdbPathCandidate(
                path = absolutePath,
                displayPath = absolutePath
            )
        )
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

    private fun cacheKey(path: String): String {
        return if (isWindows()) path.lowercase() else path
    }

    private fun pathFingerprint(path: String): PathFingerprint {
        val file = File(path)
        if (!file.exists()) {
            return PathFingerprint(exists = false, isFile = false, lastModified = 0L, length = 0L)
        }
        return PathFingerprint(
            exists = true,
            isFile = file.isFile,
            lastModified = file.lastModified(),
            length = if (file.isFile) file.length() else 0L
        )
    }

    private fun shouldEnterDirectory(directory: File): Boolean {
        if (!directory.canRead()) {
            return false
        }

        return try {
            val attributes = Files.readAttributes(
                directory.toPath(),
                DosFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS
            )
            !attributes.isSystem
        } catch (e: Exception) {
            true
        }
    }
}
