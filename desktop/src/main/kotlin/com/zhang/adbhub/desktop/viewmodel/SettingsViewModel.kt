package com.zhang.adbhub.desktop.viewmodel

import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.config.AdbPathDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SettingsViewModel {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _customAdbPath = MutableStateFlow("")
    val customAdbPath: StateFlow<String> = _customAdbPath.asStateFlow()

    private val _deviceLogPath = MutableStateFlow("")
    val deviceLogPath: StateFlow<String> = _deviceLogPath.asStateFlow()

    private val _adbVersion = MutableStateFlow<String?>(null)
    val adbVersion: StateFlow<String?> = _adbVersion.asStateFlow()

    private val _detectedPaths = MutableStateFlow<List<String>>(emptyList())
    val detectedPaths: StateFlow<List<String>> = _detectedPaths.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isAdbAvailable = MutableStateFlow(false)
    val isAdbAvailable: StateFlow<Boolean> = _isAdbAvailable.asStateFlow()

    init {
        loadConfig()
        detectPaths()
        checkCurrentAdb()
    }

    private fun loadConfig() {
        val config = AdbConfig.load()
        _customAdbPath.value = config.customAdbPath ?: ""
        _deviceLogPath.value = config.deviceLogPath ?: ""
    }

    private fun detectPaths() {
        scope.launch(Dispatchers.IO) {
            val paths = AdbPathDetector.detectPossiblePaths()
            val validPaths = paths.filter { path ->
                try {
                    AdbPathDetector.isAdbAvailable(path)
                } catch (e: Exception) {
                    false
                }
            }
            _detectedPaths.value = validPaths
        }
    }

    private fun checkCurrentAdb() {
        scope.launch(Dispatchers.IO) {
            val config = AdbConfig.load()
            val adbPath = AdbPathDetector.getValidAdbPath(config.customAdbPath)
            if (adbPath != null) {
                _isAdbAvailable.value = true
                _adbVersion.value = AdbPathDetector.getAdbVersion(adbPath)
            } else {
                _isAdbAvailable.value = false
                _adbVersion.value = null
            }
        }
    }

    fun setCustomPath(path: String) {
        _customAdbPath.value = path
    }

    fun setDeviceLogPath(path: String) {
        _deviceLogPath.value = path
    }

    fun saveConfig() {
        val config = AdbConfig(
            customAdbPath = _customAdbPath.value.takeIf { it.isNotBlank() },
            deviceLogPath = _deviceLogPath.value.takeIf { it.isNotBlank() }
        )
        AdbConfig.save(config)
        checkCurrentAdb()
        _statusMessage.value = "配置已保存"
    }

    fun testConnection() {
        scope.launch(Dispatchers.IO) {
            val path = _customAdbPath.value.ifBlank { null }
            val validPath = AdbPathDetector.getValidAdbPath(path)

            if (validPath != null) {
                val version = AdbPathDetector.getAdbVersion(validPath)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "连接成功！\n版本: $version"
                    _isAdbAvailable.value = true
                    _adbVersion.value = version
                }
            } else {
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "连接失败：无法找到有效的 ADB 工具"
                    _isAdbAvailable.value = false
                    _adbVersion.value = null
                }
            }
        }
    }

    fun resetToDefault() {
        _customAdbPath.value = ""
        AdbConfig.save(AdbConfig())
        checkCurrentAdb()
        _statusMessage.value = "已重置为默认配置"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun cleanup() {
        scope.cancel()
    }
}
