package com.zhang.adbhub.desktop.utils

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 高效的循环缓冲区，用于存储日志行
 * 避免频繁的大数组复制操作
 * 线程安全：使用读写锁保护并发访问
 */
class CircularLogBuffer(private val maxSize: Int) {
    private val buffer = ArrayDeque<String>(minOf(maxOf(maxSize, 1), 1000))
    private val lock = ReentrantReadWriteLock()

    init {
        require(maxSize > 0) { "maxSize must be positive" }
    }

    /**
     * 添加多行日志
     */
    fun addAll(lines: List<String>) {
        if (lines.isEmpty()) return
        lock.write {
            if (lines.size >= maxSize) {
                buffer.clear()
                buffer.addAll(lines.takeLast(maxSize))
                return@write
            }
            buffer.addAll(lines)
            trimToMaxSize()
        }
    }

    /**
     * 添加单行日志
     */
    fun add(line: String) {
        lock.write {
            buffer.addLast(line)
            trimToMaxSize()
        }
    }

    /**
     * 清空缓冲区
     */
    fun clear() {
        lock.write {
            buffer.clear()
        }
    }

    /**
     * 获取所有日志行
     */
    fun toList(): List<String> = lock.read {
        buffer.toList()
    }

    /**
     * 获取当前大小
     */
    val size: Int get() = lock.read { buffer.size }

    /**
     * 判断是否为空
     */
    val isEmpty: Boolean get() = lock.read { buffer.isEmpty() }

    private fun trimToMaxSize() {
        while (buffer.size > maxSize) {
            buffer.removeFirst()
        }
    }
}
