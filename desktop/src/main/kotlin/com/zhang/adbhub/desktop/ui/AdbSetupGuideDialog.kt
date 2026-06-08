package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zhang.adbhub.desktop.viewmodel.SettingsViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun AdbSetupGuideDialog(
    onDismiss: () -> Unit,
    onAdbConfigured: () -> Unit
) {
    val viewModel = remember { SettingsViewModel() }
    val detectedPaths by viewModel.detectedPaths.collectAsState()
    val customAdbPath by viewModel.customAdbPath.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(650.dp)
                .heightIn(max = 750.dp),
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
                    text = "未检测到 ADB 工具",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "ADBHub 需要 Android Debug Bridge (ADB) 工具才能工作。请配置 ADB 路径以继续。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Detected paths section
                    if (detectedPaths.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "检测到可能的 ADB 路径",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                detectedPaths.forEach { path ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = path,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.setCustomPath(path)
                                                viewModel.saveConfig()
                                                onAdbConfigured()
                                            }
                                        ) {
                                            Text("选择此路径")
                                        }
                                    }
                                    if (path != detectedPaths.last()) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Manual selection section
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "手动选择 ADB 路径",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customAdbPath,
                                    onValueChange = { viewModel.setCustomPath(it) },
                                    label = { Text("ADB 可执行文件路径") },
                                    modifier = Modifier.weight(1f),
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
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text("浏览")
                                }
                            }

                            if (customAdbPath.isNotBlank()) {
                                Button(
                                    onClick = {
                                        viewModel.saveConfig()
                                        onAdbConfigured()
                                    },
                                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                                ) {
                                    Text("使用此路径")
                                }
                            }
                        }
                    }

                    // Setup guide section
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "如何安装 ADB",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "ADB 通常包含在 Android SDK Platform Tools 中。",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "常见路径：",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "• Windows: %LOCALAPPDATA%\\Android\\Sdk\\platform-tools\\adb.exe\n" +
                                        "• Windows: C:\\Android\\Sdk\\platform-tools\\adb.exe\n" +
                                        "• macOS: ~/Library/Android/sdk/platform-tools/adb\n" +
                                        "• Linux: ~/Android/Sdk/platform-tools/adb",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                            )

                            Text(
                                text = "如果您没有安装 Android SDK，可以从 Google 官网下载 SDK Platform Tools：\n" +
                                        "https://developer.android.com/studio/releases/platform-tools",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status message
                    statusMessage?.let { message ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Bottom button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("稍后配置")
                    }
                }
            }
        }
    }
}
