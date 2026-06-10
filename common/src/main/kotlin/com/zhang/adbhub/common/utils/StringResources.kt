package com.zhang.adbhub.common.utils

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

object StringResources {
    private val bundle: Properties by lazy {
        Properties().apply {
            val inputStream = javaClass.classLoader?.getResourceAsStream("strings.properties")
            if (inputStream != null) {
                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                    load(reader)
                }
            }
        }
    }

    fun get(key: String, vararg args: Any): String {
        val value = bundle.getProperty(key) ?: key
        return if (args.isEmpty()) {
            value
        } else {
            String.format(value, *args)
        }
    }
}
