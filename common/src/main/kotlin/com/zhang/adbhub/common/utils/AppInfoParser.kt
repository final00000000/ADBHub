package com.zhang.adbhub.common.utils

import com.zhang.adbhub.common.utils.StringResources

/**
 * 应用信息解析工具类
 * 用于从 dumpsys package 输出中提取关键信息
 */
object AppInfoParser {

    /**
     * 从 dumpsys package 输出中提取关键信息
     */
    fun extractKeyAppInfo(dumpsysOutput: String): String {
        val lines = dumpsysOutput.lines()
        val result = StringBuilder()

        // 检查是否找到包
        if (dumpsysOutput.contains("Unable to find package:") ||
            dumpsysOutput.trim().isEmpty()) {
            return dumpsysOutput.ifEmpty { "未找到应用信息" }
        }

        var packageName: String? = null
        var versionCode: String? = null
        var versionName: String? = null
        var targetSdk: String? = null
        var minSdk: String? = null
        var firstInstallTime: String? = null
        var lastUpdateTime: String? = null
        var dataDir: String? = null
        var userId: String? = null
        var enabled: String? = null
        val activities = mutableListOf<String>()
        val permissions = mutableListOf<String>()

        var inActivities = false
        var inPermissions = false
        var activityCount = 0

        for (line in lines) {
            val trimmed = line.trim()

            // 包名
            if (trimmed.startsWith("Package [") && packageName == null) {
                packageName = trimmed.substringAfter("Package [").substringBefore("]")
            }

            // 版本信息
            when {
                trimmed.startsWith("versionCode=") -> {
                    versionCode = trimmed.substringAfter("versionCode=").substringBefore(" ")
                }
                trimmed.startsWith("versionName=") -> {
                    versionName = trimmed.substringAfter("versionName=")
                }
                trimmed.startsWith("targetSdk=") -> {
                    targetSdk = trimmed.substringAfter("targetSdk=").substringBefore(" ")
                }
                trimmed.startsWith("minSdk=") -> {
                    minSdk = trimmed.substringAfter("minSdk=").substringBefore(" ")
                }
                trimmed.startsWith("firstInstallTime=") -> {
                    firstInstallTime = trimmed.substringAfter("firstInstallTime=")
                }
                trimmed.startsWith("lastUpdateTime=") -> {
                    lastUpdateTime = trimmed.substringAfter("lastUpdateTime=")
                }
                trimmed.startsWith("dataDir=") -> {
                    dataDir = trimmed.substringAfter("dataDir=")
                }
                trimmed.startsWith("userId=") -> {
                    userId = trimmed.substringAfter("userId=").substringBefore(" ")
                }
                trimmed.startsWith("enabled=") -> {
                    enabled = trimmed.substringAfter("enabled=").substringBefore(" ")
                }
            }

            // Activity 列表
            if (trimmed.contains("Activity Resolver Table:") || trimmed.contains("android.intent.action.MAIN:")) {
                inActivities = true
                inPermissions = false
            }

            if (inActivities && activityCount < 10) {
                // 匹配 packageName/ActivityName 格式
                val activityMatch = Regex("""([a-zA-Z0-9_.]+)/([a-zA-Z0-9_.$]+)""").find(trimmed)
                if (activityMatch != null && !activities.contains(activityMatch.value)) {
                    activities.add(activityMatch.value)
                    activityCount++
                }

                // 遇到空行或新段落停止
                if (trimmed.isEmpty() || trimmed.startsWith("Receiver Resolver Table:")) {
                    inActivities = false
                }
            }

            // 权限列表
            if (trimmed.startsWith("requested permissions:")) {
                inPermissions = true
                inActivities = false
            } else if (inPermissions && permissions.size < 15) {
                if (trimmed.isEmpty() || trimmed.startsWith("install permissions:")) {
                    inPermissions = false
                } else if (trimmed.startsWith("android.permission.") || trimmed.startsWith("com.")) {
                    val permission = trimmed.substringBefore(":")
                    if (permission.isNotEmpty()) {
                        permissions.add(permission)
                    }
                }
            }
        }

        // 格式化输出
        packageName?.let {
            result.appendLine(StringResources.get("operation.app.info.package.name"))
            result.appendLine("  $it")
            result.appendLine()
        }

        result.appendLine(StringResources.get("operation.app.info.version"))
        versionName?.let { result.appendLine("  ${StringResources.get("operation.app.info.version.name")}: $it") }
        versionCode?.let { result.appendLine("  ${StringResources.get("operation.app.info.version.code")}: $it") }
        targetSdk?.let { result.appendLine("  ${StringResources.get("operation.app.info.target.sdk")}: $it") }
        minSdk?.let { result.appendLine("  ${StringResources.get("operation.app.info.min.sdk")}: $it") }

        result.appendLine()
        result.appendLine(StringResources.get("operation.app.info.install"))
        enabled?.let { result.appendLine("  ${StringResources.get("operation.app.info.enabled")}: $it") }
        firstInstallTime?.let { result.appendLine("  ${StringResources.get("operation.app.info.first.install")}: $it") }
        lastUpdateTime?.let { result.appendLine("  ${StringResources.get("operation.app.info.last.update")}: $it") }
        dataDir?.let { result.appendLine("  ${StringResources.get("operation.app.info.data.dir")}: $it") }
        userId?.let { result.appendLine("  ${StringResources.get("operation.app.info.user.id")}: $it") }

        if (activities.isNotEmpty()) {
            result.appendLine()
            result.appendLine(StringResources.get("operation.app.info.activities", activities.size))
            activities.forEach { result.appendLine("  $it") }
        }

        if (permissions.isNotEmpty()) {
            result.appendLine()
            result.appendLine(StringResources.get("operation.app.info.permissions", permissions.size))
            permissions.forEach { result.appendLine("  $it") }
        }

        return result.toString().ifEmpty { dumpsysOutput }
    }
}
