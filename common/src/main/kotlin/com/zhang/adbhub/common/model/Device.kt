package com.zhang.adbhub.common.model

/**
 * ADB 设备数据模型
 */
data class Device(
    val serialNumber: String,
    val model: String? = null,
    val state: DeviceState = DeviceState.UNKNOWN,
    val transportId: String? = null
)

enum class DeviceState {
    ONLINE,
    OFFLINE,
    UNAUTHORIZED,
    UNKNOWN
}
