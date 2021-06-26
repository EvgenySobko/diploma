package com.evgenysobko.diploma.ui

import com.evgenysobko.diploma.toolwindow.EPWithPluginNameAndTracepointStats
import com.evgenysobko.diploma.tracer.TracepointStats
import com.evgenysobko.diploma.ui.TableModel.Column
import com.evgenysobko.diploma.util.formatNsInMs
import com.evgenysobko.diploma.util.fromNsToMs
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.ui.table.JBTable
import java.awt.Component
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter


class Table(tableModel: TableModel) : JBTable(tableModel) {

    init {
        createTable(this, tableModel)
    }

    class MyTableRowSorter(model: TableModel) : TableRowSorter<TableModel>(model) {
        init {
            sortsOnUpdates = true
            toggleSortOrder(Column.TOTAL_WALL_TIME.ordinal)
        }

        override fun toggleSortOrder(col: Int) {
            val alreadySorted = sortKeys.any {
                it.column == col && it.sortOrder != SortOrder.UNSORTED
            }
            if (alreadySorted) return
            val order = when (Column.valueOf(col)) {
                Column.TOTAL_WALL_TIME -> SortOrder.DESCENDING
                Column.PLUGIN_NAME, Column.CALLS, Column.WALL_TIME, Column.CLASSES -> SortOrder.ASCENDING
            }
            sortKeys = listOf(SortKey(col, order))
        }
    }

    companion object {

        fun createTable(table: JBTable, tableModel: TableModel): JBTable {
            with(table) {
                font = EditorUtil.getEditorFont()
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                setShowGrid(true)
                rowSorter = MyTableRowSorter(tableModel)
                autoscrolls = false
            }

            val columns = table.columnModel.columns.toList()

            for (i in 0..1) {
                SwingUtilities.invokeLater {
                    with(columns[i]) {
                        minWidth = 200
                        cellRenderer = object : DefaultTableCellRenderer() {

                            init {
                                horizontalAlignment = SwingConstants.LEFT
                                verticalAlignment = SwingConstants.TOP
                            }

                            override fun setValue(value: Any?) {
                                if (value == null) return super.setValue(value)
                                val ep = value as EPWithPluginNameAndTracepointStats
                                when (i) {
                                    0 -> super.setValue(ep.pluginName)
                                    1 -> {
                                        var totalTime = 0L
                                        ep.stats.map { totalTime += it.wallTime }
                                        super.setValue(totalTime.formatNsInMs())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SwingUtilities.invokeLater {
                for (i in 2 until columns.size) {
                    with(columns[i]) {
                        minWidth = 200
                        cellRenderer = object : DefaultTableCellRenderer() {

                            private var minHeight = -1
                            private var currHeight = -1

                            override fun getTableCellRendererComponent(
                                table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
                            ): Component {
                                val setValue = value as EPWithPluginNameAndTracepointStats
                                val passed = setValue.stats
                                if (minHeight == -1) {
                                    minHeight = table.rowHeight
                                }
                                if (currHeight != (passed.size + 1) * minHeight) {
                                    currHeight = (passed.size + 1) * minHeight
                                    table.setRowHeight(row, currHeight)
                                }

                                val innerTable = JBTable(object : AbstractTableModel() {

                                    override fun getColumnCount(): Int = 1

                                    override fun getRowCount(): Int = passed.size

                                    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any = passed.elementAt(rowIndex)

                                    override fun isCellEditable(row: Int, col: Int): Boolean = true
                                })

                                innerTable.columnModel.columns.toList().forEach {
                                    it.cellRenderer = object : DefaultTableCellRenderer() {
                                        override fun setValue(value: Any?) {
                                            if (value == null) return super.setValue(value)
                                            val newValue = value as TracepointStats
                                            var result: Any? = null
                                            when (i) {
                                                2 -> result = newValue.tracepoint.displayName
                                                3 -> result = newValue.callCount
                                                4 -> result = newValue.wallTime.fromNsToMs()
                                            }
                                            super.setValue(result)
                                        }
                                    }
                                }
                                return innerTable
                            }
                        }
                    }
                }
            }

            return table
        }
    }
}
