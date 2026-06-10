package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.common.model.Device

@Composable
fun DeviceListPanel(
    devices: List<Device>,
    selectedDevice: Device?,
    onDeviceSelected: (Device) -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    adbStatus: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // 标题
        Text(
            text = "已连接设备",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 刷新按钮
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "刷新设备",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("刷新设备列表")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ADB 状态显示 - 固定高度避免跳动
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (adbStatus != null) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = adbStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 设备列表区域 - 使用 weight 占据剩余空间
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (devices.isEmpty()) {
                Text(
                    text = "未检测到设备\n\n请确保：\n• ADB 已安装\n• 设备已连接\n• USB 调试已开启",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(devices) { device ->
                        DeviceItem(
                            device = device,
                            isSelected = device == selectedDevice,
                            onClick = { onDeviceSelected(device) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // 底部设置按钮
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("设置")
        }
    }
}

@Composable
fun DeviceItem(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Smartphone,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = device.model ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = device.serialNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
