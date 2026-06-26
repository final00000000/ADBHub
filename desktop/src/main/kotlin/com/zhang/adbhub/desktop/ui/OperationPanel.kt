package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.AdbResult
import com.zhang.adbhub.desktop.utils.StringResources
import com.zhang.adbhub.desktop.viewmodel.MainViewModel
import com.zhang.adbhub.desktop.viewmodel.OperationTab
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import org.jetbrains.skia.Image as SkiaImage

private const val SCREENSHOT_FILE_PREFIX = "adbhub_screenshot"
private const val SCREENSHOT_FILE_EXTENSION = "png"
private const val SCREENSHOT_ZOOM_STEP = 0.25f
private const val SCREENSHOT_MIN_ZOOM = 0.5f
private const val SCREENSHOT_MAX_ZOOM = 3f

@Composable
fun OperationPanel(
    selectedDevice: Device?,
    selectedTab: OperationTab,
    onTabSelected: (OperationTab) -> Unit,
    viewModel: MainViewModel,
    toastState: ToastState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        OperationTabBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 内容区 - 使用 key 防止 Tab 切换时组件销毁/重建
        // 优化: 只创建当前选中的 Tab 面板，避免不必要的初始化
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                OperationTab.PUSH_APK -> PushApkPanel(selectedDevice, viewModel)
                OperationTab.DEVICE_COMMANDS -> VehicleDeviceCommandsPanel(selectedDevice, viewModel, toastState)
                OperationTab.APP_MANAGEMENT -> AppManagementPanel(selectedDevice, viewModel)
                OperationTab.FILE_MANAGER -> {} // 暂时隐藏文件管理器
                OperationTab.LOGS -> VehicleDeviceCommandsPanel(selectedDevice, viewModel, toastState)
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
            // 文件管理器标签暂时隐藏，留更多空间给日志面板
            /*
            OperationTabButton(
                selected = selectedTab == OperationTab.FILE_MANAGER,
                label = StringResources.get("operation.tab.file.manager"),
                icon = Icons.Default.FolderOpen,
                onClick = { onTabSelected(OperationTab.FILE_MANAGER) },
                modifier = Modifier.weight(1f)
            )
            */
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
        mutableStateOf(config.pushTargetPath ?: "")
    }
    var statusText by remember { mutableStateOf("") }
    val isExecuting by viewModel.isExecuting.collectAsState()

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

private enum class DeviceCommandType {
    RAW,
    VEHICLE_INFO,
    DISPLAY_INFO,
    CURRENT_FOCUS,
    SCREENSHOT,
    ROOT_RESTART  // 会导致设备重启 ADB daemon 的命令
}

private data class DeviceCommandAction(
    val title: String,
    val description: String,
    val commandPreview: String,
    val arguments: List<String>,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val commandType: DeviceCommandType = DeviceCommandType.RAW,
    val id: String = commandPreview
)

private data class DeviceCommandResult(
    val title: String,
    val commandPreview: String,
    val output: String,
    val commandType: DeviceCommandType,
    val running: Boolean = false
)

@Composable
fun VehicleDeviceCommandsPanel(
    selectedDevice: Device?,
    viewModel: MainViewModel,
    toastState: ToastState
) {
    val resultState = remember { mutableStateOf<DeviceCommandResult?>(null) }
    val runningCommandIdState = remember { mutableStateOf<String?>(null) }
    var screenshotPreviewFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedDevice?.serialNumber) {
        resultState.value = null
        runningCommandIdState.value = null
        screenshotPreviewFile?.delete()
        screenshotPreviewFile = null
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

        DeviceCommandList(
            viewModel = viewModel,
            resultState = resultState,
            runningCommandIdState = runningCommandIdState,
            toastState = toastState,
            onScreenshotReady = { screenshotPreviewFile = it },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        )

        Spacer(modifier = Modifier.height(12.dp))

        DeviceCommandResultPanel(
            resultState = resultState,
            modifier = Modifier.fillMaxWidth().height(176.dp)
        )
    }

    screenshotPreviewFile?.let { file ->
        ScreenshotPreviewDialog(
            imageFile = file,
            onDismiss = {
                file.delete()
                screenshotPreviewFile = null
            }
        )
    }
}

