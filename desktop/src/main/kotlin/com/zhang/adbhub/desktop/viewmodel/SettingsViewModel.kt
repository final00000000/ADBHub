package com.zhang.adbhub.desktop.viewmodel

import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.config.AdbPathDetector
import com.zhang.adbhub.desktop.utils.StringResources
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

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        loadConfig()
        checkCurrentAdb()
    }

    private fun loadConfig() {
        val config = AdbConfig.load()
        _customAdbPath.value = config.customAdbPath ?: ""
        _deviceLogPath.value = config.deviceLogPath ?: ""
    }

    fun scanAllDrives() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isScanning.value = true
                _detectedPaths.value = emptyList()
                _statusMessage.value = StringResources.get("status.scanning.drives")
            }
            try {
                AdbPathDetector.clearCache()
                val paths = AdbPathDetector.detectPossiblePaths()
                val validPaths = paths.filter { path ->
                    try {
                        AdbPathDetector.isAdbAvailable(path)
                    } catch (e: Exception) {
                        false
                    }
                }
                withContext(Dispatchers.Main) {
                    _detectedPaths.value = validPaths
                    _statusMessage.value = if (validPaths.isEmpty()) {
                        StringResources.get("status.no.adb.found")
                    } else {
                        StringResources.get("status.found.paths", validPaths.size)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isScanning.value = false
                }
            }
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
        val config = buildConfigFromInputs()
        AdbConfig.save(config)
        AdbPathDetector.clearCache()
        checkCurrentAdb()
        _statusMessage.value = StringResources.get("status.config.saved")
    }

    fun saveConfigIfAdbValid(onValid: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            val config = buildConfigFromInputs()
            AdbPathDetector.clearCache()
            val validPath = AdbPathDetector.getValidAdbPath(config.customAdbPath)

            if (validPath == null) {
                withContext(Dispatchers.Main) {
                    _statusMessage.value = StringResources.get("status.connection.failed")
                    _isAdbAvailable.value = false
                    _adbVersion.value = null
                }
                return@launch
            }

            val savedConfig = config.copy(customAdbPath = validPath)
            AdbConfig.save(savedConfig)
            val version = AdbPathDetector.getAdbVersion(validPath)

            withContext(Dispatchers.Main) {
                _customAdbPath.value = validPath
                _statusMessage.value = StringResources.get("status.config.saved")
                _isAdbAvailable.value = true
                _adbVersion.value = version
                onValid()
            }
        }
    }

    fun testConnection() {
        scope.launch(Dispatchers.IO) {
            val path = _customAdbPath.value.ifBlank { null }
            AdbPathDetector.clearCache()
            val validPath = AdbPathDetector.getValidAdbPath(path)

            if (validPath != null) {
                val version = AdbPathDetector.getAdbVersion(validPath)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = StringResources.get("status.connection.success", version ?: "")
                    _isAdbAvailable.value = true
                    _adbVersion.value = version
                }
            } else {
                withContext(Dispatchers.Main) {
                    _statusMessage.value = StringResources.get("status.connection.failed")
                    _isAdbAvailable.value = false
                    _adbVersion.value = null
                }
            }
        }
    }

    fun resetToDefault() {
        _customAdbPath.value = ""
        AdbConfig.save(AdbConfig())
        AdbPathDetector.clearCache()
        checkCurrentAdb()
        _statusMessage.value = StringResources.get("status.reset.success")
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun cleanup() {
        scope.cancel()
    }

    private fun buildConfigFromInputs(): AdbConfig {
        return AdbConfig.load().copy(
            customAdbPath = _customAdbPath.value.trim().takeIf { it.isNotBlank() },
            deviceLogPath = _deviceLogPath.value.trim().takeIf { it.isNotBlank() }
        )
    }
}
