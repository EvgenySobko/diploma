package com.evgenysobko.diploma.ui

import com.evgenysobko.diploma.toolwindow.EPWithPluginNameAndTracepointStats
import com.evgenysobko.diploma.util.formatNsInMs
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import javax.swing.table.AbstractTableModel

class TableModel : AbstractTableModel() {

    enum class Column(val displayName: String, type: Type) {
        PLUGIN_NAME("Plugin Name", EPWithPluginNameAndTracepointStats::class.java),
        TOTAL_WALL_TIME("Total Time", EPWithPluginNameAndTracepointStats::class.java),
        CLASSES("Classes and Methods", object : TypeToken<List<String>>() {}.type),
        CALLS("Calls Count", object : TypeToken<List<Long>>() {}.type),
        WALL_TIME("Wall Time (in ms)", object : TypeToken<List<Long>>() {}.type);

        companion object {
            val values = values()
            val count = values.size
            fun valueOf(col: Int): Column = values[col]
        }
    }

    var data = listOf<EPWithPluginNameAndTracepointStats>()

    override fun getRowCount(): Int = data.size

    override fun getColumnName(column: Int): String = Column.valueOf(column).displayName

    override fun getColumnClass(columnIndex: Int): Class<*> = Column.valueOf(columnIndex).javaClass

    override fun getColumnCount(): Int = Column.count

    override fun getValueAt(row: Int, col: Int): Any = data[row]

    fun clearData() {
        data = emptyList()
        fireTableDataChanged()
    }

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
        distinctList.forEach { it.stats.removeIf { it.wallTime.formatNsInMs() < 0.toString() } }
        data = distinctList.distinctBy { it.pluginName }
        data.sortedBy { it.pluginName }
        fireTableDataChanged()
    }
}