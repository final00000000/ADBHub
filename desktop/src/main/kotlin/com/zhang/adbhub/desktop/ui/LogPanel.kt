package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.desktop.viewmodel.MainViewModel
import com.zhang.adbhub.desktop.utils.StringResources
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser

private const val LOG_EXPORT_FOLDER_PREFIX = "logs"
private const val UNKNOWN_DEVICE_SERIAL = "unknown"
private const val LOG_EXPORT_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"

@Composable
fun LogPanel(
    selectedDevice: Device?,
    viewModel: MainViewModel,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedLogTab by remember {
        mutableStateOf(AdbConfig.load().lastLogTab)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        LogTabBar(
            selectedLogTab = selectedLogTab,
            onTabSelected = { tab ->
                selectedLogTab = tab
                val config = AdbConfig.load()
                AdbConfig.save(config.copy(lastLogTab = tab))
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedLogTab) {
                0 -> OperationLogView(viewModel, modifier = Modifier.fillMaxSize())
                1 -> DeviceLogView(selectedDevice, viewModel, isFullscreen, onToggleFullscreen, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun LogTabBar(
    selectedLogTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.height(76.dp)) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LogTabButton(
                selected = selectedLogTab == 0,
                label = StringResources.get("log.panel.operations"),
                icon = Icons.AutoMirrored.Filled.List,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            LogTabButton(
                selected = selectedLogTab == 1,
                label = StringResources.get("log.panel.device.log"),
                icon = Icons.Default.PhoneAndroid,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun LogTabButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .padding(horizontal = 8.dp)
        ) {
            if (selected) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.extraSmall,
                    content = {}
                )
            }
        }
    }
}

@Composable
fun OperationLogView(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val logs by viewModel.operationLogs.collectAsState()
    val listState = rememberLazyListState()

    // Optimization: Use derivedStateOf to cache scroll position calculation
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            totalItems == 0 || layoutInfo.visibleItemsInfo.lastOrNull()?.index == totalItems - 1
        }
    }

    // Optimization: Reduce LaunchedEffect dependencies - only trigger on logs.size and when at bottom
    LaunchedEffect(logs.size, isAtBottom) {
        if (logs.isNotEmpty() && isAtBottom) {
            listState.scrollToItem(logs.lastIndex)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = StringResources.get("log.panel.operations"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (logs.isEmpty()) {
                EmptyLogMessage(StringResources.get("log.panel.no.operations"))
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp, start = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Optimization: Add stable key based on timestamp to prevent recomposition flicker
                        items(
                            items = logs,
                            key = { log -> log.timestamp }
                        ) { log ->
                            OperationLogItem(log)
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun OperationLogItem(log: MainViewModel.OperationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log.success) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = if (log.success) "OK" else "ERR",
                    color = if (log.success) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = log.operation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            log.command?.let { command ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            log.output?.let { output ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = output,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DeviceLogView(selectedDevice: Device?, viewModel: MainViewModel, isFullscreen: Boolean, onToggleFullscreen: () -> Unit, modifier: Modifier = Modifier) {
    val logLines by viewModel.logLines.collectAsState()
    val filterText by viewModel.logFilter.collectAsState()
    val excludeFilterText by viewModel.logExcludeFilter.collectAsState()
    val levelFilter by viewModel.logLevelFilter.collectAsState()
    val logStats by viewModel.logStats.collectAsState()
    val isLogcatRunning by viewModel.isLogcatRunning.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    var exportStatus by remember { mutableStateOf("") }
    var showClearDeviceConfirm by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val horizontalLogScrollState = rememberScrollState()

    // Optimization: Use derivedStateOf to cache scroll behavior check, reducing recomposition
    val shouldScrollToTop by remember {
        derivedStateOf {
            logLines.isNotEmpty() && (filterText.isNotBlank() || excludeFilterText.isNotBlank() || levelFilter != null)
        }
    }

    val shouldScrollToBottom by remember {
        derivedStateOf {
            logLines.isNotEmpty() && isLogcatRunning && filterText.isBlank() && excludeFilterText.isBlank() && levelFilter == null
        }
    }

    // Optimization: Separate LaunchedEffects with minimal dependencies to reduce unnecessary triggers
    LaunchedEffect(shouldScrollToTop, filterText, excludeFilterText, levelFilter) {
        if (shouldScrollToTop) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(shouldScrollToBottom, logLines.size) {
        if (shouldScrollToBottom) {
            listState.scrollToItem(logLines.lastIndex)
        }
    }

    Column(modifier = modifier) {
        LogToolbar(
            isFullscreen = isFullscreen,
            isLogcatRunning = isLogcatRunning,
            selectedDevice = selectedDevice,
            isExecuting = isExecuting,
            hasLogs = logStats.total > 0,
            onToggleFullscreen = onToggleFullscreen,
            onToggleStream = {
                if (isLogcatRunning) {
                    viewModel.stopLogcat()
                } else {
                    viewModel.startLogcat()
                }
            },
            onClearVisible = { viewModel.clearLogcat() },
            onExport = {
                val chooser = JFileChooser().apply {
                    dialogTitle = StringResources.get("log.panel.export.directory.title")
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isAcceptAllFileFilterUsed = false
                }

                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val outputFolder = createLogExportFolder(chooser.selectedFile, selectedDevice)

                    viewModel.exportLogs(outputFolder) { result ->
                        exportStatus = result
                    }
                }
            },
            onClearDevice = { showClearDeviceConfirm = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LogFilterControls(
            filterText = filterText,
            excludeFilterText = excludeFilterText,
            levelFilter = levelFilter,
            enabled = selectedDevice != null,
            onFilterChanged = { viewModel.setLogFilter(it) },
            onExcludeChanged = { viewModel.setLogExcludeFilter(it) },
            onLevelSelected = { viewModel.setLogLevelFilter(it) },
            onResetFilters = { viewModel.resetLogFilters() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LogStatsBar(
            isLogcatRunning = isLogcatRunning,
            stats = logStats,
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            when {
                selectedDevice == null -> DeviceRequiredPrompt(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    description = StringResources.get("log.panel.select.device")
                )
                logLines.isEmpty() -> {
                    EmptyLogMessage(
                        if (isLogcatRunning) {
                            StringResources.get("log.panel.waiting.logs")
                        } else {
                            StringResources.get("log.panel.start.hint")
                        }
                    )
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 12.dp, start = 8.dp, top = 8.dp, bottom = 20.dp)
                                .horizontalScroll(horizontalLogScrollState)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.width(2400.dp).fillMaxHeight()
                            ) {
                                // Optimization: Use index-based key to prevent key conflicts
                                // Log lines can be duplicated, so we use index as unique identifier
                                items(
                                    count = logLines.size,
                                    key = { index -> "log_$index" }
                                ) { index ->
                                    LogLineItem(
                                        entry = logLines[index],
                                        isFullscreen = isFullscreen,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                        HorizontalScrollbar(
                            adapter = rememberScrollbarAdapter(horizontalLogScrollState),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 20.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (exportStatus.isNotEmpty()) {
                Text(
                    text = exportStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (
                        exportStatus.contains(StringResources.get("status.success.keyword")) ||
                        exportStatus.contains("success", true)
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(8.dp),
                    maxLines = 2
                )
            }
        }

        if (showClearDeviceConfirm) {
            AlertDialog(
                onDismissRequest = { showClearDeviceConfirm = false },
                title = { Text(StringResources.get("log.panel.clear.device.confirm.title")) },
                text = { Text(StringResources.get("log.panel.clear.device.confirm.message")) },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearDeviceConfirm = false
                            viewModel.clearDeviceLogs { result ->
                                exportStatus = result
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(StringResources.get("log.panel.confirm"))
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showClearDeviceConfirm = false }
                    ) {
                        Text(StringResources.get("log.panel.cancel"))
                    }
                }
            )
        }
    }
}

private fun createLogExportFolder(parentDir: File, selectedDevice: Device?): File {
    val timestamp = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern(LOG_EXPORT_TIMESTAMP_PATTERN))
    val deviceSerial = selectedDevice?.serialNumber ?: UNKNOWN_DEVICE_SERIAL

    // 只自动命名本地最外层导出目录，设备内原始日志文件名保持不变。
    return File(parentDir, "${LOG_EXPORT_FOLDER_PREFIX}_${deviceSerial}_$timestamp")
}

@Composable
private fun EmptyLogMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LogToolbar(
    isFullscreen: Boolean,
    isLogcatRunning: Boolean,
    selectedDevice: Device?,
    isExecuting: Boolean,
    hasLogs: Boolean,
    onToggleFullscreen: () -> Unit,
    onToggleStream: () -> Unit,
    onClearVisible: () -> Unit,
    onExport: () -> Unit,
    onClearDevice: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = StringResources.get("log.panel.logcat.stream"),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactLogButton(
                icon = if (isLogcatRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                label = if (isLogcatRunning) StringResources.get("log.panel.stop") else StringResources.get("log.panel.start"),
                enabled = selectedDevice != null,
                isDanger = isLogcatRunning,
                onClick = onToggleStream
            )
            CompactLogButton(
                icon = Icons.Default.ClearAll,
                label = StringResources.get("log.panel.clear"),
                enabled = selectedDevice != null && hasLogs,
                onClick = onClearVisible
            )
            CompactLogButton(
                icon = Icons.Default.Save,
                label = StringResources.get("log.panel.export"),
                enabled = selectedDevice != null && !isExecuting,
                onClick = onExport
            )
            CompactLogButton(
                icon = Icons.Default.Delete,
                label = StringResources.get("log.panel.clear.device"),
                enabled = selectedDevice != null && !isExecuting,
                isDanger = true,
                onClick = onClearDevice
            )
            Spacer(modifier = Modifier.weight(1f))
            CompactLogButton(
                icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                label = if (isFullscreen) StringResources.get("log.panel.exit.fullscreen") else StringResources.get("log.panel.fullscreen"),
                onClick = onToggleFullscreen
            )
        }
    }
}

@Composable
private fun CompactLogButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isDanger -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            },
            disabledContainerColor = when {
                isDanger -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun LogFilterControls(
    filterText: String,
    excludeFilterText: String,
    levelFilter: MainViewModel.LogLevel?,
    enabled: Boolean,
    onFilterChanged: (String) -> Unit,
    onExcludeChanged: (String) -> Unit,
    onLevelSelected: (MainViewModel.LogLevel?) -> Unit,
    onResetFilters: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = filterText,
                onValueChange = onFilterChanged,
                label = { Text(StringResources.get("log.panel.filter")) },
                placeholder = { Text(StringResources.get("log.panel.filter.placeholder")) },
                modifier = Modifier.weight(1.3f).height(58.dp),
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = { onFilterChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = StringResources.get("log.panel.clear.filter"))
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = excludeFilterText,
                onValueChange = onExcludeChanged,
                label = { Text(StringResources.get("log.panel.exclude")) },
                placeholder = { Text(StringResources.get("log.panel.exclude.placeholder")) },
                modifier = Modifier.weight(1f).height(58.dp),
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    if (excludeFilterText.isNotEmpty()) {
                        IconButton(onClick = { onExcludeChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = StringResources.get("log.panel.clear.filter"))
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LogLevelChip(
                label = StringResources.get("log.panel.level.all"),
                selected = levelFilter == null,
                enabled = enabled,
                onClick = { onLevelSelected(null) }
            )
            listOf(
                MainViewModel.LogLevel.ERROR,
                MainViewModel.LogLevel.WARN,
                MainViewModel.LogLevel.INFO,
                MainViewModel.LogLevel.DEBUG
            ).forEach { level ->
                LogLevelChip(
                    label = level.displayName,
                    selected = levelFilter == level,
                    enabled = enabled,
                    color = logLevelColor(level),
                    onClick = { onLevelSelected(level) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onResetFilters,
                enabled = enabled && (filterText.isNotBlank() || excludeFilterText.isNotBlank() || levelFilter != null),
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(StringResources.get("log.panel.reset.filters"), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LogLevelChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val background = when {
        selected -> color.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        selected -> color
        enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = Modifier
            .height(30.dp)
            .clickable(enabled = enabled, onClick = onClick),
        color = background,
        shape = MaterialTheme.shapes.small
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LogStatsBar(
    isLogcatRunning: Boolean,
    stats: MainViewModel.LogStreamStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (isLogcatRunning) {
                StringResources.get("log.panel.streaming.stats", stats.visible, stats.total, stats.important, stats.matched)
            } else {
                StringResources.get("log.panel.stopped.stats", stats.visible, stats.total, stats.important, stats.matched)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun LogLineItem(
    entry: MainViewModel.LogEntry,
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fontSize = if (isFullscreen) 14.sp else 12.sp
    val lineHeight = if (isFullscreen) 18.sp else 16.sp
    val itemHeight = if (isFullscreen) 26.dp else 22.dp
    val levelColor = logLevelColor(entry.level)
    val backgroundColor = when {
        entry.isHighlighted && entry.level == MainViewModel.LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
        entry.isHighlighted -> levelColor.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    }
    val annotatedLine = buildAnnotatedString {
        entry.timestamp?.let { timestamp ->
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(timestamp)
                append(" ")
            }
        }
        withStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.Bold)) {
            append(entry.level.displayName)
            append(" ")
        }
        entry.tag?.let { tag ->
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                append(tag)
                append(": ")
            }
        }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
            append(entry.message)
        }
    }

    Text(
        text = annotatedLine,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        lineHeight = lineHeight,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .height(itemHeight)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun logLevelColor(level: MainViewModel.LogLevel): Color {
    return when (level) {
        MainViewModel.LogLevel.ERROR,
        MainViewModel.LogLevel.ASSERT -> MaterialTheme.colorScheme.error
        MainViewModel.LogLevel.WARN -> Color(0xFFB26A00)
        MainViewModel.LogLevel.INFO -> MaterialTheme.colorScheme.primary
        MainViewModel.LogLevel.DEBUG -> MaterialTheme.colorScheme.secondary
        MainViewModel.LogLevel.VERBOSE -> MaterialTheme.colorScheme.onSurfaceVariant
        MainViewModel.LogLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
