package com.evgenysobko.diploma.agent

import com.evgenysobko.diploma.tracer.TracerClassFileTransformer
import com.evgenysobko.diploma.tracer.TracerHookImpl
import com.intellij.execution.process.OSProcessUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.io.isFile
import com.sun.tools.attach.VirtualMachine
import java.lang.instrument.Instrumentation
import kotlin.system.measureTimeMillis

object AgentLoader {
    private val LOG = Logger.getInstance(AgentLoader::class.java)

    val instrumentation: Instrumentation?
        get() = if (ensureJavaAgentLoaded) AgentMain.savedInstrumentationInstance else null

    val ensureJavaAgentLoaded: Boolean by lazy { doLoadJavaAgent() }

    val ensureTracerHooksInstalled: Boolean by lazy { doInstallTracerHooks() }

    // Note: this method can take around 200 ms or so.
    private fun doLoadJavaAgent(): Boolean {
        val agentLoadedAtStartup = try {
            // Until the agent is loaded, we cannot trigger symbol resolution for its
            // classes---otherwise NoClassDefFoundError is imminent. So we use reflection.
            Class.forName("com.google.idea.perf.agent.AgentMain", false, null)
            true
        }
        catch (e: ClassNotFoundException) {
            false
        }

        if (agentLoadedAtStartup) {
            LOG.info("Java agent was loaded at startup")
        }
        else {
            try {
                val overhead = measureTimeMillis { tryLoadAgentAfterStartup() }
                LOG.info("Java agent was loaded on demand in $overhead ms")
            }
            catch (e: Throwable) {
                val msg = """
                    [Tracer] Failed to attach the instrumentation agent after startup.
                    On JDK 9+, make sure jdk.attach.allowAttachSelf is set to true.
                    Alternatively, you can attach the agent at startup via the -javaagent flag.
                    """.trimIndent()
                Notification("Tracer", "", msg, NotificationType.ERROR).notify(null)
                LOG.warn(e)
                return false
            }
        }

        // Disable tracing entirely if class retransformation is not supported.
        val instrumentation = checkNotNull(AgentMain.savedInstrumentationInstance)
        if (!instrumentation.isRetransformClassesSupported) {
            val msg = "[Tracer] The current JVM configuration does not allow class retransformation"
            Notification("Tracer", "", msg, NotificationType.ERROR).notify(null)
            LOG.warn(msg)
            return false
        }

        return true
    }

    // Note: this method can throw a variety of exceptions.
    private fun tryLoadAgentAfterStartup() {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.google.ide-perf"))
            ?: error("Failed to find our own plugin")
        val agentDir = plugin.pluginPath.resolve("agent")

        val javaAgent = agentDir.resolve("agent.jar")
        check(javaAgent.isFile()) { "Could not find agent.jar at $javaAgent" }

        val vm = VirtualMachine.attach(OSProcessUtil.getApplicationPid())
        try {
            vm.loadAgent(javaAgent.toAbsolutePath().toString())
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
