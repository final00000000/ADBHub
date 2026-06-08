package com.zhang.adbhub.common.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

/**
 * ADB 配置数据类
 */
@Serializable
data class AdbConfig(
    val customAdbPath: String? = null,
    val lastUsedAdbPath: String? = null
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

        /**
         * 加载配置
         */
        fun load(): AdbConfig {
            return try {
                if (configFile.exists()) {
                    val content = configFile.readText()
                    json.decodeFromString<AdbConfig>(content)
                } else {
                    AdbConfig()
                }
            } catch (e: Exception) {
                println("Failed to load config: ${e.message}")
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
            } catch (e: Exception) {
                println("Failed to save config: ${e.message}")
            }
        }
    }
}
