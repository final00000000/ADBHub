package com.zhang.adbhub.common.adb

import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.DeviceState
import com.zhang.adbhub.common.model.AdbResult
import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.config.AdbPathDetector
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Dadb 实现的 ADB 管理器
 */
class DadbManager : AdbManager {
    private val logcatProcesses = ConcurrentHashMap<String, Process>()

    /**
     * 获取当前使用的 ADB 路径
     */
    private fun getAdbPath(): String? {
        val config = AdbConfig.load()
        return AdbPathDetector.getValidAdbPath(config.customAdbPath)
    }

    override suspend fun getDevices(): AdbResult<List<Device>> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

            val processBuilder = ProcessBuilder(adbPath, "devices", "-l")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().use { it.readText() }
                return@withContext AdbResult.Error("获取设备列表失败: $error")
            }

            val devices = output.lines()
                .drop(1) // Skip "List of devices attached"
                .filter { it.isNotBlank() && it.contains("\t") }
                .mapNotNull { line ->
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        val serialNumber = parts[0]
                        val state = when (parts[1]) {
                            "device" -> DeviceState.ONLINE
                            "offline" -> DeviceState.OFFLINE
                            "unauthorized" -> DeviceState.UNAUTHORIZED
                            else -> DeviceState.UNKNOWN
                        }

                        // Extract model from line (format: model:xxx)
                        val modelMatch = "model:(\\S+)".toRegex().find(line)
                        val model = modelMatch?.groupValues?.get(1)

                        Device(
                            serialNumber = serialNumber,
                            model = model,
                            state = state
                        )
                    } else null
                }

            AdbResult.Success(devices)
        } catch (e: Exception) {
            AdbResult.Error("Failed to get devices: ${e.message}", e)
        }
    }

    override suspend fun pushApk(device: Device, apkFile: File): AdbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!apkFile.exists()) {
                return@withContext AdbResult.Error("APK file not found: ${apkFile.absolutePath}")
            }

            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

            // Use adb command directly for install
            val processBuilder = ProcessBuilder(
                adbPath, "-s", device.serialNumber, "install", "-r", apkFile.absolutePath
            )
            val process = processBuilder.start()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(Unit)
            } else {
                val error = process.errorStream.bufferedReader().use { it.readText() }
                AdbResult.Error("Failed to install APK: $error")
            }
        } catch (e: Exception) {
            AdbResult.Error("Failed to push APK: ${e.message}", e)
        }
    }

    override fun getLogcatFlow(device: Device): Flow<String> = flow {
        try {
            val adbPath = getAdbPath()
            if (adbPath == null) {
                emit("错误: 未检测到 ADB 工具，请在设置中配置 ADB 路径")
                return@flow
            }

            val processBuilder = ProcessBuilder(
                adbPath, "-s", device.serialNumber, "logcat"
            )
            val process = processBuilder.start()
            logcatProcesses[device.serialNumber] = process

            process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    emit(line)
                }
            }
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        } finally {
            logcatProcesses.remove(device.serialNumber)
        }
    }.flowOn(Dispatchers.IO)

    override fun stopLogcat(device: Device) {
        logcatProcesses[device.serialNumber]?.destroy()
        logcatProcesses.remove(device.serialNumber)
    }

    override suspend fun exportLogs(device: Device, outputFile: File, maxLines: Int): AdbResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "logcat", "-d", "-t", maxLines.toString()
                )
                val process = processBuilder.start()
                val logOutput = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode != 0) {
                    val error = process.errorStream.bufferedReader().use { it.readText() }
                    return@withContext AdbResult.Error("导出日志失败: $error")
                }

                outputFile.writeText(logOutput)

                AdbResult.Success(Unit)
            } catch (e: Exception) {
                AdbResult.Error("Failed to export logs: ${e.message}", e)
            }
        }

    override suspend fun executeRoot(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "root")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(output.ifEmpty { "Root 命令执行成功" })
            } else {
                AdbResult.Error("Root 命令失败: ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error("执行 root 命令失败: ${e.message}", e)
        }
    }

    override suspend fun executeRemount(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "remount")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(output.ifEmpty { "Remount 命令执行成功" })
            } else {
                AdbResult.Error("Remount 命令失败: ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error("执行 remount 命令失败: ${e.message}", e)
        }
    }

    override suspend fun rebootDevice(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "reboot")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success("设备重启命令已发送")
            } else {
                AdbResult.Error("重启命令失败: ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error("执行重启命令失败: ${e.message}", e)
        }
    }

    override suspend fun rebootRecovery(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "reboot", "recovery")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success("设备重启到 Recovery 模式命令已发送")
            } else {
                AdbResult.Error("重启到 Recovery 失败: ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error("执行重启到 Recovery 失败: ${e.message}", e)
        }
    }

    override suspend fun rebootBootloader(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "reboot", "bootloader")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success("设备重启到 Bootloader 模式命令已发送")
            } else {
                AdbResult.Error("重启到 Bootloader 失败: ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error("执行重启到 Bootloader 失败: ${e.message}", e)
        }
    }

    override suspend fun startApp(device: Device, packageName: String, activityName: String): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

                val component = "$packageName/$activityName"
                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "shell", "am", "start", "-n", component
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success(output.ifEmpty { "应用启动成功" })
                } else {
                    AdbResult.Error("启动应用失败: ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("执行启动应用失败: ${e.message}", e)
            }
        }

    override suspend fun getAppInfo(device: Device, packageName: String): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "shell", "dumpsys", "package", packageName
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success(output)
                } else {
                    AdbResult.Error("获取应用信息失败: ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("执行获取应用信息失败: ${e.message}", e)
            }
        }

    override suspend fun stopApp(device: Device, packageName: String): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "shell", "am", "force-stop", packageName
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success("应用已停止")
                } else {
                    AdbResult.Error("停止应用失败: ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("执行停止应用失败: ${e.message}", e)
            }
        }

    override suspend fun clearAppData(device: Device, packageName: String): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error("未检测到 ADB 工具，请在设置中配置 ADB 路径")

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "shell", "pm", "clear", packageName
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success(output.ifEmpty { "应用数据已清除" })
                } else {
                    AdbResult.Error("清除应用数据失败: ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("执行清除应用数据失败: ${e.message}", e)
            }
        }
}
