package com.evgenysobko.diploma.toolwindow

import com.evgenysobko.diploma.AgentLoader
import com.evgenysobko.diploma.tracer.TracepointStats
import com.evgenysobko.diploma.util.formatNsInMs
import com.evgenysobko.diploma.util.log
import com.intellij.ui.table.TableView
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.plaf.PanelUI

object ToolWindowContent {

    lateinit var content: JPanel
    lateinit var tableView: TableView<TracepointStats>

    fun init() {
        content = JPanel()
        content.setUI(
            object : PanelUI() {
                override fun installUI(c: JComponent?) {
                    AgentLoader.ensureJavaAgentLoaded
                    /*tableView = TableView<TracepointStats>()
                    val column = TableColumn()
                    tableView.addColumn(column)
                    c?.add(tableView)*/
                }
            }
        )
    }

    fun updateData(stats: List<TracepointStats>) {
        log("stats = $stats")
        stats.forEach {
            //tableView.items.add(it)
            //log("items = ${tableView.items}")
            log("execute time for ${it.tracepoint.displayName} is ${it.wallTime.formatNsInMs()}, calls count is ${it.callCount}")
        }
    }
}