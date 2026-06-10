package com.zhang.adbhub.common.adb
import com.zhang.adbhub.common.utils.StringResources

import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.DeviceState
import com.zhang.adbhub.common.model.AdbResult
import com.zhang.adbhub.common.model.FileInfo
import com.zhang.adbhub.common.config.AdbConfig
import com.zhang.adbhub.common.config.AdbPathDetector
import kotlinx.coroutines.CancellationException
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
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

            val processBuilder = ProcessBuilder(adbPath, "devices", "-l")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().use { it.readText() }
                return@withContext AdbResult.Error("获取设备列表失败: $error)
            }

            val devices = output.lines()
                .drop(1)
                .filter { it.isNotBlank() && it.split(Regex("\\s+")).size >= 2 }
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

                        val modelMatch = "model:(\\S+)".toRegex().find(line)
                        val model = modelMatch?.groupValues?.get(1)

                        Device(
                            serialNumber = serialNumber,
                            model = model,
                            state = state
                        )
                    } else {
                        null
                    }
                }

            AdbResult.Success(devices)
        } catch (e: Exception) {
            AdbResult.Error("Failed to get devices: ${e.message}", e)
        }
    }

    override suspend fun pushApk(device: Device, apkFile: File, targetPath: String): AdbResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!apkFile.exists()) {
                return@withContext AdbResult.Error("APK file not found: ${apkFile.absolutePath}")
            }

            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

            // Use adb push command to push APK to device
            val processBuilder = ProcessBuilder(
                adbPath, "-s", device.serialNumber, "push", apkFile.absolutePath, targetPath
            )
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(Unit)
            } else {
                AdbResult.Error(StringResources.get("common.adb.push.failed", ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error(StringResources.get("common.adb.push.failed", ${e.message}", e)
        }
    }

    override fun getLogcatFlow(device: Device): Flow<String> = flow {
        var process: Process? = null
        try {
            val adbPath = getAdbPath()
            if (adbPath == null) {
                emit(StringResources.get("common.error.prefix") + StringResources.get("common.adb.not.detected"))
                return@flow
            }

            val processBuilder = ProcessBuilder(
                adbPath, "-s", device.serialNumber, "logcat", "-T", "100", "-v", "threadtime"
            ).redirectErrorStream(true)
            val startedProcess = processBuilder.start()
            process = startedProcess
            logcatProcesses.put(device.serialNumber, startedProcess)?.let { previousProcess ->
                stopProcess(previousProcess)
            }

            startedProcess.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    emit(line)
                }
            }
            val exitCode = startedProcess.waitFor()
            if (exitCode != 0) {
                emit("logcat exited with code $exitCode")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        } finally {
            process?.let { startedProcess ->
                logcatProcesses.remove(device.serialNumber, startedProcess)
                stopProcess(startedProcess)
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun stopLogcat(device: Device) {
        logcatProcesses.remove(device.serialNumber)?.let(::stopProcess)
    }

    private fun stopProcess(process: Process) {
        process.destroy()
        if (process.isAlive) {
            process.destroyForcibly()
        }
    }

    override suspend fun exportLogs(device: Device, outputFolder: File): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

                val config = AdbConfig.load()
                val deviceLogPath = config.deviceLogPath ?: "/sdcard/"

                // 确保输出文件夹存在
                if (!outputFolder.exists()) {
                    outputFolder.mkdirs()
                }

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "pull", deviceLogPath, outputFolder.absolutePath
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success("日志文件夹导出成功: ${outputFolder.absolutePath}\n$output")
                } else {
                    AdbResult.Error(StringResources.get("common.adb.export.failed", ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("Failed to export logs: ${e.message}", e)
            }
        }

    override suspend fun clearDeviceLogs(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

            val config = AdbConfig.load()
            val deviceLogPath = config.deviceLogPath ?: "/sdcard/"
            val clearPath = if (deviceLogPath.endsWith("/")) "${deviceLogPath}*" else "$deviceLogPath/*"

            val processBuilder = ProcessBuilder(
                adbPath, "-s", device.serialNumber, "shell", "rm", "-rf", clearPath
            )
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(StringResources.get("common.adb.clear.success"))
            } else {
                AdbResult.Error(StringResources.get("common.adb.clear.failed", ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error(StringResources.get("common.adb.execute.clear.failed", ${e.message}", e)
        }
    }

    override suspend fun executeRoot(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "root")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(output.ifEmpty { StringResources.get("common.adb.root.success") })
            } else {
                AdbResult.Error(StringResources.get("common.adb.root.failed", ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error(StringResources.get("common.adb.execute.root.failed", ${e.message}", e)
        }
    }

    override suspend fun executeRemount(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "remount")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(output.ifEmpty { StringResources.get("common.adb.remount.success") })
            } else {
                AdbResult.Error(StringResources.get("common.adb.remount.failed", ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error(StringResources.get("common.adb.execute.remount.failed", ${e.message}", e)
        }
    }

    override suspend fun rebootDevice(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "reboot")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(StringResources.get("common.adb.reboot.sent"))
            } else {
                AdbResult.Error(StringResources.get("common.adb.reboot.failed", ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error(StringResources.get("common.adb.execute.reboot.failed", ${e.message}", e)
        }
    }

    override suspend fun rebootRecovery(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

            val processBuilder = ProcessBuilder(adbPath, "-s", device.serialNumber, "reboot", "recovery")
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                AdbResult.Success(StringResources.get("common.adb.reboot.recovery.sent"))
            } else {
                AdbResult.Error(StringResources.get("common.adb.reboot.recovery.failed", ${error.ifEmpty { output }}")
            }
        } catch (e: Exception) {
            AdbResult.Error("执行重启到 Recovery 失败: ${e.message}", e)
        }
    }

    override suspend fun rebootBootloader(device: Device): AdbResult<String> = withContext(Dispatchers.IO) {
        try {
            val adbPath = getAdbPath()
                ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

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
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

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
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

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
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

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
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

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

    override suspend fun listFiles(device: Device, path: String): AdbResult<List<FileInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "shell", "ls", "-la", path
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode != 0) {
                    return@withContext AdbResult.Error("列出文件失败: ${error.ifEmpty { output }}")
                }

                val files = output.lines()
                    .filter { it.isNotBlank() && !it.startsWith("total") }
                    .mapNotNull { line ->
                        parseFileInfo(line, path)
                    }

                AdbResult.Success(files)
            } catch (e: Exception) {
                AdbResult.Error("列出文件失败: ${e.message}", e)
            }
        }

    /**
     * 解析 ls -la 输出的单行
     * 示例: drwxrwxr-x 2 root root 4096 2024-06-09 10:30 folder
     */
    private fun parseFileInfo(line: String, basePath: String): FileInfo? {
        try {
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 9) return null

            val permissions = parts[0]
            val isDirectory = permissions.startsWith("d")
            val size = parts[4].toLongOrNull() ?: 0

            // 文件名可能包含空格，所以从第8个元素开始拼接
            val name = parts.drop(8).joinToString(" ")
            if (name == "." || name == "..") return null

            // 时间格式: parts[5] parts[6] parts[7] 例如: 2024-06-09 10:30
            val modifiedTime = "${parts[5]} ${parts[6]}"

            val fullPath = if (basePath.endsWith("/")) {
                "$basePath$name"
            } else {
                "$basePath/$name"
            }

            return FileInfo(
                name = name,
                isDirectory = isDirectory,
                size = size,
                permissions = permissions,
                modifiedTime = modifiedTime,
                fullPath = fullPath
            )
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun pullFile(device: Device, remotePath: String, localPath: File): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

                // 确保本地目录存在
                localPath.parentFile?.mkdirs()

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "pull", remotePath, localPath.absolutePath
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success("文件下载成功: ${localPath.absolutePath}\n$output")
                } else {
                    AdbResult.Error("下载文件失败: ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("下载文件失败: ${e.message}", e)
            }
        }

    override suspend fun pushFile(device: Device, localFile: File, remotePath: String): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                if (!localFile.exists()) {
                    return@withContext AdbResult.Error("本地文件不存在: ${localFile.absolutePath}")
                }

                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "push", localFile.absolutePath, remotePath
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success("文件上传成功: $remotePath\n$output")
                } else {
                    AdbResult.Error("上传文件失败: ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("上传文件失败: ${e.message}", e)
            }
        }

    override suspend fun deleteFile(device: Device, path: String): AdbResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val adbPath = getAdbPath()
                    ?: return@withContext AdbResult.Error(StringResources.get("common.adb.not.detected"))

                val processBuilder = ProcessBuilder(
                    adbPath, "-s", device.serialNumber, "shell", "rm", "-rf", path
                )
                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    AdbResult.Success("文件已删除: $path")
                } else {
                    AdbResult.Error("删除文件失败: ${error.ifEmpty { output }}")
                }
            } catch (e: Exception) {
                AdbResult.Error("删除文件失败: ${e.message}", e)
            }
        }
}
