package com.evgenysobko.diploma.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.extensions.impl.ExtensionComponentAdapter
import com.intellij.openapi.extensions.impl.ExtensionPointImpl
import com.intellij.openapi.project.ProjectManager

object EPFinder {

    fun getExtendedPoints(): MutableMap<String, MutableList<String>> {
        val epList = mutableMapOf<String, MutableList<String>>()

        val projectManager = ProjectManager.getInstance()
        projectManager.openProjects.forEach { find(it.extensionArea, epList) }
        find(ApplicationManager.getApplication().extensionArea, epList)

        return epList
    }

    private fun find(area: ExtensionsArea, epList: MutableMap<String, MutableList<String>>) {
        area.extensionPoints.forEach {
            (it as ExtensionPointImpl<*>)
            val method = ExtensionPointImpl::class.java.getDeclaredMethod("getSortedAdapters")
            method.isAccessible = true
            val result = method.invoke(it) as? List<ExtensionComponentAdapter>
            result?.forEach {
                if (it.assignableToClassName == "com.intellij.openapi.options.ConfigurableEP") {
                    // todo
                } else {
                    epList.computeIfAbsent(it.pluginDescriptor.name) {
                        mutableListOf()
                    }.add(it.assignableToClassName)
                }
            }
        }
    }
}
