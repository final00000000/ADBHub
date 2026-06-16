package com.zhang.adbhub.common.utils

/**
 * ADB 命令输出解析工具类
 * 用于从原始命令输出中提取关键信息
 */
object CommandOutputParser {

    /**
     * 解析结果数据类
     * @param summary 关键信息摘要（国际化）
     * @param rawOutput 原始输出
     */
    data class ParsedOutput(
        val summary: String,
        val rawOutput: String
    )

    /**
     * 解析车机信息命令输出
     * 命令：sh -c "printf 'Model: '; getprop ro.product.model; ..."
     */
    fun parseVehicleInfo(output: String): ParsedOutput {
        val summary = StringBuilder()
        val lines = output.lines()

        var model: String? = null
        var brand: String? = null
        var androidVersion: String? = null
        var buildId: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Model:") -> model = trimmed.substringAfter("Model:").trim()
                trimmed.startsWith("Brand:") -> brand = trimmed.substringAfter("Brand:").trim()
                trimmed.startsWith("Android:") -> androidVersion = trimmed.substringAfter("Android:").trim()
                trimmed.startsWith("Build:") -> buildId = trimmed.substringAfter("Build:").trim()
            }
        }

        if (listOf(model, brand, androidVersion, buildId).all { it.isNullOrBlank() }) {
            return ParsedOutput(summary = output, rawOutput = output)
        }

        summary.appendLine(StringResources.get("command.vehicle.info.summary"))
        model?.let { summary.appendLine("  ${StringResources.get("command.vehicle.info.model")}: $it") }
        brand?.let { summary.appendLine("  ${StringResources.get("command.vehicle.info.brand")}: $it") }
        androidVersion?.let { summary.appendLine("  ${StringResources.get("command.vehicle.info.android")}: $it") }
        buildId?.let { summary.appendLine("  ${StringResources.get("command.vehicle.info.build")}: $it") }

        return ParsedOutput(
            summary = summary.toString().ifEmpty { output },
            rawOutput = output
        )
    }

    /**
     * 解析显示信息命令输出
     * 命令：sh -c "wm size; wm density"
     */
    fun parseDisplayInfo(output: String): ParsedOutput {
        val summary = StringBuilder()
        val lines = output.lines()

        var physicalSize: String? = null
        var overrideSize: String? = null
        var physicalDensity: String? = null
        var overrideDensity: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Physical size:") -> physicalSize = trimmed.substringAfter("Physical size:").trim()
                trimmed.startsWith("Override size:") -> overrideSize = trimmed.substringAfter("Override size:").trim()
                trimmed.startsWith("Physical density:") -> physicalDensity = trimmed.substringAfter("Physical density:").trim()
                trimmed.startsWith("Override density:") -> overrideDensity = trimmed.substringAfter("Override density:").trim()
            }
        }

        if (listOf(physicalSize, overrideSize, physicalDensity, overrideDensity).all { it.isNullOrBlank() }) {
            return ParsedOutput(summary = output, rawOutput = output)
        }

        summary.appendLine(StringResources.get("command.display.info.summary"))
        summary.appendLine()
        summary.appendLine(StringResources.get("command.display.info.resolution"))
        physicalSize?.let { summary.appendLine("  ${StringResources.get("command.display.info.physical")}: $it") }
        overrideSize?.let { summary.appendLine("  ${StringResources.get("command.display.info.override")}: $it") }

        summary.appendLine()
        summary.appendLine(StringResources.get("command.display.info.density"))
        physicalDensity?.let { summary.appendLine("  ${StringResources.get("command.display.info.physical")}: $it") }
        overrideDensity?.let { summary.appendLine("  ${StringResources.get("command.display.info.override")}: $it") }

        return ParsedOutput(
            summary = summary.toString().ifEmpty { output },
            rawOutput = output
        )
    }

    /**
     * 解析当前焦点窗口命令输出
     * 命令：sh -c "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp|mFocusedWindow'"
     */
    fun parseCurrentFocus(output: String): ParsedOutput {
        val summary = StringBuilder()
        val lines = output.lines()

        var currentFocus: String? = null
        var focusedApp: String? = null
        var focusedWindow: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.contains("mCurrentFocus") -> {
                    currentFocus = trimmed.substringAfter("mCurrentFocus=").trim()
                }
                trimmed.contains("mFocusedApp") -> {
                    focusedApp = trimmed.substringAfter("mFocusedApp=").trim()
                }
                trimmed.contains("mFocusedWindow") -> {
                    focusedWindow = trimmed.substringAfter("mFocusedWindow=").trim()
                }
            }
        }

        if (listOf(currentFocus, focusedApp, focusedWindow).all { it.isNullOrBlank() }) {
            return ParsedOutput(summary = output, rawOutput = output)
        }

        summary.appendLine(StringResources.get("command.focus.info.summary"))
        currentFocus?.let {
            val windowName = extractWindowName(it)
            summary.appendLine("  ${StringResources.get("command.focus.info.current")}: $windowName")
        }
        focusedApp?.let {
            val appName = extractAppName(it)
            summary.appendLine("  ${StringResources.get("command.focus.info.app")}: $appName")
        }

        return ParsedOutput(
            summary = summary.toString().ifEmpty { output },
            rawOutput = output
        )
    }

    /**
     * 解析截屏命令输出
     * 命令：sh -c "screencap ..."
     */
    fun parseScreenshot(output: String): ParsedOutput {
        val summary = StringBuilder()

        if (output.contains("saved:") || output.contains("/Pictures/")) {
            val pathMatch = Regex("""saved:(.+)|(/sdcard/.+\.png)""").find(output)
            val path = pathMatch?.groupValues?.firstOrNull { it.isNotBlank() && it != "saved:" }?.trim()

            summary.appendLine(StringResources.get("command.screenshot.success"))
            path?.let { summary.appendLine("  ${StringResources.get("command.screenshot.path")}: $it") }
        } else if (output.contains("error") || output.contains("failed")) {
            summary.appendLine(StringResources.get("command.screenshot.failed"))
        } else {
            summary.appendLine(StringResources.get("command.screenshot.executed"))
        }

        return ParsedOutput(
            summary = summary.toString().ifEmpty { output },
            rawOutput = output
        )
    }

    /**
     * 从窗口字符串中提取窗口名称
     * 例如：Window{abc123 u0 com.example.app/com.example.MainActivity} -> com.example.app/com.example.MainActivity
     */
    private fun extractWindowName(windowStr: String): String {
        val match = Regex("""([a-zA-Z0-9_.]+/[a-zA-Z0-9_.$]+)""").find(windowStr)
        return match?.value ?: windowStr
    }

    /**
     * 从应用字符串中提取应用名称
     * 例如：ActivityRecord{abc123 u0 com.example.app/.MainActivity} -> com.example.app
     */
    private fun extractAppName(appStr: String): String {
        val match = Regex("""([a-zA-Z0-9_.]+)""").find(appStr)
        return match?.value ?: appStr
    }

    /**
     * 判断命令类型并智能解析
     */
    fun smartParse(commandTitle: String, output: String): ParsedOutput {
        return when {
            commandTitle.contains("车机信息") || commandTitle.contains("Vehicle") ||
                commandTitle.contains("info") && output.contains("Model:") -> parseVehicleInfo(output)

            commandTitle.contains("屏幕") || commandTitle.contains("Display") ||
                output.contains("Physical size:") -> parseDisplayInfo(output)

            commandTitle.contains("焦点") || commandTitle.contains("Focus") || commandTitle.contains("focus") ||
                output.contains("mCurrentFocus") -> parseCurrentFocus(output)

            commandTitle.contains("截屏") || commandTitle.contains("Screenshot") ||
                output.contains("screencap") -> parseScreenshot(output)

            else -> ParsedOutput(
                summary = output,
                rawOutput = output
            )
        }
    }
}
