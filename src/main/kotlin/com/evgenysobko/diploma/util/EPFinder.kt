package com.evgenysobko.diploma.util

import com.intellij.diagnostic.PluginException
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.extensions.impl.ExtensionPointImpl
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.serviceContainer.ComponentManagerImpl
import kotlin.properties.Delegates

object EPFinder {

    private val taskExecutor: (task: () -> Unit) -> Unit = { task ->
        try {
            task()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            log(e)
        }
    }

    fun getExtendedPoints(): MutableMap<String, MutableList<Any>> {
        val epList = mutableMapOf<String, MutableList<Any>>()

        val firstMap = checkContainer(ApplicationManager.getApplication() as ComponentManagerImpl, taskExecutor)
        firstMap.keys.forEach { key ->
            if (epList.containsKey(key)) {
                epList[key]!!.addAll(firstMap[key]!!)
            } else {
                if (epList[key].isNullOrEmpty()) epList[key] = firstMap[key]!!
                else epList[key]!!.add(firstMap[key]!!)
            }
        }

        ProjectUtil.getOpenProjects().forEach {
            val newMap = checkContainer(it as ComponentManagerImpl, taskExecutor)
            newMap.keys.forEach { key ->
                if (epList.containsKey(key)) {
                    epList[key]!!.addAll(newMap[key]!!)
                } else {
                    if (epList[key].isNullOrEmpty()) epList[key] = newMap[key]!!
                    else epList[key]!!.add(newMap[key]!!)
                }
            }
        }
        log(epList["BadPlugin"])

        return epList
    }

    private fun checkContainer(container: ComponentManagerImpl, taskExecutor: (task: () -> Unit) -> Unit): MutableMap<String, MutableList<Any>> {
        val result = mutableMapOf<String, MutableList<Any>>()
        container.extensionArea.processExtensionPoints {
            result.putAll(checkExtensionPoint(it, taskExecutor))
        }
        return result
    }

    private fun checkExtensionPoint(
        extensionPoint: ExtensionPointImpl<*>,
        taskExecutor: (task: () -> Unit) -> Unit = this.taskExecutor,
    ): MutableMap<String, MutableList<Any>> {
        var pluginName = ""
        val resultList = mutableMapOf<String, MutableList<Any>>()
        extensionPoint.processImplementations(false) { supplier, pluginDescriptor ->
            var extensionClass: Class<out Any> by Delegates.notNull()

            pluginName = pluginDescriptor.name

            try {
                extensionClass = extensionPoint.extensionClass
            } catch (notFound: Exception) {
                log("Extension class for ep = ${extensionPoint.name} not found")
            }

            taskExecutor {
                try {
                    val extension = supplier.get() ?: return@taskExecutor
                    if (!extensionClass.isInstance(extension)) {
                        throw PluginException(
                            "Extension ${extension.javaClass.name} does not implement $extensionClass",
                            pluginDescriptor.pluginId
                        )
                    } else {
                        if (!resultList.keys.contains(pluginName)) {
                            resultList.put(pluginName, mutableListOf(extension))
                        } else {
                            resultList[pluginName]!!.add(extension)
                        }

                    }
                } catch (ignore: ExtensionNotApplicableException) {}
            }
        }

        return resultList
    }
}
