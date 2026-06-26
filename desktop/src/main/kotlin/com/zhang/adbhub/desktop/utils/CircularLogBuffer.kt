package com.zhang.adbhub.desktop.utils

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 高效的循环缓冲区，用于存储日志行
 * 避免频繁的大数组复制操作
 * 线程安全：使用读写锁保护并发访问
 */
class CircularLogBuffer<T>(private val maxSize: Int) {
    private val buffer = ArrayDeque<T>(minOf(maxOf(maxSize, 1), 1000))
    private val lock = ReentrantReadWriteLock()

    init {
        require(maxSize > 0) { "maxSize must be positive" }
    }

    /**
     * 添加多行日志
     */
    fun addAll(items: List<T>) {
        if (items.isEmpty()) return
        lock.write {
            if (items.size >= maxSize) {
                buffer.clear()
                buffer.addAll(items.takeLast(maxSize))
                return@write
            }
            buffer.addAll(items)
            trimToMaxSize()
        }
    }

    /**
     * 添加单行日志
     */
    fun add(item: T) {
        lock.write {
            buffer.addLast(item)
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
    fun toList(): List<T> = lock.read {
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
