package com.evgenysobko.diploma

import com.evgenysobko.diploma.agent.AgentMain
import com.evgenysobko.diploma.agent.TracerTrampoline
import com.evgenysobko.diploma.tracer.TracerClassFileTransformer
import com.evgenysobko.diploma.tracer.TracerHookImpl
import com.evgenysobko.diploma.util.log
import com.intellij.execution.process.OSProcessUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.io.isFile
import com.sun.tools.attach.VirtualMachine
import java.lang.instrument.Instrumentation
import kotlin.system.measureTimeMillis

object AgentLoader {

    val instrumentation: Instrumentation?
        get() = if (ensureJavaAgentLoaded) AgentMain.savedInstrumentationInstance else null

    val ensureJavaAgentLoaded: Boolean by lazy { doLoadJavaAgent() }

    val ensureTracerHooksInstalled: Boolean by lazy { doInstallTracerHooks() }

    private fun doLoadJavaAgent(): Boolean {
        val agentLoadedAtStartup = try {
            Class.forName("com.evgenysobko.diploma.agent.AgentMainKt", false, null)
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        if (agentLoadedAtStartup) {
            Logger.getInstance(this.javaClass).info("Java agent was loaded at startup")
        } else {
            try {
                val overhead = measureTimeMillis { tryLoadAgentAfterStartup() }
                log("Java agent was loaded on demand in $overhead ms")
            } catch (e: Throwable) {
                log(e)
                return false
            }
        }

        val instrumentation = checkNotNull(AgentMain.savedInstrumentationInstance)
        if (!instrumentation.isRetransformClassesSupported) {
            log("[Tracer] The current JVM configuration does not allow class retransformation")
            return false
        }

        return true
    }

    private fun tryLoadAgentAfterStartup() {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.evgenysobko.diploma"))
            ?: error("Failed to find our own plugin")
        val agentDir = plugin.pluginPath.resolve("agent")

        val javaAgent = agentDir.resolve("agent.jar")
        check(javaAgent.isFile()) { "Could not find agent.jar at $javaAgent" }

        val vm = VirtualMachine.attach(OSProcessUtil.getApplicationPid())
        try {
            vm.loadAgent(javaAgent.toAbsolutePath().toString())
            log("AGENT LOADED")
        } catch (e: Exception) {
            log("error = $e")
        }
        finally {
            vm.detach()
        }
    }

    private fun doInstallTracerHooks(): Boolean {
        val instrumentation = instrumentation ?: return false
        TracerTrampoline.installHook(TracerHookImpl())
        instrumentation.addTransformer(TracerClassFileTransformer(), true)
        return true
    }
}
