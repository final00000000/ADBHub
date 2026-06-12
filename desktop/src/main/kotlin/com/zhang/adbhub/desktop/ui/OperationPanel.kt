package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
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
        OperationTabBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 内容区
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                OperationTab.PUSH_APK -> PushApkPanel(selectedDevice, viewModel)
                OperationTab.DEVICE_COMMANDS -> VehicleDeviceCommandsPanel(selectedDevice, viewModel)
                OperationTab.APP_MANAGEMENT -> AppManagementPanel(selectedDevice, viewModel)
                OperationTab.FILE_MANAGER -> FileManagerPanel(selectedDevice, viewModel)
                OperationTab.LOGS -> LogManagementPanel(selectedDevice, viewModel)
            }
        }
    }
}

@Composable
private fun OperationTabBar(
    selectedTab: OperationTab,
    onTabSelected: (OperationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.height(76.dp)) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            OperationTabButton(
                selected = selectedTab == OperationTab.PUSH_APK,
                label = StringResources.get("operation.tab.push.apk"),
                icon = Icons.Default.Android,
                onClick = { onTabSelected(OperationTab.PUSH_APK) },
                modifier = Modifier.weight(1f)
            )
            OperationTabButton(
                selected = selectedTab == OperationTab.DEVICE_COMMANDS,
                label = StringResources.get("operation.tab.device.commands"),
                icon = Icons.Default.PhoneAndroid,
                onClick = { onTabSelected(OperationTab.DEVICE_COMMANDS) },
                modifier = Modifier.weight(1f)
            )
            OperationTabButton(
                selected = selectedTab == OperationTab.APP_MANAGEMENT,
                label = StringResources.get("operation.tab.app.management"),
                icon = Icons.Default.Apps,
                onClick = { onTabSelected(OperationTab.APP_MANAGEMENT) },
                modifier = Modifier.weight(1f)
            )
            OperationTabButton(
                selected = selectedTab == OperationTab.FILE_MANAGER,
                label = StringResources.get("operation.tab.file.manager"),
                icon = Icons.Default.FolderOpen,
                onClick = { onTabSelected(OperationTab.FILE_MANAGER) },
                modifier = Modifier.weight(1f)
            )
            OperationTabButton(
                selected = selectedTab == OperationTab.LOGS,
                label = StringResources.get("operation.tab.log.management"),
                icon = Icons.AutoMirrored.Filled.Article,
                onClick = { onTabSelected(OperationTab.LOGS) },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun OperationTabButton(
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
            DeviceRequiredPrompt(
                modifier = Modifier.weight(1f).fillMaxWidth()
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
            DeviceRequiredPrompt(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                description = StringResources.get("log.panel.select.device")
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

private data class DeviceCommandGroup(
    val title: String,
    val actions: List<DeviceCommandAction>
)

private data class DeviceCommandAction(
    val title: String,
    val description: String,
    val commandPreview: String,
    val arguments: List<String>,
    val icon: ImageVector,
    val destructive: Boolean = false
)

@Composable
fun VehicleDeviceCommandsPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    var resultText by remember { mutableStateOf("") }
    var runningCommandTitle by remember { mutableStateOf<String?>(null) }
    val commandScrollState = rememberScrollState()
    val resultScrollState = rememberScrollState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val commandGroups = remember { buildVehicleCommandGroups() }

    LaunchedEffect(selectedDevice?.serialNumber) {
        resultText = ""
        runningCommandTitle = null
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = StringResources.get("operation.device.commands"),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDevice == null) {
            DeviceRequiredPrompt(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                description = StringResources.get("operation.command.connect.first")
            )
            return@Column
        }

        DeviceCommandTargetBar(selectedDevice = selectedDevice)

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .verticalScroll(commandScrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            commandGroups.forEach { group ->
                DeviceCommandGroupSection(
                    group = group,
                    enabled = !isExecuting,
                    runningCommandTitle = runningCommandTitle,
                    onCommandClick = { action ->
                        runningCommandTitle = action.title
                        resultText = StringResources.get(
                            "operation.command.running",
                            action.title,
                            action.commandPreview
                        )
                        viewModel.executeDeviceCommand(action.title, action.arguments) { result ->
                            resultText = StringResources.get(
                                "operation.command.result.format",
                                action.title,
                                action.commandPreview,
                                result
                            )
                            runningCommandTitle = null
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DeviceCommandResultPanel(
            resultText = resultText,
            scrollState = resultScrollState,
            modifier = Modifier.fillMaxWidth().height(176.dp)
        )
    }
}

@Composable
private fun DeviceCommandTargetBar(selectedDevice: Device?) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = if (selectedDevice == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = StringResources.get("operation.target.device.label"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = selectedDevice?.let { it.model ?: it.serialNumber }
                        ?: StringResources.get("operation.select.device.first"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedDevice == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DeviceCommandGroupSection(
    group: DeviceCommandGroup,
    enabled: Boolean,
    runningCommandTitle: String?,
    onCommandClick: (DeviceCommandAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        group.actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowActions.forEach { action ->
                    DeviceCommandButton(
                        action = action,
                        enabled = enabled,
                        isRunning = runningCommandTitle == action.title,
                        onClick = { onCommandClick(action) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f).height(72.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DeviceCommandButton(
    action: DeviceCommandAction,
    enabled: Boolean,
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        colors = if (action.destructive) {
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DeviceCommandResultPanel(
    resultText: String,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                text = StringResources.get("operation.result"),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                SelectionContainer {
                    Text(
                        text = resultText.ifBlank { StringResources.get("operation.command.result.empty") },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (resultText.isBlank()) null else FontFamily.Monospace,
                        color = if (resultText.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

private fun buildVehicleCommandGroups(): List<DeviceCommandGroup> {
    return listOf(
        DeviceCommandGroup(
            title = StringResources.get("operation.command.group.quick"),
            actions = listOf(
                DeviceCommandAction(
                    title = StringResources.get("operation.command.home"),
                    description = StringResources.get("operation.command.home.desc"),
                    commandPreview = "shell input keyevent KEYCODE_HOME",
                    arguments = listOf("shell", "input", "keyevent", "KEYCODE_HOME"),
                    icon = Icons.Default.Home
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.back"),
                    description = StringResources.get("operation.command.back.desc"),
                    commandPreview = "shell input keyevent KEYCODE_BACK",
                    arguments = listOf("shell", "input", "keyevent", "KEYCODE_BACK"),
                    icon = Icons.AutoMirrored.Filled.ArrowBack
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.recent"),
                    description = StringResources.get("operation.command.recent.desc"),
                    commandPreview = "shell input keyevent KEYCODE_APP_SWITCH",
                    arguments = listOf("shell", "input", "keyevent", "KEYCODE_APP_SWITCH"),
                    icon = Icons.Default.Apps
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.power"),
                    description = StringResources.get("operation.command.power.desc"),
                    commandPreview = "shell input keyevent KEYCODE_POWER",
                    arguments = listOf("shell", "input", "keyevent", "KEYCODE_POWER"),
                    icon = Icons.Default.PowerSettingsNew
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.volume.up"),
                    description = StringResources.get("operation.command.volume.up.desc"),
                    commandPreview = "shell input keyevent KEYCODE_VOLUME_UP",
                    arguments = listOf("shell", "input", "keyevent", "KEYCODE_VOLUME_UP"),
                    icon = Icons.AutoMirrored.Filled.VolumeUp
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.volume.down"),
                    description = StringResources.get("operation.command.volume.down.desc"),
                    commandPreview = "shell input keyevent KEYCODE_VOLUME_DOWN",
                    arguments = listOf("shell", "input", "keyevent", "KEYCODE_VOLUME_DOWN"),
                    icon = Icons.AutoMirrored.Filled.VolumeDown
                )
            )
        ),
        DeviceCommandGroup(
            title = StringResources.get("operation.command.group.diagnostics"),
            actions = listOf(
                DeviceCommandAction(
                    title = StringResources.get("operation.command.vehicle.info"),
                    description = StringResources.get("operation.command.vehicle.info.desc"),
                    commandPreview = "shell getprop product/build info",
                    arguments = listOf(
                        "shell",
                        "sh",
                        "-c",
                        "printf 'Model: '; getprop ro.product.model; printf 'Brand: '; getprop ro.product.brand; printf 'Android: '; getprop ro.build.version.release; printf 'Build: '; getprop ro.build.display.id"
                    ),
                    icon = Icons.Default.Info
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.display.info"),
                    description = StringResources.get("operation.command.display.info.desc"),
                    commandPreview = "shell wm size; wm density",
                    arguments = listOf("shell", "sh", "-c", "wm size; wm density"),
                    icon = Icons.Default.Build
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.current.focus"),
                    description = StringResources.get("operation.command.current.focus.desc"),
                    commandPreview = "shell dumpsys window focus",
                    arguments = listOf(
                        "shell",
                        "sh",
                        "-c",
                        "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp|mFocusedWindow'"
                    ),
                    icon = Icons.Default.BugReport
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.screenshot"),
                    description = StringResources.get("operation.command.screenshot.desc"),
                    commandPreview = "shell screencap -p /sdcard/Pictures/adbhub_screen.png",
                    arguments = listOf(
                        "shell",
                        "sh",
                        "-c",
                        "mkdir -p /sdcard/Pictures; screencap -p /sdcard/Pictures/adbhub_screen.png; echo saved:/sdcard/Pictures/adbhub_screen.png"
                    ),
                    icon = Icons.Default.CameraAlt
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.open.settings"),
                    description = StringResources.get("operation.command.open.settings.desc"),
                    commandPreview = "shell am start -a android.settings.SETTINGS",
                    arguments = listOf("shell", "am", "start", "-a", "android.settings.SETTINGS"),
                    icon = Icons.Default.Settings
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.clear.logcat"),
                    description = StringResources.get("operation.command.clear.logcat.desc"),
                    commandPreview = "logcat -c",
                    arguments = listOf("logcat", "-c"),
                    icon = Icons.Default.DeleteSweep,
                    destructive = true
                )
            )
        ),
        DeviceCommandGroup(
            title = StringResources.get("operation.command.group.maintenance"),
            actions = listOf(
                DeviceCommandAction(
                    title = StringResources.get("operation.command.root"),
                    description = StringResources.get("operation.command.root.desc"),
                    commandPreview = "root",
                    arguments = listOf("root"),
                    icon = Icons.Default.Security
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.remount"),
                    description = StringResources.get("operation.command.remount.desc"),
                    commandPreview = "remount",
                    arguments = listOf("remount"),
                    icon = Icons.Default.Build
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.disable.verity"),
                    description = StringResources.get("operation.command.disable.verity.desc"),
                    commandPreview = "disable-verity",
                    arguments = listOf("disable-verity"),
                    icon = Icons.Default.Security,
                    destructive = true
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.enable.verity"),
                    description = StringResources.get("operation.command.enable.verity.desc"),
                    commandPreview = "enable-verity",
                    arguments = listOf("enable-verity"),
                    icon = Icons.Default.Security
                )
            )
        ),
        DeviceCommandGroup(
            title = StringResources.get("operation.command.group.reboot"),
            actions = listOf(
                DeviceCommandAction(
                    title = StringResources.get("operation.command.reboot"),
                    description = StringResources.get("operation.command.reboot.desc"),
                    commandPreview = "reboot",
                    arguments = listOf("reboot"),
                    icon = Icons.Default.RestartAlt,
                    destructive = true
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.recovery"),
                    description = StringResources.get("operation.command.recovery.desc"),
                    commandPreview = "reboot recovery",
                    arguments = listOf("reboot", "recovery"),
                    icon = Icons.Default.RestartAlt,
                    destructive = true
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.bootloader"),
                    description = StringResources.get("operation.command.bootloader.desc"),
                    commandPreview = "reboot bootloader",
                    arguments = listOf("reboot", "bootloader"),
                    icon = Icons.Default.RestartAlt,
                    destructive = true
                )
            )
        )
    )
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
            DeviceRequiredPrompt(
                modifier = Modifier.fillMaxWidth().height(220.dp)
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

        // 回到桌面
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.go.home"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = StringResources.get("operation.go.home.description"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.goHome { result ->
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
                        Text(StringResources.get("operation.execute.go.home"))
                    }
                }
            }
        }

        // 启用 dm-verity
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.enable.verity"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = StringResources.get("operation.enable.verity.description"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.enableVerity { result ->
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
                        Text(StringResources.get("operation.execute.enable.verity"))
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
            DeviceRequiredPrompt(
                modifier = Modifier.fillMaxWidth().height(220.dp)
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
