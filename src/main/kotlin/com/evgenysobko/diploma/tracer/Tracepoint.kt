package com.evgenysobko.diploma.tracer

import org.objectweb.asm.Type
import java.util.*
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface Tracepoint {
    val displayName: String
    val detailedName: String
    val measureWallTime: Boolean

    companion object {
        val ROOT = SimpleTracepoint("[root]", "the synthetic root of the call tree")
    }
}

class SimpleTracepoint(
    override val displayName: String,
    override val detailedName: String = displayName,
    override val measureWallTime: Boolean = true,
) : Tracepoint {
    override fun toString(): String = displayName
}

class MethodTracepoint(
    private val fqName: MethodFqName,
) : Tracepoint {
    override val displayName = "${fqName.clazz.substringAfterLast('.')}.${fqName.method}"

    override val detailedName by lazy(PUBLICATION) {
        buildString {
            val argTypes = Type.getArgumentTypes(fqName.desc)
            val argString = argTypes.joinToString { it.className.substringAfterLast('.') }
            appendLine("Class: ${fqName.clazz}")
            append("Method: ${fqName.method}($argString)")
        }
    }

    @Volatile // Value may change over time as new trace requests come in.
    override var measureWallTime: Boolean = true

    // Note: reference equality is sufficient currently because TracerConfig maintains
    // a single canonical MethodTracepoint per traced method.

    override fun toString(): String = displayName
}

/**
 * A [Tracepoint] representing a method call with a specific set of arguments.
 * Two instances with the same backing [method] and the same [argStrings] are considered equal.
 */
class MethodTracepointWithArgs(
    private val method: Tracepoint,
    private val argStrings: Array<String>
) : Tracepoint by method {
    private val cachedHashCode = Objects.hash(method, argStrings.contentDeepHashCode())

    override val displayName = "${method.displayName}: ${argStrings.joinToString(", ")}"

    override val detailedName by lazy(PUBLICATION) {
        buildString {
            append(method.detailedName)
            for (arg in argStrings) {
                append("\nArg: $arg")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other !is MethodTracepointWithArgs -> false
            hashCode() != other.hashCode() -> false
            else -> method == other.method && argStrings.contentEquals(other.argStrings)
        }
    }

    override fun hashCode(): Int = cachedHashCode

    override fun toString(): String = displayName
}
