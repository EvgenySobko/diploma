package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.util.ConcurrentCopyOnGrowList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object TracerConfig {
    private val tracepoints = ConcurrentCopyOnGrowList<MethodTracepoint>()
    private val lock = ReentrantLock()
    private val methodIds = mutableMapOf<MethodFqName, Int>()
    private val traceRequests = mutableListOf<TraceRequest>()

    fun appendTraceRequest(request: TraceRequest) {
        lock.withLock {
            traceRequests.add(request)
        }
    }

    fun getAllRequests(): List<TraceRequest> {
        lock.withLock {
            return ArrayList(traceRequests)
        }
    }

    fun clearAllRequests(): List<TraceRequest> {
        lock.withLock {
            val copy = ArrayList(traceRequests)
            traceRequests.clear()
            return copy
        }
    }

    fun getMethodTracepoint(methodId: Int): MethodTracepoint = tracepoints.get(methodId)

    /** Returns true if the given class might have methods that need to be traced. */
    fun shouldInstrumentClass(clazz: String): Boolean {
        lock.withLock {
            // Currently O(n) in the number of trace requests---could be optimized if needed.
            return traceRequests.any { request ->
                request.config.enabled && request.matcher.mightMatchMethodInClass(clazz)
            }
        }
    }

    /** Returns [MethodTraceData] based on the most recent matching [TraceRequest]. */
    fun getMethodTraceData(m: MethodFqName): MethodTraceData? {
        lock.withLock {
            val recentMatch = traceRequests.asReversed().firstOrNull { it.matcher.matches(m) }
                ?: return null

            val methodId = methodIds.getOrPut(m) {
                tracepoints.append(MethodTracepoint(m))
            }

            // Sync tracepoint flags.
            val config = recentMatch.config
            val tracepoint = getMethodTracepoint(methodId)
            tracepoint.measureWallTime = !config.countOnly

            return MethodTraceData(methodId, config)
        }
    }
}
