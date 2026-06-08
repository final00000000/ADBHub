package com.zhang.adbhub.desktop.viewmodel

import com.zhang.adbhub.common.adb.AdbManager
import com.zhang.adbhub.common.adb.DadbManager
import com.zhang.adbhub.common.model.AdbResult
import com.zhang.adbhub.common.model.Device
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class MainViewModel {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val adbManager: AdbManager = DadbManager()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _selectedTab = MutableStateFlow(OperationTab.PUSH_APK)
    val selectedTab: StateFlow<OperationTab> = _selectedTab.asStateFlow()

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    private val _isLogcatRunning = MutableStateFlow(false)
    val isLogcatRunning: StateFlow<Boolean> = _isLogcatRunning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _adbStatus = MutableStateFlow<String?>(null)
    val adbStatus: StateFlow<String?> = _adbStatus.asStateFlow()

    private var logcatJob: Job? = null

    fun refreshDevices() {
        scope.launch {
            // 检查 ADB 状态
            val config = com.zhang.adbhub.common.config.AdbConfig.load()
            val adbPath = com.zhang.adbhub.common.config.AdbPathDetector.getValidAdbPath(config.customAdbPath)

            if (adbPath == null) {
                _adbStatus.value = "ADB 未找到"
                _devices.value = emptyList()
                return@launch
            } else {
                _adbStatus.value = "ADB 已找到: $adbPath"
            }

            when (val result = adbManager.getDevices()) {
                is AdbResult.Success -> {
                    _devices.value = result.data
                    if (_selectedDevice.value == null && result.data.isNotEmpty()) {
                        _selectedDevice.value = result.data.first()
                    }
                    if (result.data.isEmpty()) {
                        _statusMessage.value = "ADB 正常，但未检测到设备。请检查设备连接和 USB 调试。"
                    } else {
                        _statusMessage.value = null
                    }
                }
                is AdbResult.Error -> {
                    _statusMessage.value = "获取设备失败: ${result.message}"
                }
            }
        }
    }

    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        stopLogcat()
        _logLines.value = emptyList()
    }

    fun selectTab(tab: OperationTab) {
        _selectedTab.value = tab
    }

    fun pushApk(apkFile: File, onProgress: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            onProgress("正在推送 APK...")
            when (val result = adbManager.pushApk(device, apkFile)) {
                is AdbResult.Success -> {
                    onProgress("APK 推送成功")
                    _statusMessage.value = "APK 推送成功"
                }
                is AdbResult.Error -> {
                    onProgress("推送失败: ${result.message}")
                    _statusMessage.value = "推送失败: ${result.message}"
                }
            }
        }
    }

    fun startLogcat(filter: String = "") {
        val device = _selectedDevice.value ?: return
        stopLogcat()

        _isLogcatRunning.value = true
        _logLines.value = emptyList()

        logcatJob = scope.launch {
            adbManager.getLogcatFlow(device)
                .filter { line ->
                    filter.isEmpty() || line.contains(filter, ignoreCase = true)
                }
                .collect { line ->
                    _logLines.value = (_logLines.value + line).takeLast(5000)
                }
        }
    }

    fun stopLogcat() {
        val device = _selectedDevice.value ?: return
        logcatJob?.cancel()
        adbManager.stopLogcat(device)
        _isLogcatRunning.value = false
    }

    fun exportLogs(outputFile: File, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.exportLogs(device, outputFile)) {
                is AdbResult.Success -> onResult("日志导出成功: ${outputFile.absolutePath}")
                is AdbResult.Error -> onResult("导出失败: ${result.message}")
            }
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun executeRoot(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.executeRoot(device)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun executeRemount(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.executeRemount(device)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun rebootDevice(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.rebootDevice(device)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun rebootRecovery(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.rebootRecovery(device)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun rebootBootloader(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.rebootBootloader(device)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun startApp(packageName: String, activityName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.startApp(device, packageName, activityName)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun getAppInfo(packageName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.getAppInfo(device, packageName)) {
                is AdbResult.Success -> onResult(result.data)
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun stopApp(packageName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.stopApp(device, packageName)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun clearAppData(packageName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            when (val result = adbManager.clearAppData(device, packageName)) {
                is AdbResult.Success -> onResult("成功: ${result.data}")
                is AdbResult.Error -> onResult("失败: ${result.message}")
            }
        }
    }

    fun cleanup() {
        stopLogcat()
        scope.cancel()
    }
}

enum class OperationTab {
    PUSH_APK,
    DEVICE_COMMANDS,
    APP_MANAGEMENT,
    LOGS
}
