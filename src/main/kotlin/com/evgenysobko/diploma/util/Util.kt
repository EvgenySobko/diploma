package com.evgenysobko.diploma.util

import com.intellij.openapi.diagnostic.Logger
import java.text.NumberFormat
import kotlin.math.absoluteValue

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <T> Sequence<T>.sumByLong(selector: (T) -> Long): Long = asIterable().sumByLong(selector)

private val formatter = NumberFormat.getInstance()
fun formatNum(num: Long): String = formatter.format(num)
fun formatNum(num: Long, unit: String): String = "${formatNum(num)} $unit"
fun formatNum(num: Double): String = formatter.format(num)
fun formatNsInMs(ns: Long): String = formatNum(ns / 1_000_000, "ms")
fun formatMsInSeconds(ms: Long): String = formatNum(ms / 1_000, "s")

fun formatNsInBestUnit(ns: Long): String {
    return when (ns.absoluteValue) {
        in 0 until 10_000 -> formatNum(ns, "ns")
        in 10_000 until 10_000_000 -> formatNum(ns / 1_000, "μs")
        else -> formatNum(ns / 1_000_000, "ms")
    }
}

fun <T> T.log(message: Any?) = Logger.getInstance(this!!::class.java).warn(message.toString())
