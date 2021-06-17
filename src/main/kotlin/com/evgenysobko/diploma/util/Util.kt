package com.evgenysobko.diploma.util

import com.intellij.openapi.diagnostic.Logger
import java.text.NumberFormat

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Sequence<T>.sumByLong(selector: (T) -> Long): Long = asIterable().sumByLong(selector)

private val formatter = NumberFormat.getInstance()
fun formatNum(num: List<Long>): List<String> = num.map { formatter.format(it) }
fun formatNum(num: Double): String = formatter.format(num)
fun Long.formatNsInMs(): String = formatter.format(this / 1_000_000) + " ms"
fun Long.fromNsToMs(): Long = this / 1_000_000

/*fun formatNsInBestUnit(ns: Long): String {
    return when (ns.absoluteValue) {
        in 0 until 10_000 -> formatNum(ns, "ns")
        in 10_000 until 10_000_000 -> formatNum(ns / 1_000, "μs")
        else -> formatNum(ns / 1_000_000, "ms")
    }
}*/

fun <T> T.log(message: Any?) = Logger.getInstance(this!!::class.java).warn(message.toString())
