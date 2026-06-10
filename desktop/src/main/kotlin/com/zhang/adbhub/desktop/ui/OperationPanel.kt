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
                text = { Text("Push APK", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.DEVICE_COMMANDS,
                onClick = { onTabSelected(OperationTab.DEVICE_COMMANDS) },
                text = { Text("设备操作", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.APP_MANAGEMENT,
                onClick = { onTabSelected(OperationTab.APP_MANAGEMENT) },
                text = { Text("应用管理", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.FILE_MANAGER,
                onClick = { onTabSelected(OperationTab.FILE_MANAGER) },
                text = { Text("文件管理", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == OperationTab.LOGS,
                onClick = { onTabSelected(OperationTab.LOGS) },
                text = { Text("日志管理", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 内容区
        when (selectedTab) {
            OperationTab.PUSH_APK -> PushApkPanel(selectedDevice, viewModel)
            OperationTab.DEVICE_COMMANDS -> DeviceCommandsPanel(selectedDevice, viewModel)
            OperationTab.APP_MANAGEMENT -> AppManagementPanel(selectedDevice, viewModel)
            OperationTab.FILE_MANAGER -> FileManagerPanel(selectedDevice, viewModel)
            OperationTab.LOGS -> LogManagementPanel(selectedDevice, viewModel)
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
            text = "推送 APK 到设备",
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = "请先选择一个设备",
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = "目标设备: ${selectedDevice.model ?: selectedDevice.serialNumber}",
            style = MaterialTheme.typography.bodyMedium
        )

        // 文件选择
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "选择的 APK:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedFile?.absolutePath ?: "未选择文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Button(
            onClick = {
                val dialog = FileDialog(Frame(), "选择 APK 文件", FileDialog.LOAD)
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
            Text("选择 APK 文件")
        }

        // 目标路径输入
        Text(
            text = "目标路径:",
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedTextField(
            value = targetPath,
            onValueChange = { targetPath = it },
            label = { Text("设备上的目标路径") },
            placeholder = { Text("例如: /system/app/") },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            singleLine = true,
            enabled = !isExecuting
        )

        // 常用路径快捷选择
        Text(
            text = "常用路径:",
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
                    Text("推送中...")
                }
            } else {
                Text("推送到设备")
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
                    text = "等待执行...",
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
            text = "日志管理",
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = "请先选择一个设备",
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = "日志查看和导出功能在右侧面板中",
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
            text = "设备操作命令",
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = "请先选择一个设备",
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = "目标设备: ${selectedDevice.model ?: selectedDevice.serialNumber}",
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
                    text = "Root 权限",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "以 root 权限重启 ADB 守护进程",
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
                            Text("执行中...")
                        }
                    } else {
                        Text("执行 adb root")
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
                    text = "重新挂载分区",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将系统分区重新挂载为可写模式",
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
                            Text("执行中...")
                        }
                    } else {
                        Text("执行 adb remount")
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
                    text = "设备重启",
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
                        Text("重启设备")
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
                    text = "执行结果",
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
                        text = "等待执行...",
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
            text = "应用管理",
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = "请先选择一个设备",
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = "目标设备: ${selectedDevice.model ?: selectedDevice.serialNumber}",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        // 包名输入
        OutlinedTextField(
            value = packageName,
            onValueChange = { packageName = it },
            label = { Text("应用包名") },
            placeholder = { Text("例如: com.example.app") },
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
                    text = "启动应用",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    label = { Text("Activity 名称") },
                    placeholder = { Text("例如: .MainActivity") },
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
                            resultText = "请输入包名和 Activity 名称"
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
                            Text("执行中...")
                        }
                    } else {
                        Text("启动应用")
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
                    text = "应用信息",
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
                            resultText = "请输入包名"
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
                            Text("查询中...")
                        }
                    } else {
                        Text("查看应用信息")
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
                    text = "应用操作",
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
                                resultText = "请输入包名"
                            }
                        },
                        enabled = packageName.isNotBlank() && !isExecuting,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("停止应用")
                    }
                    Button(
                        onClick = {
                            if (packageName.isNotBlank()) {
                                viewModel.clearAppData(packageName) { result ->
                                    resultText = result
                                }
                            } else {
                                resultText = "请输入包名"
                            }
                        },
                        enabled = packageName.isNotBlank() && !isExecuting,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清除数据")
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
                    text = "执行结果",
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
                            text = "等待执行...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
