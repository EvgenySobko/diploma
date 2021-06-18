package com.evgenysobko.diploma.toolwindow

import com.evgenysobko.diploma.tracer.*
import com.evgenysobko.diploma.util.EPFinder
import com.evgenysobko.diploma.util.EPWithPluginName
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.attach
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

class ToolWindowFactory : ToolWindowFactory {

    private lateinit var myToolWindow: ToolWindowContent

    private val listOfEPWithPluginName: MutableList<EPWithPluginName> = mutableListOf()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        myToolWindow = ToolWindowContent
        myToolWindow.init(toolWindow)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(myToolWindow.content, "", false)
        toolWindow.contentManager.addContent(content)
        SwingUtilities.invokeLater {
            toolWindow.disposable.also {
                startTransformClasses(it)
                updateData(it)
            }
        }
    }

    private fun startTransformClasses(disposable: Disposable) {
        val controller = TracerController(disposable)
        val extensionPoints = EPFinder.getExtendedPoints()
        val config = MethodConfig()
        val requests = mutableListOf<TraceRequest>()
        extensionPoints.forEach { epWithName ->
            listOfEPWithPluginName.add(epWithName)
            epWithName.epList.forEach {
                val request = TracerConfigUtil.appendTraceRequest(MethodFqName(it.javaClass.name, "*", "*"), config)
                requests.add(request)
            }
        }
        val affectedClasses = TracerConfigUtil.getAffectedClasses(requests)
        controller.retransformClasses(affectedClasses)
        CallTreeManager.clearCallTrees()
    }

    private fun updateData(disposable: Disposable) {
        val refreshFuture = Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay(::updateCallTree, 0, 3000, TimeUnit.MILLISECONDS)
        disposable.attach { refreshFuture.cancel(false) }
    }

    private fun updateCallTree() {
        val result = mutableSetOf<EPWithPluginNameAndTracepointStats>()
        val treeSnapshot = CallTreeManager.getCallTreeSnapshotAllThreadsMerged()
        val stats = CallTreeUtil.computeFlatTracepointStats(treeSnapshot)

        if (stats.isNotEmpty()) {
            listOfEPWithPluginName.forEach { epWithPluginName ->
                val pluginName = epWithPluginName.name
                val tracepointStatsForPlugin = mutableSetOf<TracepointStats>()
                epWithPluginName.epList.forEach {
                    stats.forEach { tracepointStats ->
                        if (it.javaClass.name == tracepointStats.tracepoint.detailedName.split(" ")[1].split("\n")[0]) {
                            tracepointStatsForPlugin.add(tracepointStats)
                        }
                    }
                }
                if (pluginName.isNotEmpty() && tracepointStatsForPlugin.isNotEmpty()) {
                    result.add(EPWithPluginNameAndTracepointStats(pluginName, tracepointStatsForPlugin))
                }
            }
        }
        if (result.isNotEmpty()) myToolWindow.updateData(result)
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