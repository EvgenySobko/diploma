package com.evgenysobko.diploma.toolwindow

import com.evgenysobko.diploma.AgentLoader
import com.evgenysobko.diploma.tracer.*
import com.evgenysobko.diploma.util.EPFinder
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.attach
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class ToolWindowFactory : ToolWindowFactory, DumbAware {

    private lateinit var myToolWindow: ToolWindowContent

    private val listOfEPWithPluginName: MutableMap<String, MutableList<String>> = mutableMapOf()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        myToolWindow = ToolWindowContent
        myToolWindow.init(toolWindow)
        toolWindow.apply {
            hide { disposable.dispose() }
            show {
                if (AgentLoader.ensureJavaAgentLoaded) {
                    SwingUtilities.invokeLater {
                        disposable.also {
                            startTransformClasses(it)
                            updateData(it)
                        }
                    }
                }
            }
        }
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(myToolWindow.content, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun startTransformClasses(disposable: Disposable) {
        val controller = TracerController(disposable)
        val extensionPoints = EPFinder.getExtendedPoints()
        val config = MethodConfig()
        val requests = mutableListOf<TraceRequest>()
        extensionPoints.keys.forEach { name ->
            if (listOfEPWithPluginName[name].isNullOrEmpty()) {
                listOfEPWithPluginName[name] = extensionPoints[name]!!
            } else {
                listOfEPWithPluginName[name]!!.addAll(extensionPoints[name]!!)
            }
            extensionPoints[name]?.forEach {
                val request = TracerConfigUtil.appendTraceRequest(MethodFqName(it, "*", "*"), config)
                requests.add(request)
            }
        }
        val affectedClasses = TracerConfigUtil.getAffectedClasses(requests)
        controller.retransformClasses(affectedClasses)
        CallTreeManager.clearCallTrees()
    }

    private fun updateData(disposable: Disposable) {
        val refreshFuture = Executors.newScheduledThreadPool(10)
            .scheduleWithFixedDelay(::updateCallTree, 0, 3000, TimeUnit.MILLISECONDS)
        disposable.attach { refreshFuture.cancel(false) }
    }

    private fun updateCallTree() {
        val result = mutableSetOf<EPWithPluginNameAndTracepointStats>()
        val treeSnapshot = CallTreeManager.getCallTreeSnapshotAllThreadsMerged()
        val stats = CallTreeUtil.computeFlatTracepointStats(treeSnapshot)

        if (stats.isNotEmpty()) {
            listOfEPWithPluginName.keys.forEach { name ->
                listOfEPWithPluginName[name]?.forEach { extension ->
                    val tracepointStatsForPlugin = mutableSetOf<TracepointStats>()
                    stats.forEach { tracepointStats ->
                        if (extension == tracepointStats.tracepoint.detailedName.split(" ")[1].split("\n")[0]) {
                            tracepointStatsForPlugin.add(tracepointStats)
                        }
                    }
                    if (name.isNotEmpty() && tracepointStatsForPlugin.isNotEmpty()) {
                        result.add(EPWithPluginNameAndTracepointStats(name, tracepointStatsForPlugin))
                    }
                }
            }
            if (result.isNotEmpty()) myToolWindow.updateData(result)
        }
    }
}

data class EPWithPluginNameAndTracepointStats(
    val pluginName: String,
    val stats: MutableSet<TracepointStats>
) {

    fun mergeWithOther(epWithPluginNameAndTracepointStats: EPWithPluginNameAndTracepointStats) {
        if (epWithPluginNameAndTracepointStats.pluginName == this.pluginName) {
            this.stats.addAll(epWithPluginNameAndTracepointStats.stats)
        }
    }
}