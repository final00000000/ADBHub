package com.zhang.adbhub.common.model

/**
 * ADB 操作结果
 */
sealed class AdbResult<out T> {
    data class Success<T>(val data: T) : AdbResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : AdbResult<Nothing>()
}

fun <T> AdbResult<T>.getOrNull(): T? = when (this) {
    is AdbResult.Success -> data
    is AdbResult.Error -> null
}

fun <T> AdbResult<T>.getOrThrow(): T = when (this) {
    is AdbResult.Success -> data
    is AdbResult.Error -> throw exception ?: Exception(message)
}
