package com.zhang.adbhub.desktop.utils

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

object StringResources {
    private val properties = Properties()

    init {
        try {
            javaClass.getResourceAsStream("/strings.properties")?.use { stream ->
                InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                    properties.load(reader)
                }
            }
        } catch (e: Exception) {
            println("Failed to load string resources: ${e.message}")
        }
    }

    fun get(key: String): String {
        return properties.getProperty(key) ?: key
    }

    fun get(key: String, vararg args: Any): String {
        val template = get(key)
        return String.format(template, *args)
    }
}
