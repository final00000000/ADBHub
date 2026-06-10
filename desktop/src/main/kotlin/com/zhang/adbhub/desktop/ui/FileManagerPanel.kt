package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.FileInfo
import com.zhang.adbhub.desktop.viewmodel.MainViewModel
import com.zhang.adbhub.desktop.utils.StringResources
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun FileManagerPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    val currentPath by viewModel.currentPath.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    var resultText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<FileInfo?>(null) }

    // 当设备改变或首次加载时，导航到默认路径
    LaunchedEffect(selectedDevice) {
        if (selectedDevice != null && fileList.isEmpty()) {
            viewModel.navigateToPath(currentPath)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = StringResources.get("file.manager.title"),
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            Text(
                text = StringResources.get("file.manager.select.device.first"),
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Text(
            text = StringResources.get("file.manager.target.device", selectedDevice.model ?: selectedDevice.serialNumber),
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        // 路径导航栏
        PathNavigationBar(
            currentPath = currentPath,
            onNavigate = { path ->
                viewModel.navigateToPath(path)
                resultText = ""
            },
            isExecuting = isExecuting
        )

        // 操作按钮区 - 固定高度
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val dialog = FileDialog(Frame(), StringResources.get("file.manager.select_file_upload"), FileDialog.LOAD)
                    dialog.isVisible = true
                    val file = dialog.file
                    val dir = dialog.directory
                    if (file != null && dir != null) {
                        val localFile = File(dir, file)
                        val remotePath = if (currentPath.endsWith("/")) {
                            "$currentPath$file"
                        } else {
                            "$currentPath/$file"
                        }
                        viewModel.pushFileToDevice(localFile, remotePath) { result ->
                            resultText = result
                        }
                    }
                },
                enabled = !isExecuting,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(StringResources.get("file.manager.upload_file"))
            }

            Button(
                onClick = {
                    viewModel.navigateToPath(currentPath)
                    resultText = StringResources.get("file.manager.directory_refreshed")
                },
                enabled = !isExecuting,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(StringResources.get("file.manager.refresh"))
            }
        }

        // 文件列表 - 固定高度
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (fileList.isEmpty()) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(32.dp)
                        )
                    } else {
                    Text(
                        text = StringResources.get("file.manager.directory_empty_or_inaccessible"),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                } else {
                    FileListContent(
                        fileList = fileList,
                        currentPath = currentPath,
                        onNavigate = { path ->
                            viewModel.navigateToPath(path)
                            resultText = ""
                        },
                        onDownload = { fileInfo ->
                            val dialog = FileDialog(Frame(), StringResources.get("file.manager.save_file"), FileDialog.SAVE)
                            dialog.file = fileInfo.name
                            dialog.isVisible = true
                            val file = dialog.file
                            val dir = dialog.directory
                            if (file != null && dir != null) {
                                val localFile = File(dir, file)
                                viewModel.pullFile(fileInfo.fullPath, localFile) { result ->
                                    resultText = result
                                }
                            }
                        },
                        onDelete = { fileInfo ->
                            fileToDelete = fileInfo
                            showDeleteDialog = true
                        },
                        isExecuting = isExecuting
                    )
                }
            }
        }

        // 结果显示 - 固定高度防止跳动
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = StringResources.get("file.manager.operation_result"),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (resultText.isNotEmpty()) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = StringResources.get("file.manager.waiting_for_operation"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(StringResources.get("file.manager.confirm_delete")) },
            text = {
                Text(StringResources.get("file.manager.delete_confirmation_message", fileToDelete!!.name, if (fileToDelete!!.isDirectory) StringResources.get("file.manager.delete_directory_warning") else ""))
            },
            confirmButton = {
                Button(
                    onClick = {
                        fileToDelete?.let { file ->
                            viewModel.deleteFile(file.fullPath) { result ->
                                resultText = result
                            }
                        }
                        showDeleteDialog = false
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(StringResources.get("file.manager.delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    fileToDelete = null
                }) {
                    Text(StringResources.get("file.manager.cancel"))
                }
            }
        )
    }
}

@Composable
fun PathNavigationBar(
    currentPath: String,
    onNavigate: (String) -> Unit,
    isExecuting: Boolean
) {
    var editablePath by remember(currentPath) { mutableStateOf(currentPath) }
    var isPathValid by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 上级目录按钮
            IconButton(
                onClick = {
                    val parentPath = currentPath.substringBeforeLast("/", "/")
                    if (parentPath.isNotEmpty()) {
                        onNavigate(parentPath.ifEmpty { "/" })
                        errorMessage = ""
                    }
                },
                enabled = currentPath != "/" && !isExecuting,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
            }

            // 可编辑路径输入框
            OutlinedTextField(
                value = editablePath,
                onValueChange = {
                    editablePath = it
                    isPathValid = it.isNotBlank() && it.startsWith("/")
                    if (!isPathValid && it.isNotBlank()) {
                        errorMessage = "路径必须以 / 开头"
                    } else {
                        errorMessage = ""
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                singleLine = true,
                enabled = !isExecuting,
                isError = !isPathValid && editablePath.isNotBlank(),
                leadingIcon = {
                    Icon(Icons.Default.Folder, contentDescription = null)
                },
                placeholder = {
                    Text("输入设备路径，例如: /sdcard/", style = MaterialTheme.typography.bodySmall)
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // 跳转按钮
            Button(
                onClick = {
                    if (isPathValid && editablePath.isNotBlank()) {
                        onNavigate(editablePath.trim())
                        errorMessage = ""
                    }
                },
                enabled = !isExecuting && isPathValid && editablePath.isNotBlank(),
                modifier = Modifier.height(48.dp).width(80.dp)
            ) {
                Text("跳转")
            }
        }

        // 固定高度的错误提示区域，防止 UI 跳动
        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp).padding(start = 56.dp, top = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun FileListContent(
    fileList: List<FileInfo>,
    currentPath: String,
    onNavigate: (String) -> Unit,
    onDownload: (FileInfo) -> Unit,
    onDelete: (FileInfo) -> Unit,
    isExecuting: Boolean
) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 12.dp)
        ) {
            // 表头
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "名称",
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "大小",
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "权限",
                        modifier = Modifier.width(100.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "修改时间",
                        modifier = Modifier.width(120.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(120.dp)) // 操作按钮空间
                }
            }

            items(fileList) { fileInfo ->
                FileListItem(
                    fileInfo = fileInfo,
                    onNavigate = onNavigate,
                    onDownload = onDownload,
                    onDelete = onDelete,
                    isExecuting = isExecuting
                )
                HorizontalDivider()
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(listState)
        )
    }
}

@Composable
fun FileListItem(
    fileInfo: FileInfo,
    onNavigate: (String) -> Unit,
    onDownload: (FileInfo) -> Unit,
    onDelete: (FileInfo) -> Unit,
    isExecuting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = fileInfo.isDirectory && !isExecuting) {
                if (fileInfo.isDirectory) {
                    onNavigate(fileInfo.fullPath)
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标 + 名称
        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (fileInfo.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (fileInfo.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = fileInfo.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }

        // 大小
        Text(
            text = fileInfo.getFormattedSize(),
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 权限
        Text(
            text = fileInfo.permissions,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 修改时间
        Text(
            text = fileInfo.modifiedTime,
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 操作按钮
        Row(
            modifier = Modifier.width(120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!fileInfo.isDirectory) {
                IconButton(
                    onClick = { onDownload(fileInfo) },
                    enabled = !isExecuting,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "下载",
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }

            IconButton(
                onClick = { onDelete(fileInfo) },
                enabled = !isExecuting,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
