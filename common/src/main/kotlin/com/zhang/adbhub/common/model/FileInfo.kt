package com.zhang.adbhub.common.model

/**
 * 文件信息数据模型
 */
data class FileInfo(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val modifiedTime: String,
    val fullPath: String
) {
    /**
     * 格式化文件大小
     */
    fun getFormattedSize(): String {
        if (isDirectory) return "-"

        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}
