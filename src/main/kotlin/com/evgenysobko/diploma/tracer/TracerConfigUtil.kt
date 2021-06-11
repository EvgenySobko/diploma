package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.AgentLoader
import com.evgenysobko.diploma.util.GlobMatcher
import com.intellij.openapi.progress.ProgressManager
import org.objectweb.asm.Type

class TraceRequest(
    val matcher: MethodFqMatcher,
    val config: MethodConfig,
)

data class MethodFqName(
    val clazz: String,
    val method: String,
    val desc: String,
)

class MethodConfig(
    val enabled: Boolean = true,
    val countOnly: Boolean = false,
    val tracedParams: List<Int> = emptyList(),
)

class MethodTraceData(
    val methodId: Int,
    val config: MethodConfig,
)

class MethodFqMatcher(methodPattern: MethodFqName) {
    private val classMatcher = GlobMatcher.create(methodPattern.clazz)
    private val methodMatcher = GlobMatcher.create(methodPattern.method)
    private val descMatcher = GlobMatcher.create(methodPattern.desc)

    fun matches(m: MethodFqName): Boolean {
        return classMatcher.matches(m.clazz) &&
                methodMatcher.matches(m.method) &&
                descMatcher.matches(m.desc)
    }

    fun mightMatchMethodInClass(className: String): Boolean {
        return classMatcher.matches(className)
    }

    fun matchesMethodInClass(clazz: Class<*>): Boolean {
        try {
            if (!classMatcher.matches(clazz.name)) return false

            for (m in clazz.declaredMethods) {
                if (methodMatcher.matches(m.name) &&
                    descMatcher.matches(Type.getMethodDescriptor(m))
                ) {
                    return true
                }
            }

            if (methodMatcher.matches("<init>")) {
                for (c in clazz.declaredConstructors) {
                    if (descMatcher.matches(Type.getConstructorDescriptor(c))) {
                        return true
                    }
                }
            }

            return false
        }
        catch (ignored: Throwable) {
            return false
        }
    }
}

object TracerConfigUtil {

    fun appendTraceRequest(methodPattern: MethodFqName, methodConfig: MethodConfig): TraceRequest {
        val matcher = MethodFqMatcher(methodPattern)
        val request = TraceRequest(matcher, methodConfig)
        TracerConfig.appendTraceRequest(request)
        return request
    }

    fun getAffectedClasses(traceRequests: Collection<TraceRequest>): List<Class<*>> {
        if (traceRequests.isEmpty()) return emptyList()
        val instrumentation = AgentLoader.instrumentation ?: return emptyList()

        fun classMightBeAffected(clazz: Class<*>): Boolean {
            ProgressManager.checkCanceled()
            return traceRequests.any { it.matcher.matchesMethodInClass(clazz) }
        }

        return instrumentation.allLoadedClasses.filter(::classMightBeAffected)
    }
}
