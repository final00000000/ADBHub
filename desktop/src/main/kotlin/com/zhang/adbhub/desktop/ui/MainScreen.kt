package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.common.config.AdbPathDetector
import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.desktop.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = remember { MainViewModel() }
    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val adbStatus by viewModel.adbStatus.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSetupGuide by remember { mutableStateOf(false) }

    // Check ADB availability on startup
    LaunchedEffect(Unit) {
        val config = AdbConfig.load()
        val adbPath = AdbPathDetector.getValidAdbPath(config.customAdbPath)
        if (adbPath == null) {
            showSetupGuide = true
        } else {
            viewModel.refreshDevices()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TopAppBar with settings button
        TopAppBar(
            title = { Text("ADB Hub") },
            actions = {
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // 左栏：设备列表
            DeviceListPanel(
                devices = devices,
                selectedDevice = selectedDevice,
                onDeviceSelected = { viewModel.selectDevice(it) },
                onRefresh = { viewModel.refreshDevices() },
                adbStatus = adbStatus,
                modifier = Modifier.width(250.dp).fillMaxHeight()
            )

            VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())

            // 中栏：操作区
            OperationPanel(
                selectedDevice = selectedDevice,
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                viewModel = viewModel,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())

            // 右栏：日志区
            LogPanel(
                selectedDevice = selectedDevice,
                viewModel = viewModel,
                modifier = Modifier.width(450.dp).fillMaxHeight()
            )
        }
    }

    // Dialogs
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = {
                showSettingsDialog = false
                viewModel.refreshDevices()
            }
        )
    }

    if (showSetupGuide) {
        AdbSetupGuideDialog(
            onDismiss = { showSetupGuide = false },
            onAdbConfigured = {
                showSetupGuide = false
                viewModel.refreshDevices()
            }
        )
    }
}
