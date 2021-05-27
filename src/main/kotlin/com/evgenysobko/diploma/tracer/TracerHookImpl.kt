package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.agent.TracerHook
import com.intellij.openapi.diagnostic.Logger

/** Dispatches method entry/exit events to the [CallTreeManager]. */
class TracerHookImpl : TracerHook {

    override fun enter(methodId: Int, args: Array<Any>?) {
        doWithExceptionLogging {
            val methodTracepoint = TracerConfig.getMethodTracepoint(methodId)

            val tracepoint =
                if (args != null) {
                    val argStrings = Array(args.size) { args[it].toString() }
                    MethodTracepointWithArgs(methodTracepoint, argStrings)
                } else {
                    methodTracepoint
                }

            CallTreeManager.enter(tracepoint)
        }
    }

    override fun leave() {
        doWithExceptionLogging {
            CallTreeManager.leave()
        }
    }

    // In case there are bugs in the tracer, catch exceptions to protect user code.
    private inline fun doWithExceptionLogging(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            val logger = Logger.getInstance(TracerHookImpl::class.java)
            logger.error(e)
        }
    }

    companion object {
        init {
            // Trigger class loading for CallTreeManager early so that it doesn't happen
            // during tracing. This reduces the chance of invoking an instrumented method
            // from a tracing hook (causing infinite recursion).
            CallTreeManager.enter(Tracepoint.ROOT)
            CallTreeManager.leave()
            CallTreeManager.clearCallTrees()
        }
    }
}
