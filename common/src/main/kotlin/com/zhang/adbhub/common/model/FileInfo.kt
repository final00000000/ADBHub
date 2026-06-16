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
    private companion object {
        const val BYTE_UNIT = 1024
        const val KILOBYTE = BYTE_UNIT
        const val MEGABYTE = KILOBYTE * BYTE_UNIT
        const val GIGABYTE = MEGABYTE * BYTE_UNIT
    }

    /**
     * 格式化文件大小
     */
    fun getFormattedSize(): String {
        if (isDirectory) return "-"

        return when {
            size < KILOBYTE -> "$size B"
            size < MEGABYTE -> "${size / KILOBYTE} KB"
            size < GIGABYTE -> "${size / MEGABYTE} MB"
            else -> "${size / GIGABYTE} GB"
        }
    }
}
