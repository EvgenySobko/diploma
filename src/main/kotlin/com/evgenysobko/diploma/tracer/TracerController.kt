package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.AgentLoader
import com.evgenysobko.diploma.util.EPFinder
import com.evgenysobko.diploma.util.ExecutorWithExceptionLogging
import com.evgenysobko.diploma.util.log
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.util.Disposer
import java.lang.instrument.UnmodifiableClassException

class TracerController(parentDisposable: Disposable) : Disposable {

    private val executor = ExecutorWithExceptionLogging("Tracer", 1)

    init {
        Disposer.register(parentDisposable, this)

        executor.execute {
            if (!AgentLoader.ensureTracerHooksInstalled) {
                log("Failed to install instrumentation agent (see idea.log)")
            }
        }
    }

    override fun dispose() {
        executor.shutdownNow()
    }

    fun retransformClasses(classes: Collection<Class<*>>) {
        classes.forEach { log("class = $it") }
        if (classes.isEmpty()) return
        val instrumentation = AgentLoader.instrumentation ?: return

        log("Retransforming ${classes.size} classes")
        for (clazz in classes) {
            invokeAndWaitIfNeeded {}
            try {
                instrumentation.retransformClasses(clazz)
            } catch (e: UnmodifiableClassException) {
                log("Cannot instrument non-modifiable class: ${clazz.name}")
            } catch (e: Throwable) {
                log("Failed to retransform class: ${clazz.name} – $e")
            }
        }
    }
}
