package com.evgenysobko.diploma.util

class ConcurrentCopyOnGrowList<T: Any>(initialCapacity: Int = 0) {

    @Volatile
    private var data = arrayOfNulls<Any?>(initialCapacity)

    private var tail = 0

    fun get(index: Int): T {
        @Suppress("UNCHECKED_CAST")
        return data[index] as T
    }

    @Synchronized
    fun append(element: T): Int {
        val index = tail++

        if (index >= data.size) {
            val newCapacity = if (data.isEmpty()) 1 else 2 * data.size
            val newData = arrayOfNulls<Any?>(newCapacity)
            System.arraycopy(data, 0, newData, 0, data.size)
            data = newData
        }

        data[index] = element
        return index
    }
}
