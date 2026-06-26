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
import com.zhang.adbhub.desktop.utils.CircularLogBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Locale

enum class DeviceDetectionStatus {
    OK,
    WARNING,
    ERROR,
    PENDING
}

data class DeviceDetectionItem(
    val title: String,
    val detail: String,
    val status: DeviceDetectionStatus
)

class MainViewModel(
    private val adbManager: AdbManager = DadbManager(),
    startMonitoring: Boolean = true
) {
    data class LogEntry(
        val raw: String,
        val timestamp: String? = null,
        val level: LogLevel = LogLevel.UNKNOWN,
        val tag: String? = null,
        val message: String = raw,
        val isHighlighted: Boolean = false
    )

    enum class LogLevel(val displayName: String) {
        VERBOSE("V"),
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E"),
        ASSERT("A"),
        UNKNOWN("?")
    }

    data class LogFilterState(
        val query: String = "",
        val excludeQuery: String = "",
        val level: LogLevel? = null
    )

    data class LogStreamStats(
        val total: Int = 0,
        val visible: Int = 0,
        val matched: Int = 0,
        val important: Int = 0
    )

    private data class ParsedLogLine(
        val timestamp: String? = null,
        val level: LogLevel = LogLevel.UNKNOWN,
        val tag: String? = null,
        val message: String = ""
    )

    private companion object {
        const val DEVICE_MONITOR_INTERVAL_MS = 2_000L
        const val MAX_LOG_LINES = 50_000
        const val MAX_OPERATION_LOGS = 1_000
        const val LOG_UI_BATCH_SIZE = 64
        const val LOG_UI_FLUSH_INTERVAL_MS = 80L
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val deviceRefreshMutex = Mutex()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _selectedTab = MutableStateFlow(OperationTab.DEVICE_COMMANDS)
    val selectedTab: StateFlow<OperationTab> = _selectedTab.asStateFlow()

    // Use efficient circular buffer for log lines
    private val logBuffer = CircularLogBuffer<LogEntry>(MAX_LOG_LINES)
    private val _rawLogEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    private val _logFilter = MutableStateFlow("")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()
    private val _logExcludeFilter = MutableStateFlow("")
    val logExcludeFilter: StateFlow<String> = _logExcludeFilter.asStateFlow()
    private val _logLevelFilter = MutableStateFlow<LogLevel?>(null)
    val logLevelFilter: StateFlow<LogLevel?> = _logLevelFilter.asStateFlow()

    val logFilterState: StateFlow<LogFilterState> = combine(
        _logFilter,
        _logExcludeFilter,
        _logLevelFilter
    ) { query, excludeQuery, level ->
        LogFilterState(query = query, excludeQuery = excludeQuery, level = level)
    }.stateIn(scope, SharingStarted.Eagerly, LogFilterState())

    // Optimization: Deduplicate filter logic to prevent unnecessary recomposition
    // Use stateIn with Eagerly to maintain a stable reference that only updates when actual content changes
    val logLines: StateFlow<List<LogEntry>> = combine(_rawLogEntries, logFilterState) { entries, filterState ->
        entries.filter { entry ->
            matchesLogFilter(entry, filterState)
        }.map { entry ->
            entry.copy(isHighlighted = shouldHighlight(entry, filterState))
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val logStats: StateFlow<LogStreamStats> = combine(_rawLogEntries, logLines) { rawEntries, visibleEntries ->
        LogStreamStats(
            total = rawEntries.size,
            visible = visibleEntries.size,
            matched = visibleEntries.count { it.isHighlighted },
            important = visibleEntries.count { isImportantLog(it) }
        )
    }.stateIn(scope, SharingStarted.Eagerly, LogStreamStats())

    private val _isLogcatRunning = MutableStateFlow(false)
    val isLogcatRunning: StateFlow<Boolean> = _isLogcatRunning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _adbStatus = MutableStateFlow<String?>(null)
    val adbStatus: StateFlow<String?> = _adbStatus.asStateFlow()

    private val _deviceDiagnostics = MutableStateFlow(defaultDeviceDiagnostics())
    val deviceDiagnostics: StateFlow<List<DeviceDetectionItem>> = _deviceDiagnostics.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _isExecutingRootCommand = MutableStateFlow(false)

    private val operationLogBuffer = ArrayDeque<OperationLog>(MAX_OPERATION_LOGS)
    private val _operationLogs = MutableStateFlow<List<OperationLog>>(emptyList())
    val operationLogs: StateFlow<List<OperationLog>> = _operationLogs.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _fileList = MutableStateFlow<List<FileInfo>>(emptyList())
    val fileList: StateFlow<List<FileInfo>> = _fileList.asStateFlow()

    private var logcatJob: Job? = null
    private var deviceMonitorJob: Job? = null
    private var logcatStreamGeneration = 0L
    private var logcatClearGeneration = 0L

    init {
        if (startMonitoring) {
            startDeviceMonitor()
        }
    }

    data class OperationLog(
        val timestamp: String,
        val operation: String,
        val command: String?,
        val output: String?,
        val success: Boolean
    )

    private fun parseLogEntry(rawLine: String): LogEntry {
        val parsed = parseThreadTimeLog(rawLine)
        return LogEntry(
            raw = rawLine,
            timestamp = parsed.timestamp,
            level = parsed.level,
            tag = parsed.tag,
            message = parsed.message.ifBlank { rawLine }
        )
    }

    private fun parseThreadTimeLog(rawLine: String): ParsedLogLine {
        val parts = rawLine.trimStart().split(Regex("\\s+"), limit = 7)
        if (parts.size < 7) {
            return ParsedLogLine(message = rawLine)
        }

        val level = when (parts[4].uppercase(Locale.ROOT)) {
            "V" -> LogLevel.VERBOSE
            "D" -> LogLevel.DEBUG
            "I" -> LogLevel.INFO
            "W" -> LogLevel.WARN
            "E" -> LogLevel.ERROR
            "F", "A" -> LogLevel.ASSERT
            else -> LogLevel.UNKNOWN
        }

        return ParsedLogLine(
            timestamp = "${parts[0]} ${parts[1]}",
            level = level,
            tag = parts[5].removeSuffix(":"),
            message = parts[6]
        )
    }

    private fun matchesLogFilter(entry: LogEntry, filterState: LogFilterState): Boolean {
        val level = filterState.level
        if (level != null && entry.level != level) {
            return false
        }

        val queryTokens = splitLogQuery(filterState.query)
        if (queryTokens.isNotEmpty() && queryTokens.none { token ->
                entry.raw.contains(token, ignoreCase = true)
            }
        ) {
            return false
        }

        val excludeTokens = splitLogQuery(filterState.excludeQuery)
        if (excludeTokens.any { token -> entry.raw.contains(token, ignoreCase = true) }) {
            return false
        }

        return true
    }

    private fun shouldHighlight(entry: LogEntry, filterState: LogFilterState): Boolean {
        val queryTokens = splitLogQuery(filterState.query)
        return isImportantLog(entry) || queryTokens.any { token ->
            entry.raw.contains(token, ignoreCase = true)
        }
    }

    private fun isImportantLog(entry: LogEntry): Boolean {
        return entry.level == LogLevel.ERROR || entry.level == LogLevel.WARN || entry.level == LogLevel.ASSERT
    }

    private fun splitLogQuery(query: String): List<String> {
        return query.split(',', '|', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun addOperationLog(operation: String, command: String? = null, output: String? = null, success: Boolean) {
        val timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val log = OperationLog(timestamp, operation, command, output, success)

        operationLogBuffer.addLast(log)
        if (operationLogBuffer.size > MAX_OPERATION_LOGS) {
            operationLogBuffer.removeFirst()
        }
        // Optimization: Create new immutable list instance to ensure Compose detects the change
        // This prevents unnecessary recomposition while maintaining reactivity
        _operationLogs.value = operationLogBuffer.toList()
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
            // Load config once per refresh
            val config = withContext(Dispatchers.IO) { AdbConfig.load() }
            val configuredAdbPath = config.customAdbPath?.trim().orEmpty()

            // Validate ADB path once per refresh
            val adbPath = withContext(Dispatchers.IO) {
                AdbPathDetector.getValidAdbPath(config.customAdbPath)
            }

            if (adbPath == null) {
                _adbStatus.value = StringResources.get("status.adb.not.found")
                _deviceDiagnostics.value = buildAdbMissingDiagnostics(configuredAdbPath)
                applyDeviceSnapshot(emptyList(), silent)
                if (!silent) {
                    _statusMessage.value = StringResources.get("status.adb.not.found")
                }
                return
            }

            _adbStatus.value = StringResources.get("status.adb.found", adbPath)

            when (val result = adbManager.getDevices()) {
                is AdbResult.Success -> {
                    _deviceDiagnostics.value = buildDeviceDiagnostics(adbPath, result.data)
                    applyDeviceSnapshot(result.data, silent)
                }
                is AdbResult.Error -> {
                    _deviceDiagnostics.value = buildDeviceQueryErrorDiagnostics(adbPath, result.message)
                    if (!silent) {
                        _statusMessage.value = StringResources.get("status.get.devices.failed", result.message)
                    }
                }
            }
        }
    }

    private fun defaultDeviceDiagnostics(): List<DeviceDetectionItem> {
        return listOf(
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.path"),
                StringResources.get("device.diagnostic.adb.path.pending"),
                DeviceDetectionStatus.PENDING
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.command"),
                StringResources.get("device.diagnostic.adb.command.pending"),
                DeviceDetectionStatus.PENDING
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.connection"),
                StringResources.get("device.diagnostic.device.connection.pending"),
                DeviceDetectionStatus.PENDING
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.usb"),
                StringResources.get("device.diagnostic.device.usb.pending"),
                DeviceDetectionStatus.PENDING
            )
        )
    }

    private fun buildAdbMissingDiagnostics(configuredAdbPath: String): List<DeviceDetectionItem> {
        val adbPathDetail = if (configuredAdbPath.isBlank()) {
            StringResources.get("device.diagnostic.adb.path.missing")
        } else {
            StringResources.get("device.diagnostic.adb.path.invalid", configuredAdbPath)
        }

        return listOf(
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.path"),
                adbPathDetail,
                DeviceDetectionStatus.ERROR
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.command"),
                StringResources.get("device.diagnostic.adb.command.pending"),
                DeviceDetectionStatus.PENDING
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.connection"),
                StringResources.get("device.diagnostic.device.connection.pending"),
                DeviceDetectionStatus.PENDING
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.usb"),
                StringResources.get("device.diagnostic.device.usb.pending"),
                DeviceDetectionStatus.PENDING
            )
        )
    }

    private fun buildDeviceQueryErrorDiagnostics(adbPath: String, errorMessage: String): List<DeviceDetectionItem> {
        return listOf(
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.path"),
                StringResources.get("device.diagnostic.adb.path.ok", adbPath),
                DeviceDetectionStatus.OK
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.command"),
                StringResources.get("device.diagnostic.adb.command.failed", errorMessage),
                DeviceDetectionStatus.ERROR
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.connection"),
                StringResources.get("device.diagnostic.device.connection.pending"),
                DeviceDetectionStatus.PENDING
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.usb"),
                StringResources.get("device.diagnostic.device.usb.pending"),
                DeviceDetectionStatus.PENDING
            )
        )
    }

    private fun buildDeviceDiagnostics(adbPath: String, latestDevices: List<Device>): List<DeviceDetectionItem> {
        val diagnostics = mutableListOf(
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.path"),
                StringResources.get("device.diagnostic.adb.path.ok", adbPath),
                DeviceDetectionStatus.OK
            ),
            DeviceDetectionItem(
                StringResources.get("device.diagnostic.adb.command"),
                StringResources.get("device.diagnostic.adb.command.ok"),
                DeviceDetectionStatus.OK
            )
        )

        if (latestDevices.isEmpty()) {
            diagnostics += DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.connection"),
                StringResources.get("device.diagnostic.device.connection.none"),
                DeviceDetectionStatus.ERROR
            )
            diagnostics += DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.usb"),
                StringResources.get("device.diagnostic.device.usb.pending"),
                DeviceDetectionStatus.PENDING
            )
            return diagnostics
        }

        // Optimize: single pass to categorize devices
        var onlineCount = 0
        var unauthorizedCount = 0
        var offlineCount = 0
        var unknownCount = 0
        val onlineDevices = mutableListOf<Device>()
        val unauthorizedDevices = mutableListOf<Device>()
        val offlineDevices = mutableListOf<Device>()
        val unknownDevices = mutableListOf<Device>()

        for (device in latestDevices) {
            when (device.state) {
                DeviceState.ONLINE -> {
                    onlineCount++
                    onlineDevices.add(device)
                }
                DeviceState.UNAUTHORIZED -> {
                    unauthorizedCount++
                    unauthorizedDevices.add(device)
                }
                DeviceState.OFFLINE -> {
                    offlineCount++
                    offlineDevices.add(device)
                }
                DeviceState.UNKNOWN -> {
                    unknownCount++
                    unknownDevices.add(device)
                }
            }
        }

        diagnostics += DeviceDetectionItem(
            StringResources.get("device.diagnostic.device.connection"),
            StringResources.get(
                "device.diagnostic.device.connection.detected",
                latestDevices.size,
                summarizeSerials(latestDevices)
            ),
            if (onlineCount > 0) DeviceDetectionStatus.OK else DeviceDetectionStatus.WARNING
        )

        diagnostics += when {
            onlineCount > 0 -> DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.online"),
                StringResources.get("device.diagnostic.device.online.ok", onlineCount),
                DeviceDetectionStatus.OK
            )
            unauthorizedCount > 0 -> DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.usb"),
                StringResources.get(
                    "device.diagnostic.device.unauthorized",
                    unauthorizedCount,
                    summarizeSerials(unauthorizedDevices)
                ),
                DeviceDetectionStatus.ERROR
            )
            offlineCount > 0 -> DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.online"),
                StringResources.get(
                    "device.diagnostic.device.offline",
                    offlineCount,
                    summarizeSerials(offlineDevices)
                ),
                DeviceDetectionStatus.ERROR
            )
            else -> DeviceDetectionItem(
                StringResources.get("device.diagnostic.device.online"),
                StringResources.get(
                    "device.diagnostic.device.unknown",
                    unknownCount,
                    summarizeSerials(unknownDevices)
                ),
                DeviceDetectionStatus.WARNING
            )
        }

        return diagnostics
    }

    private fun summarizeSerials(devices: List<Device>): String {
        val serials = devices.map { it.serialNumber }
        return if (serials.size <= 2) {
            serials.joinToString(", ")
        } else {
            serials.take(2).joinToString(", ") + " +${serials.size - 2}"
        }
    }

    private fun applyDeviceSnapshot(latestDevices: List<Device>, silent: Boolean) {
        val previousSelectedDevice = _selectedDevice.value
        val onlineDevices = latestDevices.filter { it.state == DeviceState.ONLINE }
        val updatedSelectedDevice = previousSelectedDevice?.let { selected ->
            onlineDevices.firstOrNull { it.serialNumber == selected.serialNumber }
        }

        // Optimization: Only update devices StateFlow if the list actually changed
        // This prevents unnecessary recomposition of all subscribers
        val previousDevices = _devices.value
        if (!devicesEqual(previousDevices, latestDevices)) {
            _devices.value = latestDevices
        }

        // Skip updating selectedDevice during root/remount command execution
        // The device will temporarily disconnect during ADB daemon restart
        if (_isExecutingRootCommand.value) {
            return
        }

        when {
            previousSelectedDevice != null && updatedSelectedDevice == null -> {
                clearSelectedDevice(previousSelectedDevice)
                _statusMessage.value = StringResources.get("status.device.disconnected", previousSelectedDevice.serialNumber)
            }
            updatedSelectedDevice != null -> {
                // Optimization: Only update selectedDevice if it actually changed
                if (!deviceEqual(previousSelectedDevice, updatedSelectedDevice)) {
                    _selectedDevice.value = updatedSelectedDevice
                }
                if (!silent) {
                    _statusMessage.value = null
                }
            }
            onlineDevices.isNotEmpty() -> {
                val newDevice = onlineDevices.first()
                // Optimization: Only update selectedDevice if it actually changed
                if (!deviceEqual(previousSelectedDevice, newDevice)) {
                    _selectedDevice.value = newDevice
                }
                if (!silent) {
                    _statusMessage.value = null
                }
            }
            !silent -> {
                _statusMessage.value = StringResources.get("status.no.device.connected")
            }
        }
    }

    /**
     * Compare two devices by their essential properties to detect meaningful changes.
     * This prevents StateFlow emission when only object references change but data is identical.
     */
    private fun deviceEqual(a: Device?, b: Device?): Boolean {
        if (a === b) return true
        if (a == null || b == null) return false
        return a.serialNumber == b.serialNumber &&
            a.state == b.state &&
            a.model == b.model &&
            a.transportId == b.transportId
    }

    /**
     * Compare two device lists by their essential properties.
     * This prevents StateFlow emission during periodic device monitoring when nothing changed.
     */
    private fun devicesEqual(a: List<Device>, b: List<Device>): Boolean {
        if (a === b) return true
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (!deviceEqual(a[i], b[i])) return false
        }
        return true
    }

    private fun clearSelectedDevice(device: Device?) {
        stopLogcatFor(device)
        _selectedDevice.value = null
        logBuffer.clear()
        _rawLogEntries.value = emptyList()
        _currentPath.value = ""
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
        logBuffer.clear()
        _rawLogEntries.value = emptyList()
        _currentPath.value = ""
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

    fun setLogExcludeFilter(filter: String) {
        _logExcludeFilter.value = filter
    }

    fun setLogLevelFilter(level: LogLevel?) {
        _logLevelFilter.value = level
    }

    fun resetLogFilters() {
        _logFilter.value = ""
        _logExcludeFilter.value = ""
        _logLevelFilter.value = null
    }

    fun clearLogcat() {
        logcatClearGeneration++
        logBuffer.clear()
        _rawLogEntries.value = emptyList()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun startLogcat(filter: String = _logFilter.value) {
        val device = _selectedDevice.value ?: return
        stopLogcatFor(device)

        _logFilter.value = filter
        _isLogcatRunning.value = true
        logBuffer.clear()
        _rawLogEntries.value = emptyList()
        val streamGeneration = ++logcatStreamGeneration

        val newJob = scope.launch(start = CoroutineStart.LAZY) {
            val pendingLines = ArrayList<LogEntry>(LOG_UI_BATCH_SIZE)
            var pendingLogUpdates = 0
            var lastPublishedAtMs = 0L
            var scheduledFlush: Job? = null
            var observedClearGeneration = logcatClearGeneration

            fun isCurrentStream(): Boolean {
                return logcatJob === coroutineContext[Job] &&
                    logcatStreamGeneration == streamGeneration
            }

            fun syncClearGeneration(): Boolean {
                if (observedClearGeneration == logcatClearGeneration) {
                    return false
                }
                observedClearGeneration = logcatClearGeneration
                pendingLines.clear()
                pendingLogUpdates = 0
                scheduledFlush?.cancel()
                scheduledFlush = null
                return true
            }

            fun flushPendingLinesToBuffer() {
                if (!isCurrentStream() || syncClearGeneration()) {
                    pendingLines.clear()
                    return
                }
                if (pendingLines.isEmpty()) return
                logBuffer.addAll(pendingLines)
                pendingLogUpdates += pendingLines.size
                pendingLines.clear()
            }

            fun publishLogs(cancelScheduled: Boolean = true) {
                if (!isCurrentStream() || syncClearGeneration()) {
                    pendingLines.clear()
                    pendingLogUpdates = 0
                    return
                }
                flushPendingLinesToBuffer()
                if (pendingLogUpdates == 0) return
                pendingLogUpdates = 0
                if (cancelScheduled) {
                    scheduledFlush?.cancel()
                }
                scheduledFlush = null
                _rawLogEntries.value = logBuffer.toList()
                lastPublishedAtMs = System.currentTimeMillis()
            }

            fun scheduleFlush() {
                if (scheduledFlush?.isActive == true) return
                scheduledFlush = launch {
                    delay(LOG_UI_FLUSH_INTERVAL_MS)
                    scheduledFlush = null
                    publishLogs(cancelScheduled = false)
                }
            }

            try {
                adbManager.getLogcatFlow(device)
                    .collect { line ->
                        syncClearGeneration()
                        pendingLines.add(parseLogEntry(line))
                        if (pendingLines.size >= LOG_UI_BATCH_SIZE) {
                            flushPendingLinesToBuffer()
                        }
                        val canPublishNow = pendingLogUpdates >= LOG_UI_BATCH_SIZE &&
                            System.currentTimeMillis() - lastPublishedAtMs >= LOG_UI_FLUSH_INTERVAL_MS
                        if (canPublishNow) {
                            publishLogs()
                        } else {
                            scheduleFlush()
                        }
                    }
            } finally {
                scheduledFlush?.cancel()
                if (isCurrentStream()) {
                    publishLogs(cancelScheduled = false)
                    _isLogcatRunning.value = false
                    logcatJob = null
                } else {
                    pendingLines.clear()
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
        logcatStreamGeneration++
        activeJob?.cancel()
        device?.let(adbManager::stopLogcat)
        _isLogcatRunning.value = false
    }

    fun exportLogs(outputFolder: File, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val config = com.zhang.adbhub.common.config.AdbConfig.load()
            val deviceLogPath = config.deviceLogPath?.trim()?.takeIf { it.isNotEmpty() }
            if (deviceLogPath == null) {
                val message = StringResources.get("operation.log.device.path.required")
                addOperationLog(StringResources.get("operation.log.export.device.log"), null, message, false)
                onResult(message)
                _isExecuting.value = false
                return@launch
            }
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
            val deviceLogPath = config.deviceLogPath?.trim()?.takeIf { it.isNotEmpty() }
            if (deviceLogPath == null) {
                val message = StringResources.get("operation.log.device.path.required")
                addOperationLog(StringResources.get("operation.log.clear.device.log"), null, message, false)
                onResult(message)
                _isExecuting.value = false
                return@launch
            }
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

    fun executeDeviceCommand(operation: String, arguments: List<String>, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} ${arguments.joinToString(" ")}"

            try {
                when (val result = adbManager.executeDeviceCommand(device, arguments)) {
                    is AdbResult.Success -> {
                        addOperationLog(operation, command, result.data, true)
                        onResult(result.data)
                    }
                    is AdbResult.Error -> {
                        addOperationLog(operation, command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun captureScreenshot(operation: String, outputFile: File, onResult: (AdbResult<File>) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} exec-out screencap -p > ${outputFile.absolutePath}"

            try {
                when (val result = adbManager.captureScreenshot(device, outputFile)) {
                    is AdbResult.Success -> {
                        val message = StringResources.get("operation.command.screenshot.preview.ready", outputFile.absolutePath)
                        addOperationLog(operation, command, message, true)
                        onResult(result)
                    }
                    is AdbResult.Error -> {
                        addOperationLog(operation, command, result.message, false)
                        onResult(result)
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun executeRoot(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            _isExecutingRootCommand.value = true
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
                _isExecutingRootCommand.value = false
            }
        }
    }

    fun executeRemount(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            _isExecutingRootCommand.value = true
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
                _isExecutingRootCommand.value = false
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

    fun goHome(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell input keyevent KEYCODE_HOME"

            try {
                when (val result = adbManager.goHome(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.go.home"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.go.home"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun enableVerity(onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} enable-verity"

            try {
                when (val result = adbManager.enableVerity(device)) {
                    is AdbResult.Success -> {
                        addOperationLog(StringResources.get("operation.enable.verity"), command, result.data, true)
                        onResult(StringResources.get("operation.log.success", result.data))
                    }
                    is AdbResult.Error -> {
                        addOperationLog(StringResources.get("operation.enable.verity"), command, result.message, false)
                        onResult(StringResources.get("operation.log.failed", result.message))
                    }
                }
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun startApp(packageName: String, activityName: String?, onResult: (String) -> Unit) {
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = if (!activityName.isNullOrBlank()) {
                "adb -s ${device.serialNumber} shell am start -n $packageName/$activityName"
            } else {
                "adb -s ${device.serialNumber} shell monkey -p $packageName -c android.intent.category.LAUNCHER 1"
            }

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
        val normalizedPath = path.trim()
        if (normalizedPath.isEmpty()) {
            return
        }
        val device = _selectedDevice.value ?: return
        scope.launch {
            _isExecuting.value = true
            val command = "adb -s ${device.serialNumber} shell ls -la $normalizedPath"

            try {
                when (val result = adbManager.listFiles(device, normalizedPath)) {
                    is AdbResult.Success -> {
                        _currentPath.value = normalizedPath
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

    fun clearPath() {
        _currentPath.value = ""
        _fileList.value = emptyList()
    }

    private suspend fun refreshCurrentPath(device: Device) {
        val path = _currentPath.value
        if (path.isBlank()) {
            return
        }
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
