package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.desktop.utils.StringResources
import com.zhang.adbhub.desktop.viewmodel.MainViewModel
import com.zhang.adbhub.desktop.viewmodel.OperationTab
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun OperationPanel(
    selectedDevice: Device?,
    selectedTab: OperationTab,
    onTabSelected: (OperationTab) -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // Tab 选择
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == OperationTab.PUSH_APK,
                onClick = { onTabSelected(OperationTab.PUSH_APK) },
                text = { Text(StringResources.get("operation.push.apk"), style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.DEVICE_COMMANDS,
                onClick = { onTabSelected(OperationTab.DEVICE_COMMANDS) },
                text = { Text(StringResources.get("operation.device.commands"), style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.APP_MANAGEMENT,
                onClick = { onTabSelected(OperationTab.APP_MANAGEMENT) },
                text = { Text(StringResources.get("operation.app.management"), style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.FILE_MANAGER,
                onClick = { onTabSelected(OperationTab.FILE_MANAGER) },
                text = { Text(StringResources.get("file.manager.title"), style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.LOGS,
                onClick = { onTabSelected(OperationTab.LOGS) },
                text = { Text(StringResources.get("operation.log.management"), style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 内容区
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                OperationTab.PUSH_APK -> PushApkPanel(selectedDevice, viewModel)
                OperationTab.DEVICE_COMMANDS -> DeviceCommandsPanel(selectedDevice, viewModel)
                OperationTab.APP_MANAGEMENT -> AppManagementPanel(selectedDevice, viewModel)
                OperationTab.FILE_MANAGER -> FileManagerPanel(selectedDevice, viewModel)
                OperationTab.LOGS -> LogManagementPanel(selectedDevice, viewModel)
            }
        }
    }
}

@Composable
fun PushApkPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var targetPath by remember {
        val config = com.zhang.adbhub.common.config.AdbConfig.load()
        mutableStateOf(config.pushTargetPath ?: "/system/app/")
    }
    var statusText by remember { mutableStateOf("") }
    val isExecuting by viewModel.isExecuting.collectAsState()

    // Common target paths
    val commonPaths = listOf(
        "/system/app/",
        "/system/priv-app/",
        "/data/app/",
        "/sdcard/",
        "/data/local/tmp/"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = StringResources.get("operation.push.title"),
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = StringResources.get("operation.select.device.first"),
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = StringResources.get("operation.target.device", selectedDevice.model ?: selectedDevice.serialNumber),
            style = MaterialTheme.typography.bodyMedium
        )

        // 文件选择
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.selected.apk"),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedFile?.absolutePath ?: StringResources.get("operation.no.file.selected"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Button(
            onClick = {
                val dialog = FileDialog(Frame(), StringResources.get("operation.select.apk"), FileDialog.LOAD)
                dialog.file = "*.apk"
                dialog.isVisible = true
                val file = dialog.file
                val dir = dialog.directory
                if (file != null && dir != null) {
                    selectedFile = File(dir, file)
                }
            },
            enabled = !isExecuting,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(StringResources.get("operation.select.apk"))
        }

        // 目标路径输入
        Text(
            text = StringResources.get("operation.target.path"),
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedTextField(
            value = targetPath,
            onValueChange = { targetPath = it },
            label = { Text(StringResources.get("operation.device.path")) },
            placeholder = { Text(StringResources.get("operation.path.example")) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            singleLine = true,
            enabled = !isExecuting
        )

        // 常用路径快捷选择
        Text(
            text = StringResources.get("operation.common.paths"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonPaths.take(3).forEach { path ->
                OutlinedButton(
                    onClick = { targetPath = path },
                    enabled = !isExecuting,
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(
                        text = path.substringAfterLast('/').substringBeforeLast('/').ifEmpty { "root" },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonPaths.drop(3).forEach { path ->
                OutlinedButton(
                    onClick = { targetPath = path },
                    enabled = !isExecuting,
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(
                        text = path.substringAfterLast('/').substringBeforeLast('/').ifEmpty { "sdcard" },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }

        Button(
            onClick = {
                selectedFile?.let { file ->
                    viewModel.pushApk(file, targetPath) { status ->
                        statusText = status
                    }
                }
            },
            enabled = selectedFile != null && targetPath.isNotBlank() && !isExecuting,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (isExecuting) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(StringResources.get("operation.pushing"))
                }
            } else {
                Text(StringResources.get("operation.push.to.device"))
            }
        }

        // 固定高度的结果显示区，防止 UI 跳动
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = StringResources.get("operation.waiting"),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LogManagementPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = StringResources.get("operation.log.management"),
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = StringResources.get("operation.select.device.first"),
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = StringResources.get("operation.log.view.hint"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DeviceCommandsPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    var resultText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val isExecuting by viewModel.isExecuting.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = StringResources.get("operation.device.commands"),
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = StringResources.get("operation.select.device.first"),
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = StringResources.get("operation.target.device", selectedDevice.model ?: selectedDevice.serialNumber),
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        // Root 命令
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.root.permission"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = StringResources.get("operation.root.description"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.executeRoot { result ->
                            resultText = result
                        }
                    },
                    enabled = !isExecuting,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    if (isExecuting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(StringResources.get("operation.executing"))
                        }
                    } else {
                        Text(StringResources.get("operation.execute.root"))
                    }
                }
            }
        }

        // Remount 命令
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.remount.title"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = StringResources.get("operation.remount.description"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.executeRemount { result ->
                            resultText = result
                        }
                    },
                    enabled = !isExecuting,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    if (isExecuting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(StringResources.get("operation.executing"))
                        }
                    } else {
                        Text(StringResources.get("operation.execute.remount"))
                    }
                }
            }
        }

        // 重启命令
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.device.reboot"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.rebootDevice { result ->
                                resultText = result
                            }
                        },
                        enabled = !isExecuting,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(StringResources.get("operation.reboot"))
                    }
                    Button(
                        onClick = {
                            viewModel.rebootRecovery { result ->
                                resultText = result
                            }
                        },
                        enabled = !isExecuting,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Recovery")
                    }
                    Button(
                        onClick = {
                            viewModel.rebootBootloader { result ->
                                resultText = result
                            }
                        },
                        enabled = !isExecuting,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Bootloader")
                    }
                }
            }
        }

        // 结果显示 - 固定高度防止跳动
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.result"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (resultText.isNotEmpty()) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                } else {
                    Text(
                        text = StringResources.get("operation.waiting"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AppManagementPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    var packageName by remember { mutableStateOf("") }
    var activityName by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val isExecuting by viewModel.isExecuting.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = StringResources.get("operation.app.management"),
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = StringResources.get("operation.select.device.first"),
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = StringResources.get("operation.target.device", selectedDevice.model ?: selectedDevice.serialNumber),
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        // 包名输入
        OutlinedTextField(
            value = packageName,
            onValueChange = { packageName = it },
            label = { Text(StringResources.get("operation.package.name")) },
            placeholder = { Text(StringResources.get("operation.package.example")) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            singleLine = true
        )

        // 启动应用
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.start.app"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    label = { Text(StringResources.get("operation.activity.name")) },
                    placeholder = { Text(StringResources.get("operation.activity.example")) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    singleLine = true,
                    enabled = !isExecuting
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (packageName.isNotBlank() && activityName.isNotBlank()) {
                            viewModel.startApp(packageName, activityName) { result ->
                                resultText = result
                            }
                        } else {
                            resultText = StringResources.get("operation.input.package.activity")
                        }
                    },
                    enabled = packageName.isNotBlank() && activityName.isNotBlank() && !isExecuting,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    if (isExecuting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(StringResources.get("operation.executing"))
                        }
                    } else {
                        Text(StringResources.get("operation.start.app"))
                    }
                }
            }
        }

        // 应用信息
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.app.info"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (packageName.isNotBlank()) {
                            viewModel.getAppInfo(packageName) { result ->
                                resultText = result
                            }
                        } else {
                            resultText = StringResources.get("operation.input.package")
                        }
                    },
                    enabled = packageName.isNotBlank() && !isExecuting,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    if (isExecuting) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(StringResources.get("operation.querying"))
                        }
                    } else {
                        Text(StringResources.get("operation.view.app.info"))
                    }
                }
            }
        }

        // 应用操作
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.app.operations"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (packageName.isNotBlank()) {
                                viewModel.stopApp(packageName) { result ->
                                    resultText = result
                                }
                            } else {
                                resultText = StringResources.get("operation.input.package")
                            }
                        },
                        enabled = packageName.isNotBlank() && !isExecuting,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(StringResources.get("operation.stop.app"))
                    }
                    Button(
                        onClick = {
                            if (packageName.isNotBlank()) {
                                viewModel.clearAppData(packageName) { result ->
                                    resultText = result
                                }
                            } else {
                                resultText = StringResources.get("operation.input.package")
                            }
                        },
                        enabled = packageName.isNotBlank() && !isExecuting,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(StringResources.get("operation.clear.data"))
                    }
                }
            }
        }

        // 结果显示 - 固定高度防止跳动
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.result"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    if (resultText.isNotEmpty()) {
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    } else {
                        Text(
                            text = StringResources.get("operation.waiting"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
