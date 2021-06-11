package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.agent.TracerHook
import com.intellij.openapi.diagnostic.Logger

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
            CallTreeManager.enter(Tracepoint.ROOT)
            CallTreeManager.leave()
            CallTreeManager.clearCallTrees()
        }
    }
}
