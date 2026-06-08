package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.desktop.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun LogPanel(
    selectedDevice: Device?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val logLines by viewModel.logLines.collectAsState()
    val isLogcatRunning by viewModel.isLogcatRunning.collectAsState()
    var filterText by remember { mutableStateOf("") }
    var exportStatus by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 自动滚动到底部
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty() && isLogcatRunning) {
            coroutineScope.launch {
                listState.animateScrollToItem(logLines.size - 1)
            }
        }
    }

    Column(modifier = modifier.padding(8.dp)) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "日志查看器",
                style = MaterialTheme.typography.titleMedium
            )
            Row {
                IconButton(
                    onClick = {
                        if (isLogcatRunning) {
                            viewModel.stopLogcat()
                        } else {
                            viewModel.startLogcat(filterText)
                        }
                    },
                    enabled = selectedDevice != null
                ) {
                    Icon(
                        if (isLogcatRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isLogcatRunning) "停止" else "开始"
                    )
                }
                IconButton(
                    onClick = {
                        val dialog = FileDialog(Frame(), "保存日志", FileDialog.SAVE)
                        dialog.file = "logcat_${System.currentTimeMillis()}.txt"
                        dialog.isVisible = true
                        val file = dialog.file
                        val dir = dialog.directory
                        if (file != null && dir != null) {
                            val outputFile = File(dir, file)
                            viewModel.exportLogs(outputFile) { result ->
                                exportStatus = result
                            }
                        }
                    },
                    enabled = selectedDevice != null
                ) {
                    Icon(Icons.Default.Save, contentDescription = "导出日志")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 筛选输入框
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            label = { Text("Grep 筛选") },
            placeholder = { Text("输入关键词筛选日志...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = selectedDevice != null,
            trailingIcon = {
                if (filterText.isNotEmpty()) {
                    IconButton(onClick = { filterText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 应用筛选按钮
        if (!isLogcatRunning) {
            Button(
                onClick = { viewModel.startLogcat(filterText) },
                enabled = selectedDevice != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("启动日志流")
            }
        } else {
            Button(
                onClick = { viewModel.stopLogcat() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("停止日志流")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日志内容
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (selectedDevice == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "请先选择设备",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (logLines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLogcatRunning) "等待日志..." else "点击启动日志流",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    items(logLines) { line ->
                        Text(
                            text = line,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 导出状态提示
        if (exportStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exportStatus,
                style = MaterialTheme.typography.bodySmall,
                color = if (exportStatus.contains("成功")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
