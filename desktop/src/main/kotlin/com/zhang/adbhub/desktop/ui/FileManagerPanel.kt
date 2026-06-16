package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.FileInfo
import com.zhang.adbhub.desktop.utils.StringResources
import com.zhang.adbhub.desktop.viewmodel.MainViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

private const val ROOT_PATH = "/"

@Composable
fun FileManagerPanel(selectedDevice: Device?, viewModel: MainViewModel) {
    val currentPath by viewModel.currentPath.collectAsState()
    val fileList by viewModel.fileList.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()

    var selectedFile by remember(currentPath) { mutableStateOf<FileInfo?>(null) }
    var resultText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDevice?.serialNumber) {
        if (selectedDevice != null && currentPath.isBlank()) {
            viewModel.navigateToPath(ROOT_PATH)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = StringResources.get("file.manager.device.explorer"),
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedDevice == null) {
            DeviceRequiredPrompt(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                description = StringResources.get("file.manager.select.device.first")
            )
            return@Column
        }

        DeviceExplorerHeader(selectedDevice = selectedDevice)

        DeviceExplorerToolbar(
            currentPath = currentPath.ifBlank { ROOT_PATH },
            selectedFile = selectedFile,
            isExecuting = isExecuting,
            onHome = {
                viewModel.navigateToPath(ROOT_PATH)
                resultText = ""
            },
            onParent = {
                viewModel.navigateToPath(parentPath(currentPath))
                resultText = ""
            },
            onRefresh = {
                viewModel.navigateToPath(currentPath.ifBlank { ROOT_PATH })
                resultText = StringResources.get("file.manager.directory.refreshed")
            },
            onUpload = {
                val dialog = FileDialog(Frame(), StringResources.get("file.manager.select.file.upload"), FileDialog.LOAD)
                dialog.isVisible = true
                val file = dialog.file
                val dir = dialog.directory
                if (file != null && dir != null) {
                    val localFile = File(dir, file)
                    viewModel.pushFileToDevice(localFile, childPath(currentPath.ifBlank { ROOT_PATH }, file)) { result ->
                        resultText = result
                    }
                }
            },
            onDownload = {
                selectedFile?.let { fileInfo ->
                    val dialog = FileDialog(Frame(), StringResources.get("file.manager.save.file"), FileDialog.SAVE)
                    dialog.file = fileInfo.name
                    dialog.isVisible = true
                    val file = dialog.file
                    val dir = dialog.directory
                    if (file != null && dir != null) {
                        viewModel.pullFile(fileInfo.fullPath, File(dir, file)) { result ->
                            resultText = result
                        }
                    }
                }
            },
            onDelete = { showDeleteDialog = true }
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isExecuting && fileList.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(32.dp)
                        )
                    }
                    fileList.isEmpty() -> {
                        Text(
                            text = StringResources.get("file.manager.directory.empty.or.inaccessible"),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        DeviceExplorerTable(
                            fileList = fileList,
                            selectedFile = selectedFile,
                            onSelect = { selectedFile = it },
                            onOpenDirectory = { fileInfo ->
                                selectedFile = null
                                resultText = ""
                                viewModel.navigateToPath(fileInfo.fullPath)
                            },
                            isExecuting = isExecuting
                        )
                    }
                }
            }
        }

        FileActionResult(resultText = resultText)
    }

    if (showDeleteDialog && selectedFile != null) {
        val fileInfo = selectedFile!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(StringResources.get("file.manager.confirm.delete")) },
            text = {
                Text(
                    StringResources.get(
                        "file.manager.delete.confirmation.message",
                        fileInfo.name,
                        if (fileInfo.isDirectory) StringResources.get("file.manager.delete.directory.warning") else ""
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(fileInfo.fullPath) { result ->
                            resultText = result
                            selectedFile = null
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(StringResources.get("file.manager.delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(StringResources.get("settings.cancel"))
                }
            }
        )
    }
}

@Composable
private fun DeviceExplorerHeader(selectedDevice: Device) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(38.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedDevice.model ?: selectedDevice.serialNumber,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeviceExplorerToolbar(
    currentPath: String,
    selectedFile: FileInfo?,
    isExecuting: Boolean,
    onHome: () -> Unit,
    onParent: () -> Unit,
    onRefresh: () -> Unit,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = currentPath,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExplorerToolButton(Icons.Default.Home, StringResources.get("file.manager.root.directories"), !isExecuting, onHome)
            ExplorerToolButton(Icons.Default.VerticalAlignTop, StringResources.get("file.manager.parent.directory"), !isExecuting && currentPath != ROOT_PATH, onParent)
            ExplorerToolButton(Icons.Default.Upload, StringResources.get("file.manager.upload.file"), !isExecuting, onUpload)
            ExplorerToolButton(Icons.Default.Download, StringResources.get("file.manager.download"), !isExecuting && selectedFile != null, onDownload)
            ExplorerToolButton(Icons.Default.Delete, StringResources.get("file.manager.delete"), !isExecuting && selectedFile != null, onDelete)
            ExplorerToolButton(Icons.Default.Refresh, StringResources.get("file.manager.refresh"), !isExecuting, onRefresh)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = selectedFile?.let { StringResources.get("file.manager.selected.item", it.name) }
                    ?: StringResources.get("file.manager.no.selection"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExplorerToolButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DeviceExplorerTable(
    fileList: List<FileInfo>,
    selectedFile: FileInfo?,
    onSelect: (FileInfo) -> Unit,
    onOpenDirectory: (FileInfo) -> Unit,
    isExecuting: Boolean
) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 12.dp)
        ) {
            item {
                DeviceExplorerHeaderRow()
            }

            items(
                items = fileList.sortedWith(compareByDescending<FileInfo> { it.isDirectory }.thenBy { it.name.lowercase() }),
                key = { it.fullPath }
            ) { fileInfo ->
                DeviceExplorerRow(
                    fileInfo = fileInfo,
                    selected = selectedFile?.fullPath == fileInfo.fullPath,
                    enabled = !isExecuting,
                    onSelect = onSelect,
                    onOpenDirectory = onOpenDirectory
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
private fun DeviceExplorerHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(StringResources.get("file.manager.name"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(StringResources.get("file.manager.permissions"), modifier = Modifier.width(112.dp), style = MaterialTheme.typography.labelMedium)
        Text(StringResources.get("file.manager.modified.time"), modifier = Modifier.width(136.dp), style = MaterialTheme.typography.labelMedium)
        Text(StringResources.get("file.manager.size"), modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DeviceExplorerRow(
    fileInfo: FileInfo,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (FileInfo) -> Unit,
    onOpenDirectory: (FileInfo) -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(background)
            .clickable(enabled = enabled) { onSelect(fileInfo) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fileInfo.isDirectory) {
                IconButton(
                    onClick = { onOpenDirectory(fileInfo) },
                    enabled = enabled,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = StringResources.get("file.manager.folder.open"), modifier = Modifier.size(18.dp))
                }
            } else {
                Spacer(modifier = Modifier.width(26.dp))
            }

            Icon(
                imageVector = if (fileInfo.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = if (fileInfo.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = fileInfo.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(fileInfo.permissions, modifier = Modifier.width(112.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(fileInfo.modifiedTime, modifier = Modifier.width(136.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(fileInfo.getFormattedSize(), modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun FileActionResult(resultText: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().height(82.dp),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Text(
                text = resultText.ifBlank { StringResources.get("file.manager.waiting.for.operation") },
                style = MaterialTheme.typography.bodySmall,
                color = if (resultText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}

private fun parentPath(path: String): String {
    val normalized = path.ifBlank { ROOT_PATH }.trimEnd('/')
    if (normalized.isBlank() || normalized == ROOT_PATH) return ROOT_PATH
    return normalized.substringBeforeLast('/', ROOT_PATH).ifBlank { ROOT_PATH }
}

private fun childPath(parent: String, childName: String): String {
    return if (parent.endsWith('/')) "$parent$childName" else "$parent/$childName"
}
