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
    Column(modifier = modifier.padding(8.dp)) {
        // Tab 选择
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == OperationTab.PUSH_APK,
                onClick = { onTabSelected(OperationTab.PUSH_APK) },
                text = { Text("Push APK") },
                icon = { Icon(Icons.Default.Android, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == OperationTab.DEVICE_COMMANDS,
                onClick = { onTabSelected(OperationTab.DEVICE_COMMANDS) },
                text = { Text("设备操作") },
                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == OperationTab.APP_MANAGEMENT,
                onClick = { onTabSelected(OperationTab.APP_MANAGEMENT) },
                text = { Text("应用管理") },
                icon = { Icon(Icons.Default.Apps, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == OperationTab.LOGS,
                onClick = { onTabSelected(OperationTab.LOGS) },
                text = { Text("日志管理") },
                icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 内容区
        when (selectedTab) {
            OperationTab.PUSH_APK -> PushApkPanel(selectedDevice, viewModel)
            OperationTab.DEVICE_COMMANDS -> DeviceCommandsPanel(selectedDevice, viewModel)
            OperationTab.APP_MANAGEMENT -> AppManagementPanel(selectedDevice, viewModel)
            OperationTab.LOGS -> LogManagementPanel(selectedDevice, viewModel)
        }
    }
}

@Composable
fun PushApkPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var statusText by remember { mutableStateOf("") }

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
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "选择的 APK:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedFile?.absolutePath ?: "未选择文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        ) {
            Text("选择 APK 文件")
        }

        Button(
            onClick = {
                selectedFile?.let { file ->
                    viewModel.pushApk(file) { status ->
                        statusText = status
                    }
                }
            },
            enabled = selectedFile != null
        ) {
            Text("推送到设备")
        }

        if (statusText.isNotEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
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
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                    }
                ) {
                    Text("执行 adb root")
                }
            }
        }

        // Remount 命令
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                    }
                ) {
                    Text("执行 adb remount")
                }
            }
        }

        // 重启命令
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                        }
                    ) {
                        Text("重启设备")
                    }
                    Button(
                        onClick = {
                            viewModel.rebootRecovery { result ->
                                resultText = result
                            }
                        }
                    ) {
                        Text("Recovery")
                    }
                    Button(
                        onClick = {
                            viewModel.rebootBootloader { result ->
                                resultText = result
                            }
                        }
                    ) {
                        Text("Bootloader")
                    }
                }
            }
        }

        // 结果显示
        if (resultText.isNotEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "执行结果",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodySmall
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
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
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 启动应用
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                    enabled = packageName.isNotBlank() && activityName.isNotBlank()
                ) {
                    Text("启动应用")
                }
            }
        }

        // 应用信息
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                    enabled = packageName.isNotBlank()
                ) {
                    Text("查看应用信息")
                }
            }
        }

        // 应用操作
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                        enabled = packageName.isNotBlank()
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
                        enabled = packageName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("清除数据")
                    }
                }
            }
        }

        // 结果显示
        if (resultText.isNotEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "执行结果",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}
