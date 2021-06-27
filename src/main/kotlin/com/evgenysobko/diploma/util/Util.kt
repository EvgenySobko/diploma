package com.evgenysobko.diploma.util

import com.intellij.openapi.diagnostic.Logger
import java.text.NumberFormat

private val formatter = NumberFormat.getInstance()
fun Long.formatNsInMs(): String = formatter.format(this / 1_000_000) + " ms"
fun Long.fromNsToMs(): Long = this / 1_000_000

fun <T> T.log(message: Any?) = Logger.getInstance(this!!::class.java).warn(message.toString())
