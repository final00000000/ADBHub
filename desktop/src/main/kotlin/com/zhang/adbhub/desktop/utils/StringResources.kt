package com.zhang.adbhub.desktop.utils

import java.util.*

object StringResources {
    private val bundle: ResourceBundle = ResourceBundle.getBundle("strings")

    fun get(key: String): String {
        return try {
            bundle.getString(key)
        } catch (e: MissingResourceException) {
            key
        }
    }

    fun get(key: String, vararg args: Any): String {
        val template = get(key)
        return String.format(template, *args)
    }
}
