package com.zhang.adbhub.common.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime

/**
 * ADB 配置数据类
 */
@Serializable
data class AdbConfig(
    val customAdbPath: String? = null,
    val lastUsedAdbPath: String? = null,
    val deviceLogPath: String? = null,
    val pushTargetPath: String? = null,
    val favoriteCommandIds: List<String> = emptyList(),
    val lastLogTab: Int = 1  // 0=操作历史, 1=设备日志
) {
    companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        private val configFile: File
            get() {
                val userHome = System.getProperty("user.home")
                return File(userHome, ".adbhub-config.json")
            }

        // Cache for config to avoid repeated file reads (synchronized for thread safety)
        @Volatile
        private var cachedConfig: AdbConfig? = null
        @Volatile
        private var cachedConfigTimestamp: FileTime? = null
        private val cacheLock = Any()

        /**
         * 加载配置（带缓存优化）
         */
        fun load(): AdbConfig {
            return try {
                if (!configFile.exists()) {
                    return AdbConfig().also {
                        synchronized(cacheLock) {
                            cachedConfig = it
                            cachedConfigTimestamp = null
                        }
                    }
                }

                val currentTimestamp = Files.getLastModifiedTime(configFile.toPath())

                // Return cached config if file hasn't changed (double-check locking)
                val cached = cachedConfig
                if (cached != null && cachedConfigTimestamp == currentTimestamp) {
                    return cached
                }

                // Acquire lock for cache update
                synchronized(cacheLock) {
                    // Double-check after acquiring lock
                    if (cachedConfig != null && cachedConfigTimestamp == currentTimestamp) {
                        return cachedConfig!!
                    }

                    // Read and cache new config
                    val content = configFile.readText()
                    val config = json.decodeFromString<AdbConfig>(content)
                    cachedConfig = config
                    cachedConfigTimestamp = currentTimestamp
                    config
                }
            } catch (e: Exception) {
                synchronized(cacheLock) {
                    cachedConfig = null
                    cachedConfigTimestamp = null
                }
                AdbConfig()
            }
        }

        /**
         * 保存配置
         */
        fun save(config: AdbConfig) {
            try {
                val content = json.encodeToString(config)
                configFile.writeText(content)
                // Update cache
                val savedTimestamp = Files.getLastModifiedTime(configFile.toPath())
                synchronized(cacheLock) {
                    cachedConfig = config
                    cachedConfigTimestamp = savedTimestamp
                }
            } catch (e: Exception) {
                // Silently fail - config save is not critical
            }
        }

        /**
         * 清除缓存（用于测试或强制重载）
         */
        fun clearCache() {
            synchronized(cacheLock) {
                cachedConfig = null
                cachedConfigTimestamp = null
            }
        }
    }
}
