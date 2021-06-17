package com.evgenysobko.diploma.ui

import com.evgenysobko.diploma.toolwindow.EPWithPluginNameAndTracepointStats
import com.evgenysobko.diploma.ui.TableModel.Column
import com.evgenysobko.diploma.util.fromNsToMs
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import javax.swing.ListSelectionModel
import javax.swing.SortOrder
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

class Table(private val tableModel: TableModel) : JBTable(tableModel) {

    init {
        createTable(this, tableModel)
    }

    class MyTableRowSorter(model: javax.swing.table.TableModel) : TableRowSorter<javax.swing.table.TableModel>(model) {
        init {
            sortsOnUpdates = true
            toggleSortOrder(Column.WALL_TIME.ordinal)
        }

        override fun toggleSortOrder(col: Int) {
            val alreadySorted = sortKeys.any {
                it.column == col && it.sortOrder != SortOrder.UNSORTED
            }
            if (alreadySorted) return
            val order = when (Column.valueOf(col)) {
                Column.PLUGIN_NAME -> SortOrder.ASCENDING
                Column.CALLS, Column.WALL_TIME, Column.CLASSES -> SortOrder.DESCENDING
            }
            sortKeys = listOf(SortKey(col, order))
        }
    }

    companion object {

        fun createTable(table: JBTable, tableModel: TableModel): JBTable {
            table.font = EditorUtil.getEditorFont()
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            table.setShowGrid(false)
            table.rowSorter = MyTableRowSorter(tableModel)
            table.rowHeight = JBUI.scale(30)

            val columns = table.columnModel.columns.toList()

            var pluginName = ""

            val pluginNameColumn = columns[0]
            SwingUtilities.invokeLater {
                with(pluginNameColumn) {
                    minWidth = 200
                    preferredWidth = 300
                    cellRenderer = object : DefaultTableCellRenderer() {

                        init {
                            horizontalAlignment = SwingConstants.CENTER
                        }

                        override fun setValue(value: Any?) {
                            if (value == null) return super.setValue(value)
                            val ep = value as EPWithPluginNameAndTracepointStats
                            pluginName = ep.pluginName
                            super.setValue(ep.pluginName)
                        }
                    }
                }
            }

            SwingUtilities.invokeLater {
                for (i in 1 until columns.size) {
                    with(columns[i]) {
                        minWidth = 200
                        preferredWidth = 200
                        cellRenderer = object : DefaultTableCellRenderer() {

                            override fun setValue(value: Any?) {
                                if (value == null) return super.setValue(value)
                                var newValue: List<Any>? = null
                                if (pluginName.isNotEmpty()) {
                                    val ep = value as EPWithPluginNameAndTracepointStats
                                    if (ep.pluginName == pluginName) {
                                        when (i) {
                                            1 -> newValue = ep.stats.map { it.tracepoint.displayName }
                                            2 -> newValue = ep.stats.map { it.callCount }
                                            3 -> newValue = ep.stats.map { it.wallTime.fromNsToMs() }
                                        }
                                    }
                                }
                                super.setValue(newValue)
                            }
                        }
                    }
                }
            }

            return table
        }
    }
}