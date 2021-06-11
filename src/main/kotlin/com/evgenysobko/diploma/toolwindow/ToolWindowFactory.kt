package com.evgenysobko.diploma.toolwindow

import com.evgenysobko.diploma.tracer.*
import com.evgenysobko.diploma.util.EPFinder
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.attach
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ToolWindowFactory : ToolWindowFactory {

    private lateinit var myToolWindow: ToolWindowContent

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        myToolWindow = ToolWindowContent
        myToolWindow.init()
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(myToolWindow.content, "", false)
        toolWindow.contentManager.addContent(content)
        toolWindow.disposable.also {
            startTransformClasses(it)
            updateData(it)
        }
    }

    private fun startTransformClasses(disposable: Disposable) {
        val controller = TracerController(disposable)
        val extensionPoints = EPFinder().getExtendedPoints()
        val config = MethodConfig()
        val requests = mutableListOf<TraceRequest>()
        extensionPoints.forEach { clazz ->
            val request = TracerConfigUtil.appendTraceRequest(MethodFqName(clazz, "*", "*"), config)
            requests.add(request)
        }
        val affectedClasses = TracerConfigUtil.getAffectedClasses(requests)
        controller.retransformClasses(affectedClasses)
        CallTreeManager.clearCallTrees()
    }

    private fun updateData(disposable: Disposable) {
        val refreshFuture = Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay(::updateCallTree, 0, 100, TimeUnit.MILLISECONDS)
        disposable.attach { refreshFuture.cancel(false) }
    }

    private fun updateCallTree() {
        val treeSnapshot = CallTreeManager.getCallTreeSnapshotAllThreadsMerged()
        val stats = CallTreeUtil.computeFlatTracepointStats(treeSnapshot)
        if (stats.isNotEmpty()) myToolWindow.updateData(stats)
    }
}