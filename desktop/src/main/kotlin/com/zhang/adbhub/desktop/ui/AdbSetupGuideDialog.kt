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
import com.zhang.adbhub.common.config.AdbPathDetector
import com.zhang.adbhub.desktop.viewmodel.SettingsViewModel
import com.zhang.adbhub.desktop.utils.StringResources
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
    val isScanning by viewModel.isScanning.collectAsState()

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
                    text = StringResources.get("setup.title"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = StringResources.get("setup.description"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Manual selection section (优先显示)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = StringResources.get("setup.manual_config"),
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
                                    label = { Text(StringResources.get("setup.adb_path_label")) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        val fileDialog = FileDialog(null as Frame?, StringResources.get("setup.adb_path_label"), FileDialog.LOAD)
                                        fileDialog.isVisible = true
                                        val directory = fileDialog.directory
                                        val file = fileDialog.file
                                        if (directory != null && file != null) {
                                            viewModel.setCustomPath(File(directory, file).absolutePath)
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text(StringResources.get("settings.browse"))
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
                                    Text(StringResources.get("setup.use_path"))
                                }
                            }
                        }
                    }

                    // Scan button
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = StringResources.get("setup.auto_scan"),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = StringResources.get("setup.scan_description"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = { viewModel.scanAllDrives() },
                                enabled = !isScanning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isScanning) StringResources.get("setup.scanning") else StringResources.get("setup.scan_button"))
                            }
                        }
                    }

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
                                    text = StringResources.get("setup.detected_paths"),
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
                                            Text(StringResources.get("setup.select_path"))
                                        }
                                    }
                                    if (path != detectedPaths.last()) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
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
                                text = StringResources.get("setup.install_guide"),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = StringResources.get("setup.install_description"),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = StringResources.get("setup.download_guide"),
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
                        Text(StringResources.get("setup.later"))
                    }
                }
            }
        }
    }
}
