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
import com.zhang.adbhub.desktop.utils.StringResources
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun SettingsDialog(
    onDismiss: (Boolean) -> Unit
) {
    val viewModel = remember { SettingsViewModel() }
    val customAdbPath by viewModel.customAdbPath.collectAsState()
    val deviceLogPath by viewModel.deviceLogPath.collectAsState()
    val adbVersion by viewModel.adbVersion.collectAsState()
    val detectedPaths by viewModel.detectedPaths.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isAdbAvailable by viewModel.isAdbAvailable.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var showScanConsent by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }

    Dialog(onDismissRequest = { onDismiss(false) }) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .heightIn(min = 600.dp, max = 800.dp),
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
                    text = StringResources.get("settings.title"),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
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
                                text = if (isAdbAvailable) StringResources.get("settings.adb.status.connected") else StringResources.get("settings.adb.status.disconnected"),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isAdbAvailable)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (adbVersion != null) {
                                Text(
                                    text = StringResources.get("settings.version.label", adbVersion!!),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Custom ADB Path
                    Text(
                        text = StringResources.get("settings.custom.adb.path"),
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
                            label = { Text(StringResources.get("settings.adb.path.label")) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val fileDialog = FileDialog(null as Frame?, StringResources.get("settings.choose.adb"), FileDialog.LOAD)
                                fileDialog.isVisible = true
                                val directory = fileDialog.directory
                                val file = fileDialog.file
                                if (directory != null && file != null) {
                                    viewModel.setCustomPath(File(directory, file).absolutePath)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically).height(48.dp)
                        ) {
                            Text(StringResources.get("settings.browse"))
                        }
                    }

                    Text(
                        text = StringResources.get("settings.auto.detect.hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    OutlinedButton(
                        onClick = { showScanConsent = true },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(if (isScanning) StringResources.get("setup.scanning") else StringResources.get("setup.scan.button"))
                    }

                    // Device Log Path
                    Text(
                        text = StringResources.get("settings.device.log.path"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = deviceLogPath,
                        onValueChange = { viewModel.setDeviceLogPath(it) },
                        label = { Text(StringResources.get("settings.device.log.path.label")) },
                        placeholder = { Text(StringResources.get("settings.device.log.path.placeholder")) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        singleLine = true
                    )

                    Text(
                        text = StringResources.get("settings.device.log.path.hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Detected Paths
                    if (detectedPaths.isNotEmpty()) {
                        Text(
                            text = StringResources.get("settings.detected.paths"),
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
                                            Text(StringResources.get("settings.use"))
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
                            Text(StringResources.get("settings.test.adb"), style = MaterialTheme.typography.bodyLarge)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetToDefault() },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text(StringResources.get("settings.reset.default"), style = MaterialTheme.typography.bodyLarge)
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
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDismiss(false) },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(StringResources.get("settings.cancel"))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveConfig()
                            onDismiss(true)
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(StringResources.get("settings.save"))
                    }
                }
            }
        }
    }

    if (showScanConsent) {
        ScanDriveConsentDialog(
            onConfirm = {
                showScanConsent = false
                viewModel.scanAllDrives()
            },
            onDismiss = { showScanConsent = false }
        )
    }
}
