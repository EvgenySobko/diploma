package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.agent.AgentLoader
import com.evgenysobko.diploma.tracer.*
import com.evgenysobko.diploma.util.ExecutorWithExceptionLogging
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import java.lang.instrument.UnmodifiableClassException

class TracerController(
    parentDisposable: Disposable
) : Disposable {
    companion object {
        private val LOG = Logger.getInstance(TracerController::class.java)
    }

    private val executor = ExecutorWithExceptionLogging("Tracer", 1)

    init {
        Disposer.register(parentDisposable, this)

        executor.execute {
            if (!AgentLoader.ensureTracerHooksInstalled) {
                Logger.getInstance(this::class.java).info("Failed to install instrumentation agent (see idea.log)")
            }
        }
    }

    override fun dispose() {
        // TODO: Should we wait for tasks to finish (under a modal progress dialog)?
        executor.shutdownNow()
    }

    private fun retransformClasses(classes: Collection<Class<*>>) {
        if (classes.isEmpty()) return
        val instrumentation = AgentLoader.instrumentation ?: return

        LOG.info("Retransforming ${classes.size} classes")
        var count = 0.0
        for (clazz in classes) {
            // Retransforming classes tends to lock up all threads, so to keep
            // the UI responsive it helps to flush the EDT queue in between.
            invokeAndWaitIfNeeded {}
            try {
                instrumentation.retransformClasses(clazz)
            }
            catch (e: UnmodifiableClassException) {
                LOG.info("Cannot instrument non-modifiable class: ${clazz.name}")
            }
            catch (e: Throwable) {
                LOG.error("Failed to retransform class: ${clazz.name}", e)
            }
        }
    }
}
