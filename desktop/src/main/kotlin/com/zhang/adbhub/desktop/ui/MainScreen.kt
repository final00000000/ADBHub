package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.config.AdbPathDetector
import com.zhang.adbhub.desktop.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = remember { MainViewModel() }
    val toastState = rememberToastState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSetupGuide by remember { mutableStateOf(false) }
    var isLogFullscreen by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLogFullscreen) {
            FullscreenLogPanelHost(
                viewModel = viewModel,
                onToggleFullscreen = { isLogFullscreen = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                DeviceListPanelHost(
                    viewModel = viewModel,
                    onSettingsClick = { showSettingsDialog = true },
                    modifier = Modifier.width(280.dp).fillMaxHeight()
                )

                VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())

                OperationPanelHost(
                    viewModel = viewModel,
                    toastState = toastState,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                VerticalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())

                LogPanelHost(
                    viewModel = viewModel,
                    onToggleFullscreen = { isLogFullscreen = true },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Toast 显示在顶部
        ToastHost(
            toastState = toastState,
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { shouldRefresh ->
                showSettingsDialog = false
                if (shouldRefresh) {
                    viewModel.refreshDevices()
                }
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

@Composable
private fun DeviceListPanelHost(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val adbStatus by viewModel.adbStatus.collectAsState()
    val deviceDiagnostics by viewModel.deviceDiagnostics.collectAsState()

    DeviceListPanel(
        devices = devices,
        selectedDevice = selectedDevice,
        onDeviceSelected = { viewModel.selectDevice(it) },
        onRefresh = { viewModel.refreshDevices() },
        onSettingsClick = onSettingsClick,
        adbStatus = adbStatus,
        deviceDiagnostics = deviceDiagnostics,
        modifier = modifier
    )
}

@Composable
private fun OperationPanelHost(
    viewModel: MainViewModel,
    toastState: ToastState,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    OperationPanel(
        selectedDevice = selectedDevice,
        selectedTab = selectedTab,
        onTabSelected = { viewModel.selectTab(it) },
        viewModel = viewModel,
        toastState = toastState,
        modifier = modifier
    )
}

@Composable
private fun LogPanelHost(
    viewModel: MainViewModel,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    LogPanel(
        selectedDevice = selectedDevice,
        viewModel = viewModel,
        isFullscreen = false,
        onToggleFullscreen = onToggleFullscreen,
        modifier = modifier
    )
}

@Composable
private fun FullscreenLogPanelHost(
    viewModel: MainViewModel,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    LogPanel(
        selectedDevice = selectedDevice,
        viewModel = viewModel,
        isFullscreen = true,
        onToggleFullscreen = onToggleFullscreen,
        modifier = modifier
    )
}