@Composable
private fun DeviceCommandList(
    viewModel: MainViewModel,
    resultState: MutableState<DeviceCommandResult?>,
    runningCommandIdState: MutableState<String?>,
    toastState: ToastState,
    onScreenshotReady: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val commandListState = rememberLazyListState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val runningCommandId = runningCommandIdState.value
    val commandGroups = remember(Unit) { buildVehicleCommandGroups() }
    val allCommands = remember(Unit) { commandGroups.flatMap { it.actions } }
    val scope = rememberCoroutineScope()
    var favoriteCommandIds by remember {
        mutableStateOf(com.zhang.adbhub.common.config.AdbConfig.load().favoriteCommandIds)
    }
    val favoriteCommands = remember(allCommands, favoriteCommandIds) {
        val actionsById = allCommands.associateBy { it.id }
        favoriteCommandIds.mapNotNull { actionsById[it] }
    }

    fun toggleFavorite(action: DeviceCommandAction) {
        val updated = if (favoriteCommandIds.contains(action.id)) {
            favoriteCommandIds.filterNot { it == action.id }
        } else {
            favoriteCommandIds + action.id
        }
        favoriteCommandIds = updated
        val config = com.zhang.adbhub.common.config.AdbConfig.load()
        com.zhang.adbhub.common.config.AdbConfig.save(config.copy(favoriteCommandIds = updated))
    }

    fun executeAction(action: DeviceCommandAction) {
        if (isExecuting) return

        runningCommandIdState.value = action.id
        resultState.value = DeviceCommandResult(
            title = action.title,
            commandPreview = action.commandPreview,
            output = StringResources.get("operation.executing"),
            commandType = action.commandType,
            running = true
        )

        if (action.commandType == DeviceCommandType.SCREENSHOT) {
            val localFile = createTemporaryScreenshotFile()
            viewModel.captureScreenshot(action.title, localFile) { result ->
                when (result) {
                    is AdbResult.Success -> {
                        resultState.value = DeviceCommandResult(
                            title = action.title,
                            commandPreview = action.commandPreview,
                            output = StringResources.get("operation.command.screenshot.preview.ready", result.data.absolutePath),
                            commandType = action.commandType
                        )
                        onScreenshotReady(result.data)
                        scope.launch {
                            toastState.showToast(
                                message = StringResources.get("operation.command.success", action.title),
                                type = ToastType.SUCCESS
                            )
                        }
                    }
                    is AdbResult.Error -> {
                        resultState.value = DeviceCommandResult(
                            title = action.title,
                            commandPreview = action.commandPreview,
                            output = result.message,
                            commandType = action.commandType
                        )
                        scope.launch {
                            toastState.showToast(
                                message = StringResources.get("operation.command.failed", action.title),
                                type = ToastType.ERROR
                            )
                        }
                    }
                }
                runningCommandIdState.value = null
            }
        } else if (action.commandType == DeviceCommandType.ROOT_RESTART) {
            // 使用专门的 root/remount 方法
            when (action.commandPreview) {
                "root" -> {
                    viewModel.executeRoot { result ->
                        resultState.value = DeviceCommandResult(
                            title = action.title,
                            commandPreview = action.commandPreview,
                            output = result,
                            commandType = action.commandType
                        )
                        runningCommandIdState.value = null

                        val isSuccess = !result.contains("失败", ignoreCase = true) &&
                                       !result.contains("failed", ignoreCase = true) &&
                                       !result.contains("error", ignoreCase = true)
                        scope.launch {
                            toastState.showToast(
                                message = StringResources.get(
                                    if (isSuccess) "operation.command.success" else "operation.command.failed",
                                    action.title
                                ),
                                type = if (isSuccess) ToastType.SUCCESS else ToastType.ERROR
                            )
                        }
                    }
                }
                "remount" -> {
                    viewModel.executeRemount { result ->
                        resultState.value = DeviceCommandResult(
                            title = action.title,
                            commandPreview = action.commandPreview,
                            output = result,
                            commandType = action.commandType
                        )
                        runningCommandIdState.value = null

                        val isSuccess = !result.contains("失败", ignoreCase = true) &&
                                       !result.contains("failed", ignoreCase = true) &&
                                       !result.contains("error", ignoreCase = true)
                        scope.launch {
                            toastState.showToast(
                                message = StringResources.get(
                                    if (isSuccess) "operation.command.success" else "operation.command.failed",
                                    action.title
                                ),
                                type = if (isSuccess) ToastType.SUCCESS else ToastType.ERROR
                            )
                        }
                    }
                }
                else -> {
                    viewModel.executeDeviceCommand(action.title, action.arguments) { result ->
                        resultState.value = DeviceCommandResult(
                            title = action.title,
                            commandPreview = action.commandPreview,
                            output = result,
                            commandType = action.commandType
                        )
                        runningCommandIdState.value = null

                        val isSuccess = !result.contains("失败", ignoreCase = true) &&
                                       !result.contains("failed", ignoreCase = true) &&
                                       !result.contains("error", ignoreCase = true)
                        scope.launch {
                            toastState.showToast(
                                message = StringResources.get(
                                    if (isSuccess) "operation.command.success" else "operation.command.failed",
                                    action.title
                                ),
                                type = if (isSuccess) ToastType.SUCCESS else ToastType.ERROR
                            )
                        }
                    }
                }
            }
        } else {
            viewModel.executeDeviceCommand(action.title, action.arguments) { result ->
                resultState.value = DeviceCommandResult(
                    title = action.title,
                    commandPreview = action.commandPreview,
                    output = result,
                    commandType = action.commandType
                )
                runningCommandIdState.value = null

                val isSuccess = !result.contains("失败", ignoreCase = true) &&
                               !result.contains("failed", ignoreCase = true) &&
                               !result.contains("error", ignoreCase = true)
                scope.launch {
                    toastState.showToast(
                        message = StringResources.get(
                            if (isSuccess) "operation.command.success" else "operation.command.failed",
                            action.title
                        ),
                        type = if (isSuccess) ToastType.SUCCESS else ToastType.ERROR
                    )
                }
            }
        }
    }

    LazyColumn(
        state = commandListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (favoriteCommands.isNotEmpty()) {
            item(key = "favorite-commands") {
                DeviceCommandGroupSection(
                    group = DeviceCommandGroup(
                        title = StringResources.get("operation.command.group.favorites"),
                        actions = favoriteCommands
                    ),
                    enabled = true,
                    runningCommandId = runningCommandId,
                    favoriteCommandIds = favoriteCommandIds,
                    onFavoriteToggle = ::toggleFavorite,
                    onCommandClick = ::executeAction
                )
            }
        }

        items(
            items = commandGroups,
            key = { group -> group.title }
        ) { group ->
            DeviceCommandGroupSection(
                group = group,
                enabled = true,
                runningCommandId = runningCommandId,
                favoriteCommandIds = favoriteCommandIds,
                onFavoriteToggle = ::toggleFavorite,
                onCommandClick = ::executeAction
            )
        }
    }
}

