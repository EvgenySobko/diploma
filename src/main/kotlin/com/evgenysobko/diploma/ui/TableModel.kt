package com.evgenysobko.diploma.ui

import com.evgenysobko.diploma.toolwindow.EPWithPluginNameAndTracepointStats
import javax.swing.table.AbstractTableModel

class TableModel : AbstractTableModel() {

    enum class Column(val displayName: String, val type: Class<*>) {
        PLUGIN_NAME("Plugin Name", EPWithPluginNameAndTracepointStats::class.java),
        CLASSES("Classes", EPWithPluginNameAndTracepointStats::class.java),
        CALLS("Calls", EPWithPluginNameAndTracepointStats::class.java),
        WALL_TIME("Wall Time", EPWithPluginNameAndTracepointStats::class.java);

        companion object {
            val values = values()
            val count = values.size
            fun valueOf(col: Int): Column = values[col]
        }
    }

    var data = listOf<EPWithPluginNameAndTracepointStats>()

    override fun getRowCount(): Int = data.size

    override fun getColumnName(column: Int): String = Column.valueOf(column).displayName

    override fun getColumnClass(columnIndex: Int): Class<*> = Column.valueOf(columnIndex).type

    override fun getColumnCount(): Int = Column.count

    override fun getValueAt(row: Int, col: Int): Any = data[row]

    fun updateData(list: Set<EPWithPluginNameAndTracepointStats>) {
        val distinctList = mutableSetOf<EPWithPluginNameAndTracepointStats>()
        for (i in 0 until list.size - 1) {
            for (j in 1 until list.size) {
                if (list.elementAt(i).pluginName == list.elementAt(j).pluginName) {
                    list.elementAt(i).mergeWithOther(list.elementAt(j))
                    distinctList.add(list.elementAt(i))
                }
            }
        }
        data = distinctList.distinctBy { it.pluginName }
        fireTableDataChanged()
    }
}