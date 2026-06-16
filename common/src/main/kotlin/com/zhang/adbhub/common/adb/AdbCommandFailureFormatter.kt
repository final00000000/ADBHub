package com.zhang.adbhub.common.adb

import com.zhang.adbhub.common.utils.StringResources

object AdbCommandFailureFormatter {
    fun format(
        command: String,
        exitCode: Int,
        stdout: String,
        stderr: String,
        fallbackMessage: String
    ): String {
        // ADB 失败原因通常写入 stderr；部分设备或命令只写 stdout，因此按顺序兜底。
        val detail = stderr.trim()
            .ifEmpty { stdout.trim() }
            .ifEmpty { fallbackMessage.trim() }
            .ifEmpty { StringResources.get("common.adb.failure.default") }

        return StringResources.get(
            "common.adb.failure.format",
            command,
            exitCode,
            detail
        )
    }
}