private fun createTemporaryScreenshotFile(): File {
    // 截图通过 adb exec-out 直接写入本机临时文件，不在车机端生成中转文件。
    return kotlin.io.path.createTempFile(
        prefix = "${SCREENSHOT_FILE_PREFIX}_",
        suffix = ".$SCREENSHOT_FILE_EXTENSION"
    ).toFile()
}

private fun defaultScreenshotFileName(): String {
    return "${SCREENSHOT_FILE_PREFIX}_${System.currentTimeMillis()}.$SCREENSHOT_FILE_EXTENSION"
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
    runningCommandId: String?,
    favoriteCommandIds: List<String>,
    onFavoriteToggle: (DeviceCommandAction) -> Unit,
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
                        isRunning = runningCommandId == action.id,
                        isFavorite = favoriteCommandIds.contains(action.id),
                        onFavoriteToggle = { onFavoriteToggle(action) },
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
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val favoriteInteractionSource = remember { MutableInteractionSource() }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        action.destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        action.destructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    }

    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
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
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = favoriteInteractionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onFavoriteToggle
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceCommandResultPanel(
    resultState: State<DeviceCommandResult?>,
    modifier: Modifier = Modifier
) {
    val result = resultState.value
    val scrollState = rememberScrollState()
    var showRawOutput by remember { mutableStateOf(false) }

    val parsedOutput = remember(result) {
        result?.takeUnless { it.running }?.let(::parseCommandResult)
    }

    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringResources.get("operation.result"),
                    style = MaterialTheme.typography.titleSmall
                )

                // 如果有解析结果且摘要与原始输出不同，显示切换按钮
                if (parsedOutput != null && parsedOutput.summary != parsedOutput.rawOutput) {
                    TextButton(
                        onClick = { showRawOutput = !showRawOutput },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (showRawOutput) {
                                StringResources.get("command.output.hide.raw")
                            } else {
                                StringResources.get("command.output.show.raw")
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                SelectionContainer {
                    if (result == null) {
                        Text(
                            text = StringResources.get("operation.command.result.empty"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (result.running) {
                        Text(
                            text = StringResources.get(
                                "operation.command.running",
                                result.title,
                                result.commandPreview
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (parsedOutput != null && !showRawOutput) {
                        Column {
                            Text(
                                text = StringResources.get("command.output.summary"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = parsedOutput.summary,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = StringResources.get("command.output.raw"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = parsedOutput?.rawOutput ?: result.output,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseCommandResult(result: DeviceCommandResult): com.zhang.adbhub.common.utils.CommandOutputParser.ParsedOutput {
    return when (result.commandType) {
        DeviceCommandType.VEHICLE_INFO -> com.zhang.adbhub.common.utils.CommandOutputParser.parseVehicleInfo(result.output)
        DeviceCommandType.DISPLAY_INFO -> com.zhang.adbhub.common.utils.CommandOutputParser.parseDisplayInfo(result.output)
        DeviceCommandType.CURRENT_FOCUS -> com.zhang.adbhub.common.utils.CommandOutputParser.parseCurrentFocus(result.output)
        DeviceCommandType.SCREENSHOT -> com.zhang.adbhub.common.utils.CommandOutputParser.parseScreenshot(result.output)
        DeviceCommandType.ROOT_RESTART -> com.zhang.adbhub.common.utils.CommandOutputParser.ParsedOutput(
            summary = result.output,
            rawOutput = result.output
        )
        DeviceCommandType.RAW -> com.zhang.adbhub.common.utils.CommandOutputParser.ParsedOutput(
            summary = result.output,
            rawOutput = result.output
        )
    }
}

@Composable
private fun ScreenshotPreviewDialog(
    imageFile: File,
    onDismiss: () -> Unit
) {
    val imageBitmap = remember(imageFile.absolutePath, imageFile.lastModified()) {
        runCatching { loadScreenshotBitmap(imageFile) }.getOrNull()
    }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var showLargePreview by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StringResources.get("operation.command.screenshot.preview.title")) },
        text = {
            Column(
                modifier = Modifier.width(720.dp).height(520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap == null) {
                            Text(
                                text = StringResources.get("operation.command.screenshot.preview.load.failed"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = StringResources.get("operation.command.screenshot.preview.title"),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { showLargePreview = true }
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                saveStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    saveStatus = saveScreenshotAs(imageFile)
                },
                enabled = imageBitmap != null
            ) {
                Text(StringResources.get("operation.command.screenshot.save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringResources.get("log.panel.cancel"))
            }
        }
    )

    if (showLargePreview && imageBitmap != null) {
        LargeScreenshotPreviewDialog(
            imageBitmap = imageBitmap,
            onDismiss = { showLargePreview = false }
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun LargeScreenshotPreviewDialog(
    imageBitmap: ImageBitmap,
    onDismiss: () -> Unit
) {
    var zoom by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val maxViewerWidth = 1120.dp
    val maxViewerHeight = 680.dp
    val imageAspectRatio = imageBitmap.width.toFloat() / imageBitmap.height.toFloat()
    val viewerAspectRatio = maxViewerWidth.value / maxViewerHeight.value
    val viewerWidth = if (imageAspectRatio >= viewerAspectRatio) {
        maxViewerWidth
    } else {
        maxViewerHeight * imageAspectRatio
    }
    val viewerHeight = if (imageAspectRatio >= viewerAspectRatio) {
        maxViewerWidth / imageAspectRatio
    } else {
        maxViewerHeight
    }

    fun clampPan(offset: Offset, targetZoom: Float): Offset {
        if (targetZoom <= 1f || viewportSize == IntSize.Zero) return Offset.Zero
        val maxX = viewportSize.width * (targetZoom - 1f) / 2f
        val maxY = viewportSize.height * (targetZoom - 1f) / 2f
        return Offset(
            x = offset.x.coerceIn(-maxX, maxX),
            y = offset.y.coerceIn(-maxY, maxY)
        )
    }

    fun setZoom(targetZoom: Float) {
        val nextZoom = targetZoom.coerceIn(SCREENSHOT_MIN_ZOOM, SCREENSHOT_MAX_ZOOM)
        zoom = nextZoom
        panOffset = clampPan(panOffset, nextZoom)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(viewerWidth)
                .height(viewerHeight)
                .clipToBounds()
                .onSizeChanged { size ->
                    viewportSize = size
                    panOffset = clampPan(panOffset, zoom)
                }
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (delta != 0f) {
                        setZoom(zoom - delta * 0.1f)
                        event.changes.forEach { it.consume() }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoom = 1f
                            panOffset = Offset.Zero
                        }
                    )
                }
                .pointerInput(zoom, viewportSize) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            panOffset = clampPan(panOffset + dragAmount, zoom)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = StringResources.get("operation.command.screenshot.large.title"),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panOffset.x
                        translationY = panOffset.y
                    },
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StringResources.get("operation.command.screenshot.zoom.level", (zoom * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { setZoom(zoom - SCREENSHOT_ZOOM_STEP) },
                    enabled = zoom > SCREENSHOT_MIN_ZOOM
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = StringResources.get("operation.command.screenshot.zoom.out"),
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = {
                        zoom = 1f
                        panOffset = Offset.Zero
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = StringResources.get("operation.command.screenshot.zoom.reset"),
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { setZoom(zoom + SCREENSHOT_ZOOM_STEP) },
                    enabled = zoom < SCREENSHOT_MAX_ZOOM
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = StringResources.get("operation.command.screenshot.zoom.in"),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = StringResources.get("log.panel.close"),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun loadScreenshotBitmap(file: File): ImageBitmap {
    return SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}

private fun saveScreenshotAs(sourceFile: File): String {
    val chooser = JFileChooser().apply {
        dialogTitle = StringResources.get("operation.command.screenshot.save.dialog.title")
        selectedFile = File(defaultScreenshotFileName())
    }
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val target = chooser.selectedFile
        sourceFile.copyTo(target, overwrite = true)
        StringResources.get("operation.command.screenshot.saved.to", target.absolutePath)
    } else {
        StringResources.get("operation.command.screenshot.save.cancelled")
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
                    icon = Icons.Default.Info,
                    commandType = DeviceCommandType.VEHICLE_INFO
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.display.info"),
                    description = StringResources.get("operation.command.display.info.desc"),
                    commandPreview = "shell wm size; wm density",
                    arguments = listOf("shell", "sh", "-c", "wm size; wm density"),
                    icon = Icons.Default.Build,
                    commandType = DeviceCommandType.DISPLAY_INFO
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
                    icon = Icons.Default.BugReport,
                    commandType = DeviceCommandType.CURRENT_FOCUS
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.screenshot"),
                    description = StringResources.get("operation.command.screenshot.desc"),
                    commandPreview = "exec-out screencap -p",
                    arguments = emptyList(),
                    icon = Icons.Default.CameraAlt,
                    commandType = DeviceCommandType.SCREENSHOT
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
                    icon = Icons.Default.Security,
                    commandType = DeviceCommandType.ROOT_RESTART
                ),
                DeviceCommandAction(
                    title = StringResources.get("operation.command.remount"),
                    description = StringResources.get("operation.command.remount.desc"),
                    commandPreview = "remount",
                    arguments = listOf("remount"),
                    icon = Icons.Default.Build,
                    commandType = DeviceCommandType.ROOT_RESTART
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

    if (selectedDevice == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = StringResources.get("operation.device.commands"),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            DeviceRequiredPrompt(
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = StringResources.get("operation.device.commands"),
            style = MaterialTheme.typography.titleMedium
        )

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

    if (selectedDevice == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = StringResources.get("operation.app.management"),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            DeviceRequiredPrompt(
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = StringResources.get("operation.app.management"),
            style = MaterialTheme.typography.titleMedium
        )

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
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("operation.start.app"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = StringResources.get("operation.start.app.hint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    label = { Text(StringResources.get("operation.activity.name.optional")) },
                    placeholder = { Text(StringResources.get("operation.activity.example")) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    singleLine = true,
                    enabled = !isExecuting
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (packageName.isNotBlank()) {
                            viewModel.startApp(packageName, activityName.takeIf { it.isNotBlank() }) { result ->
                                resultText = result
                            }
                        } else {
                            resultText = StringResources.get("operation.input.package")
                        }
                    },
                    enabled = packageName.isNotBlank() && !isExecuting,
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

        // 结果显示 - 增大高度以容纳更多内容
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(360.dp),
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
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace
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
