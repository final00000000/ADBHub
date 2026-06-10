package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zhang.adbhub.desktop.viewmodel.SettingsViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val viewModel = remember { SettingsViewModel() }
    val customAdbPath by viewModel.customAdbPath.collectAsState()
    val deviceLogPath by viewModel.deviceLogPath.collectAsState()
    val adbVersion by viewModel.adbVersion.collectAsState()
    val detectedPaths by viewModel.detectedPaths.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 700.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = "ADB 设置",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ADB Status
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAdbAvailable)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isAdbAvailable) "ADB 状态：已连接" else "ADB 状态：未连接",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isAdbAvailable)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (adbVersion != null) {
                                Text(
                                    text = "版本: $adbVersion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Custom ADB Path
                    Text(
                        text = "自定义 ADB 路径",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customAdbPath,
                            onValueChange = { viewModel.setCustomPath(it) },
                            label = { Text("ADB 可执行文件路径") },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val fileDialog = FileDialog(null as Frame?, "选择 ADB 可执行文件", FileDialog.LOAD)
                                fileDialog.isVisible = true
                                val directory = fileDialog.directory
                                val file = fileDialog.file
                                if (directory != null && file != null) {
                                    viewModel.setCustomPath(File(directory, file).absolutePath)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically).height(48.dp)
                        ) {
                            Text("浏览")
                        }
                    }

                    Text(
                        text = "留空则自动检测系统中的 ADB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Device Log Path
                    Text(
                        text = "设备日志路径",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = deviceLogPath,
                        onValueChange = { viewModel.setDeviceLogPath(it) },
                        label = { Text("设备上的日志目录路径") },
                        placeholder = { Text("例如: /data/logs/ 或 /sdcard/logs/") },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        singleLine = true
                    )

                    Text(
                        text = "留空则使用默认路径 /sdcard/（通用 Android 路径）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Detected Paths
                    if (detectedPaths.isNotEmpty()) {
                        Text(
                            text = "检测到的 ADB 路径",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                detectedPaths.forEach { path ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = path,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { viewModel.setCustomPath(path) }) {
                                            Text("使用")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons - 增加高度并移除固定height，让按钮自适应
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testConnection() },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text("测试 ADB", style = MaterialTheme.typography.bodyLarge)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetToDefault() },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text("重置默认", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    // Status Message
                    Card(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = statusMessage ?: " ",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3
                        )
                    }
                }

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveConfig()
                            onDismiss()
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
