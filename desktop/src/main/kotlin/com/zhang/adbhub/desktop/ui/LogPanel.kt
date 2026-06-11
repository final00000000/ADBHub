package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.desktop.viewmodel.MainViewModel
import com.zhang.adbhub.desktop.utils.StringResources
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        TabRow(
            selectedTabIndex = selectedLogTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedLogTab == 0,
                onClick = {
                    selectedLogTab = 0
                    val config = AdbConfig.load()
                    AdbConfig.save(config.copy(lastLogTab = 0))
                },
                text = { Text(StringResources.get("log.panel.operations"), style = MaterialTheme.typography.labelLarge) },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Tab(
                selected = selectedLogTab == 1,
                onClick = {
                    selectedLogTab = 1
                    val config = AdbConfig.load()
                    AdbConfig.save(config.copy(lastLogTab = 1))
                },
                text = { Text(StringResources.get("log.panel.device.log"), style = MaterialTheme.typography.labelLarge) },
                icon = {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }

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
fun OperationLogView(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val logs by viewModel.operationLogs.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
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
                        items(logs) { log ->
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
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 2
                )
            }

            log.output?.let { output ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = output,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceLogView(selectedDevice: Device?, viewModel: MainViewModel, isFullscreen: Boolean, onToggleFullscreen: () -> Unit, modifier: Modifier = Modifier) {
    val logLines by viewModel.logLines.collectAsState()
    val filterText by viewModel.logFilter.collectAsState()
    val isLogcatRunning by viewModel.isLogcatRunning.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    var exportStatus by remember { mutableStateOf("") }
    var showClearDeviceConfirm by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val horizontalLogScrollState = rememberScrollState()

    LaunchedEffect(filterText) {
        if (logLines.isNotEmpty() && filterText.isNotBlank()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(logLines.size, filterText, isLogcatRunning) {
        if (logLines.isNotEmpty() && isLogcatRunning && filterText.isBlank()) {
            listState.scrollToItem(logLines.lastIndex)
        }
    }

    Column(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = StringResources.get("log.panel.logcat.stream"),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isFullscreen) StringResources.get("log.panel.exit.fullscreen") else StringResources.get("log.panel.fullscreen"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = {
                        if (isLogcatRunning) {
                            viewModel.stopLogcat()
                        } else {
                            viewModel.startLogcat()
                        }
                    },
                    enabled = selectedDevice != null,
                    modifier = Modifier.height(36.dp),
                    colors = if (isLogcatRunning) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Icon(
                        if (isLogcatRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isLogcatRunning) StringResources.get("log.panel.stop") else StringResources.get("log.panel.start"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { viewModel.clearLogcat() },
                    enabled = selectedDevice != null && logLines.isNotEmpty(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(StringResources.get("log.panel.clear"), style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val dialog = FileDialog(Frame(), "Select export directory", FileDialog.LOAD)
                        System.setProperty("apple.awt.fileDialogForDirectories", "true")
                        dialog.isVisible = true
                        System.setProperty("apple.awt.fileDialogForDirectories", "false")

                        val selectedDir = dialog.directory
                        if (selectedDir != null) {
                            val timestamp = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            val deviceSerial = selectedDevice?.serialNumber ?: "unknown"
                            val outputFolder = File(selectedDir, "logs_${deviceSerial}_$timestamp")

                            viewModel.exportLogs(outputFolder) { result ->
                                exportStatus = result
                            }
                        }
                    },
                    enabled = selectedDevice != null && !isExecuting,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(StringResources.get("log.panel.export"), style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { showClearDeviceConfirm = true },
                    enabled = selectedDevice != null && !isExecuting,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(StringResources.get("log.panel.clear.device"), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = filterText,
            onValueChange = { viewModel.setLogFilter(it) },
            label = { Text(StringResources.get("log.panel.filter")) },
            placeholder = { Text(StringResources.get("log.panel.filter.placeholder")) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            singleLine = true,
            enabled = selectedDevice != null,
            trailingIcon = {
                if (filterText.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setLogFilter("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear filter")
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            if (!isLogcatRunning) {
                Button(
                    onClick = { viewModel.startLogcat() },
                    enabled = selectedDevice != null,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(StringResources.get("log.panel.start.stream"))
                }
            } else {
                Button(
                    onClick = { viewModel.stopLogcat() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(StringResources.get("log.panel.stop.stream"))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (isLogcatRunning) {
                    StringResources.get("log.panel.streaming.lines", logLines.size)
                } else {
                    StringResources.get("log.panel.stopped.lines", logLines.size)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
                selectedDevice == null -> EmptyLogMessage(StringResources.get("log.panel.select.device"))
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
                                items(logLines) { line ->
                                    LogLineItem(
                                        line = line,
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

        if (exportStatus.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp, max = 60.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = exportStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exportStatus.contains("成功") || exportStatus.contains("success", true)) {
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
private fun LogLineItem(
    line: String,
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fontSize = if (isFullscreen) 14.sp else 12.sp
    val lineHeight = if (isFullscreen) 18.sp else 16.sp
    val itemHeight = if (isFullscreen) 26.dp else 22.dp

    Text(
        text = line,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = lineHeight,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .height(itemHeight)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
