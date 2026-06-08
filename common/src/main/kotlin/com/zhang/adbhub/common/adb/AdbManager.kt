package com.zhang.adbhub.common.adb

import com.zhang.adbhub.common.model.Device
import com.zhang.adbhub.common.model.AdbResult
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * ADB 操作接口
 */
interface AdbManager {
    /**
     * 获取已连接的设备列表
     */
    suspend fun getDevices(): AdbResult<List<Device>>

    /**
     * 推送 APK 到设备
     */
    suspend fun pushApk(device: Device, apkFile: File): AdbResult<Unit>

    /**
     * 获取设备日志流
     */
    fun getLogcatFlow(device: Device): Flow<String>

    /**
     * 停止日志流
     */
    fun stopLogcat(device: Device)

    /**
     * 导出日志到文件
     */
    suspend fun exportLogs(device: Device, outputFile: File, maxLines: Int = 10000): AdbResult<Unit>

    // 设备操作命令
    /**
     * 以 root 权限重启 ADB
     */
    suspend fun executeRoot(device: Device): AdbResult<String>

    /**
     * 重新挂载分区为可写
     */
    suspend fun executeRemount(device: Device): AdbResult<String>

    /**
     * 重启设备
     */
    suspend fun rebootDevice(device: Device): AdbResult<String>

    /**
     * 重启到 Recovery 模式
     */
    suspend fun rebootRecovery(device: Device): AdbResult<String>

    /**
     * 重启到 Bootloader 模式
     */
    suspend fun rebootBootloader(device: Device): AdbResult<String>

    // 应用管理命令
    /**
     * 启动指定应用
     */
    suspend fun startApp(device: Device, packageName: String, activityName: String): AdbResult<String>

    /**
     * 获取应用信息
     */
    suspend fun getAppInfo(device: Device, packageName: String): AdbResult<String>

    /**
     * 停止应用
     */
    suspend fun stopApp(device: Device, packageName: String): AdbResult<String>

    /**
     * 清除应用数据
     */
    suspend fun clearAppData(device: Device, packageName: String): AdbResult<String>
}
