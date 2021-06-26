package com.evgenysobko.diploma.toolwindow

import com.evgenysobko.diploma.ui.Table
import com.evgenysobko.diploma.ui.TableModel
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel

object ToolWindowContent: DumbAware {

    lateinit var content: JPanel
    private val tableModel: TableModel = TableModel()

    fun init(toolWindow: ToolWindow) {
        val table = Table(tableModel)
        var jbScrollPane: JBScrollPane? = null
        content = JPanel(BorderLayout())
        val jButton = JButton("Clear")
        jButton.addActionListener { tableModel.clearData() }
        Dimension(toolWindow.component.width, toolWindow.component.height).let {
            content.preferredSize = it
            jbScrollPane = JBScrollPane(table)
            jbScrollPane!!.border = null
            jbScrollPane!!.autoscrolls = false
            content.autoscrolls = false
            table.border = null
        }
        content.apply {
            add(jbScrollPane)
            jButton.maximumSize = Dimension(100, 100)
            add(jButton, BorderLayout.WEST)
        }
    }

    fun updateData(resultList: Set<EPWithPluginNameAndTracepointStats>) = tableModel.updateData(resultList)
}