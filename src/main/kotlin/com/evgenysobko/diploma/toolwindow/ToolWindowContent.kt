package com.evgenysobko.diploma.toolwindow

import com.evgenysobko.diploma.AgentLoader
import com.evgenysobko.diploma.ui.Table
import com.evgenysobko.diploma.ui.TableModel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

object ToolWindowContent {

    lateinit var content: JPanel
    private val tableModel: TableModel = TableModel()

    fun init(toolWindow: ToolWindow) {
        AgentLoader.ensureJavaAgentLoaded
        val table = Table(tableModel)
        var jbScrollPane: JBScrollPane? = null
        content = JPanel(BorderLayout())
        Dimension(toolWindow.component.width, toolWindow.component.height).let {
            content.preferredSize = it
            jbScrollPane = JBScrollPane(table)
            jbScrollPane!!.border = null
            jbScrollPane!!.autoscrolls = false
            content.autoscrolls = false
            table.border = null
        }
        content.add(jbScrollPane)
    }

    fun updateData(resultList: Set<EPWithPluginNameAndTracepointStats>) = tableModel.updateData(resultList)
}