package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.DeviceState
import com.zhang.adbhub.desktop.utils.StringResources
import com.zhang.adbhub.desktop.viewmodel.DeviceDetectionItem
import com.zhang.adbhub.desktop.viewmodel.DeviceDetectionStatus

@Composable
fun DeviceListPanel(
    devices: List<Device>,
    selectedDevice: Device?,
    onDeviceSelected: (Device) -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    adbStatus: String? = null,
    deviceDiagnostics: List<DeviceDetectionItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // 标题
        Text(
            text = StringResources.get("device.list.title"),
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
                contentDescription = StringResources.get("device.list.refresh"),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(StringResources.get("device.list.refresh.button"))
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
                DeviceDiagnosticsPanel(
                    diagnostics = deviceDiagnostics,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (devices.none { it.state == DeviceState.ONLINE }) {
                        DeviceDiagnosticsSummary(
                            diagnostics = deviceDiagnostics,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
        }

        // 底部设置按钮
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(StringResources.get("device.list.settings"))
        }
    }
}

@Composable
private fun DeviceDiagnosticsPanel(
    diagnostics: List<DeviceDetectionItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = StringResources.get("device.diagnostic.title"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = StringResources.get("device.diagnostic.subtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (diagnostics.isEmpty()) {
            item {
                Text(
                    text = StringResources.get("device.list.no.device"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(diagnostics) { diagnostic ->
                DeviceDiagnosticRow(diagnostic)
            }
        }
    }
}

@Composable
private fun DeviceDiagnosticsSummary(
    diagnostics: List<DeviceDetectionItem>,
    modifier: Modifier = Modifier
) {
    val diagnostic = diagnostics.firstOrNull { it.status == DeviceDetectionStatus.ERROR }
        ?: diagnostics.firstOrNull { it.status == DeviceDetectionStatus.WARNING }
        ?: return

    Surface(
        modifier = modifier,
        color = diagnosticContainerColor(diagnostic.status),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = diagnosticIcon(diagnostic.status),
                contentDescription = null,
                tint = diagnosticContentColor(diagnostic.status),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = StringResources.get("device.diagnostic.summary.title"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${diagnostic.title}: ${diagnostic.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceDiagnosticRow(diagnostic: DeviceDetectionItem) {
    val color = diagnosticContentColor(diagnostic.status)

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = diagnosticIcon(diagnostic.status),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diagnostic.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = detectionStatusText(diagnostic.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = diagnostic.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
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
            .height(72.dp)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.model ?: StringResources.get("device.model.unknown"),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = device.serialNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = deviceStateText(device.state),
                style = MaterialTheme.typography.labelSmall,
                color = deviceStateColor(device.state),
                maxLines = 1
            )
        }
    }
}

private fun detectionStatusText(status: DeviceDetectionStatus): String {
    return when (status) {
        DeviceDetectionStatus.OK -> StringResources.get("device.diagnostic.status.ok")
        DeviceDetectionStatus.WARNING -> StringResources.get("device.diagnostic.status.warning")
        DeviceDetectionStatus.ERROR -> StringResources.get("device.diagnostic.status.error")
        DeviceDetectionStatus.PENDING -> StringResources.get("device.diagnostic.status.pending")
    }
}

private fun diagnosticIcon(status: DeviceDetectionStatus): ImageVector {
    return when (status) {
        DeviceDetectionStatus.OK -> Icons.Default.CheckCircle
        DeviceDetectionStatus.WARNING -> Icons.Default.Warning
        DeviceDetectionStatus.ERROR -> Icons.Default.Error
        DeviceDetectionStatus.PENDING -> Icons.Default.Info
    }
}

@Composable
private fun diagnosticContentColor(status: DeviceDetectionStatus) = when (status) {
    DeviceDetectionStatus.OK -> MaterialTheme.colorScheme.primary
    DeviceDetectionStatus.WARNING -> MaterialTheme.colorScheme.tertiary
    DeviceDetectionStatus.ERROR -> MaterialTheme.colorScheme.error
    DeviceDetectionStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun diagnosticContainerColor(status: DeviceDetectionStatus) = when (status) {
    DeviceDetectionStatus.OK -> MaterialTheme.colorScheme.primaryContainer
    DeviceDetectionStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
    DeviceDetectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
    DeviceDetectionStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
}

private fun deviceStateText(state: DeviceState): String {
    return when (state) {
        DeviceState.ONLINE -> StringResources.get("device.state.online")
        DeviceState.OFFLINE -> StringResources.get("device.state.offline")
        DeviceState.UNAUTHORIZED -> StringResources.get("device.state.unauthorized")
        DeviceState.UNKNOWN -> StringResources.get("device.state.unknown")
    }
}

@Composable
private fun deviceStateColor(state: DeviceState) = when (state) {
    DeviceState.ONLINE -> MaterialTheme.colorScheme.primary
    DeviceState.OFFLINE -> MaterialTheme.colorScheme.error
    DeviceState.UNAUTHORIZED -> MaterialTheme.colorScheme.error
    DeviceState.UNKNOWN -> MaterialTheme.colorScheme.tertiary
}
