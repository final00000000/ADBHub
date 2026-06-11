package com.zhang.adbhub.desktop.viewmodel

import com.zhang.adbhub.common.adb.AdbManager
import com.zhang.adbhub.common.adb.DadbManager
import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.config.AdbPathDetector
import com.zhang.adbhub.common.model.AdbResult
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.DeviceState
import com.zhang.adbhub.common.model.FileInfo
import com.zhang.adbhub.desktop.utils.StringResources
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class MainViewModel {
    private companion object {
        const val DEVICE_MONITOR_INTERVAL_MS = 2_000L
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val adbManager: AdbManager = DadbManager()
    private val deviceRefreshMutex = Mutex()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _selectedTab = MutableStateFlow(OperationTab.PUSH_APK)
    val selectedTab: StateFlow<OperationTab> = _selectedTab.asStateFlow()

    private val _rawLogLines = MutableStateFlow<List<String>>(emptyList())
    private val _logFilter = MutableStateFlow("")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()
    val logLines: StateFlow<List<String>> = combine(_rawLogLines, _logFilter) { lines, filter ->
        if (filter.isBlank()) {
            lines
        } else {
            lines.filter { line -> line.contains(filter, ignoreCase = true) }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _isLogcatRunning = MutableStateFlow(false)
    val isLogcatRunning: StateFlow<Boolean> = _isLogcatRunning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _adbStatus = MutableStateFlow<String?>(null)
    val adbStatus: StateFlow<String?> = _adbStatus.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _operationLogs = MutableStateFlow<List<OperationLog>>(emptyList())
    val operationLogs: StateFlow<List<OperationLog>> = _operationLogs.asStateFlow()

    private val _currentPath = MutableStateFlow("/sdcard/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _fileList = MutableStateFlow<List<FileInfo>>(emptyList())
    val fileList: StateFlow<List<FileInfo>> = _fileList.asStateFlow()

    private var logcatJob: Job? = null
    private var deviceMonitorJob: Job? = null

    init {
        startDeviceMonitor()
    }

    data class OperationLog(
        val timestamp: String,
        val operation: String,
        val command: String?,
        val output: String?,
        val success: Boolean
    )

    private fun addOperationLog(operation: String, command: String? = null, output: String? = null, success: Boolean) {
        val timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        val log = OperationLog(timestamp, operation, command, output, success)
        _operationLogs.value = (_operationLogs.value + log).takeLast(1000)
    }

    fun refreshDevices() {
        scope.launch {
            refreshDevicesOnce(silent = false)
        }
    }

    private fun startDeviceMonitor() {
        if (deviceMonitorJob != null) return

        deviceMonitorJob = scope.launch {
            while (isActive) {
                refreshDevicesOnce(silent = true)
                delay(DEVICE_MONITOR_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshDevicesOnce(silent: Boolean) {
        deviceRefreshMutex.withLock {
            val config = withContext(Dispatchers.IO) { AdbConfig.load() }
            val adbPath = withContext(Dispatchers.IO) { AdbPathDetector.getValidAdbPath(config.customAdbPath) }

            if (adbPath == null) {
                _adbStatus.value = StringResources.get("status.adb.not.found")
                applyDeviceSnapshot(emptyList(), silent)
                if (!silent) {
                    _statusMessage.value = StringResources.get("status.adb.not.found")
                }
                return
            }

            _adbStatus.value = StringResources.get("status.adb.found", adbPath)

            when (val result = adbManager.getDevices()) {
                is AdbResult.Success -> applyDeviceSnapshot(result.data, silent)
                is AdbResult.Error -> {
                    if (!silent) {
                        _statusMessage.value = StringResources.get("status.get.devices.failed", result.message)
                    }
                }
            }
        }
    }

    private fun applyDeviceSnapshot(latestDevices: List<Device>, silent: Boolean) {
        val previousSelectedDevice = _selectedDevice.value
        val onlineDevices = latestDevices.filter { it.state == DeviceState.ONLINE }
        val updatedSelectedDevice = previousSelectedDevice?.let { selected ->
            onlineDevices.firstOrNull { it.serialNumber == selected.serialNumber }
        }

        _devices.value = latestDevices

        when {
            previousSelectedDevice != null && updatedSelectedDevice == null -> {
                clearSelectedDevice(previousSelectedDevice)
                _statusMessage.value = StringResources.get("status.device.disconnected", previousSelectedDevice.serialNumber)
            }
            updatedSelectedDevice != null -> {
                _selectedDevice.value = updatedSelectedDevice
                if (!silent) {
                    _statusMessage.value = null
                }
            }
            onlineDevices.isNotEmpty() -> {
                _selectedDevice.value = onlineDevices.first()
                if (!silent) {
                    _statusMessage.value = null
                }
            }
            !silent -> {
                _statusMessage.value = StringResources.get("status.no.device.connected")
            }
        }
    }

    private fun clearSelectedDevice(device: Device?) {
        stopLogcatFor(device)
        _selectedDevice.value = null
        _rawLogLines.value = emptyList()
        _currentPath.value = "/sdcard/"
        _fileList.value = emptyList()
    }

    fun selectDevice(device: Device) {
        if (device.state != DeviceState.ONLINE) {
            _statusMessage.value = StringResources.get("status.device.not.online", device.serialNumber)
            return
        }

        val previousDevice = _selectedDevice.value
        if (previousDevice?.serialNumber == device.serialNumber) {
            return
        }

        stopLogcatFor(previousDevice)
        _selectedDevice.value = device
        _rawLogLines.value = emptyList()
        _currentPath.value = "/sdcard/"
        _fileList.value = emptyList()
    }

    fun selectTab(tab: OperationTab) {
        _selectedTab.value = tab
    }

    fun pushApk(apkFile: File, targetPath: String, onProgress: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} push ${apkFile.absolutePath} $targetPath"
            addOperationLog(StringResources.get("operation.push.apk"), command, null, false)

            try {
                onProgress(StringResources.get("operation.pushing"))
                when (val result = adbManager.pushApk(device, apkFile, targetPath)) {
                    is AdbResult.Success -> {
                        // Save the target path for next use
                        val config = com.zhang.adbhub.common.config.AdbConfig.load()
                        val newConfig = config.copy(pushTargetPath = targetPath)
                        com.zhang.adbhub.common.config.AdbConfig.save(newConfig)

                        addOperationLog(StringResources.get("operation.push.apk"), command, StringResources.get("operation.push.apk.success", targetPath), true)
                        onProgress("${StringResources.get("operation.push.success")} ✓")
                        _statusMessage.value = StringResources.get("operation.push.success")
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.push.apk"), command, result.message, false)
                        onProgress("${StringResources.get("operation.push.failed", result.message)} ✗")
                        _statusMessage.value = StringResources.get("operation.push.failed", result.message)
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun clearLogcat() {
        _rawLogLines.value = emptyList()
    }

    fun startLogcat(filter: String = _logFilter.value) {
        val device = _selectedDevice.value ?: return
        stopLogcatFor(device)

        _logFilter.value = filter
        _isLogcatRunning.value = true
        _rawLogLines.value = emptyList()

        val newJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                adbManager.getLogcatFlow(device)
                    .collect { line ->
                        _rawLogLines.value = (_rawLogLines.value + line).takeLast(5000)
                    }
            } finally {
                if (logcatJob === coroutineContext[Job]) {
                    _isLogcatRunning.value = false
                    logcatJob = null
                }
            }
        }
        logcatJob = newJob
        newJob.start()
    }

    fun stopLogcat() {
        stopLogcatFor(_selectedDevice.value)
    }

    private fun stopLogcatFor(device: Device?) {
        val activeJob = logcatJob
        logcatJob = null
        activeJob?.cancel()
        device?.let(adbManager::stopLogcat)
        _isLogcatRunning.value = false
    }

    fun exportLogs(outputFolder: File, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val config = com.zhang.adbhub.common.config.AdbConfig.load()
            val deviceLogPath = config.deviceLogPath ?: "/sdcard/"
            val command = "adb -s ${device.serialNumber} pull ${deviceLogPath} ${outputFolder.absolutePath}"

            try {
                when (val result = adbManager.exportLogs(device, outputFolder)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.export.device.log"), command, result.data, true)
                        onResult(StringResources.get("operation.log.export.success", outputFolder.absolutePath))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.export.device.log"), command, result.message, false)
                        onResult(StringResources.get("operation.log.export.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun clearDeviceLogs(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val config = com.zhang.adbhub.common.config.AdbConfig.load()
            val deviceLogPath = config.deviceLogPath ?: "/sdcard/"
            val clearPath = if (deviceLogPath.endsWith("/")) "${deviceLogPath}*" else "$deviceLogPath/*"
            val command = "adb -s ${device.serialNumber} shell rm -rf ${clearPath}"

            try {
                when (val result = adbManager.clearDeviceLogs(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.clear.device.log"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.clear.device.log"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun executeRoot(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} root"

            try {
                when (val result = adbManager.executeRoot(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.execute.root"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.execute.root"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun executeRemount(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} remount"

            try {
                when (val result = adbManager.executeRemount(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.execute.remount"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.execute.remount"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun rebootDevice(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} reboot"

            try {
                when (val result = adbManager.rebootDevice(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.reboot.device"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.reboot.device"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun rebootRecovery(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} reboot recovery"

            try {
                when (val result = adbManager.rebootRecovery(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.reboot.recovery"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.reboot.recovery"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun rebootBootloader(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} reboot bootloader"

            try {
                when (val result = adbManager.rebootBootloader(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.reboot.bootloader"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.reboot.bootloader"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun startApp(packageName: String, activityName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell am start -n $packageName/$activityName"

            try {
                when (val result = adbManager.startApp(device, packageName, activityName)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.start.app"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.start.app"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun getAppInfo(packageName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell dumpsys package $packageName"

            try {
                when (val result = adbManager.getAppInfo(device, packageName)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.get.app.info"), command, StringResources.get("operation.log.get.app.info.success"), true)
                        onResult(result.data)
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.get.app.info"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun stopApp(packageName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell am force-stop $packageName"

            try {
                when (val result = adbManager.stopApp(device, packageName)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.stop.app"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.stop.app"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun clearAppData(packageName: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell pm clear $packageName"

            try {
                when (val result = adbManager.clearAppData(device, packageName)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.clear.app.data"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.clear.app.data"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    // File management methods
    fun navigateToPath(path: String) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell ls -la $path"

            try {
                when (val result = adbManager.listFiles(device, path)) {
                    is AdbResult.Success -> {
                        _currentPath.value = path
                        _fileList.value = result.data
                        addOperationLog(StringResources.get("operation.log.browse.directory"), command, StringResources.get("operation.log.browse.success", result.data.size), true)
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.browse.directory"), command, result.message, false)
                        _statusMessage.value = StringResources.get("operation.log.failed", result.message)
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    private suspend fun refreshCurrentPath(device: Device) {
        val path = _currentPath.value
        val command = "adb -s ${device.serialNumber} shell ls -la $path"

        when (val result = adbManager.listFiles(device, path)) {
            is AdbResult.Success -> {
                _fileList.value = result.data
                addOperationLog(StringResources.get("operation.log.refresh.directory"), command, "Listed ${result.data.size} item(s)", true)
            }
            is AdbResult.Error -> {
                addOperationLog(StringResources.get("operation.log.refresh.directory"), command, result.message, false)
                _statusMessage.value = StringResources.get("operation.log.failed", result.message)
            }
        }
    }

    fun pullFile(remotePath: String, localPath: File, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} pull $remotePath ${localPath.absolutePath}"

            try {
                when (val result = adbManager.pullFile(device, remotePath, localPath)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.download.file"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.download.file"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun pushFileToDevice(localFile: File, remotePath: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} push ${localFile.absolutePath} $remotePath"

            try {
                when (val result = adbManager.pushFile(device, localFile, remotePath)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.upload.file"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                        // 刷新当前目录
                        refreshCurrentPath(device)
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.upload.file"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun deleteFile(path: String, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell rm -rf $path"

            try {
                when (val result = adbManager.deleteFile(device, path)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.log.delete.file"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                        // 刷新当前目录
                        refreshCurrentPath(device)
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.log.delete.file"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun cleanup() {
        stopLogcat()
        deviceMonitorJob?.cancel()
        scope.cancel()
    }
}

enum class OperationTab {
    PUSH_APK,
    DEVICE_COMMANDS,
    APP_MANAGEMENT,
    FILE_MANAGER,
    LOGS
}
