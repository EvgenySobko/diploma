package com.evgenysobko.diploma.util

import com.intellij.diagnostic.PluginException
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.extensions.impl.ExtensionPointImpl
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.serviceContainer.ComponentManagerImpl
import java.util.function.BiConsumer
import kotlin.properties.Delegates

class EPFinder {

    private val taskExecutor: (task: () -> Unit) -> Unit = { task ->
        try {
            task()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            log(e)
        }
    }

    fun getExtendedPoints(): List<String> {
        val epList = mutableListOf<Any>()

        ProjectUtil.getOpenProjects().firstOrNull()?.let {
            epList.addAll(checkContainer(it as ComponentManagerImpl, taskExecutor))
        }

        return epList.map { it.javaClass.name }
    }

    private fun checkContainer(container: ComponentManagerImpl, taskExecutor: (task: () -> Unit) -> Unit): Set<Any> {
        val resultSet = mutableSetOf<Any>()
        container.extensionArea.processExtensionPoints { extensionPoint ->
            if (extensionPoint.name == "com.intellij.favoritesListProvider" || extensionPoint.name == "com.intellij.favoritesListProvider") {
                return@processExtensionPoints
            }

            resultSet.addAll(checkExtensionPoint(extensionPoint, taskExecutor))
        }
        return resultSet
    }

    private fun checkExtensionPoint(
        extensionPoint: ExtensionPointImpl<*>,
        taskExecutor: (task: () -> Unit) -> Unit = this.taskExecutor
    ): List<Any> {
        val resultList = mutableListOf<Any>()
        extensionPoint.processImplementations(false, BiConsumer { supplier, pluginDescriptor ->
            var extensionClass: Class<out Any> by Delegates.notNull()
            taskExecutor {
                extensionClass = extensionPoint.extensionClass
            }

            taskExecutor {
                try {
                    val extension = supplier.get() ?: return@taskExecutor
                    if (!extensionClass.isInstance(extension)) {
                        throw PluginException(
                            "Extension ${extension.javaClass.name} does not implement $extensionClass",
                            pluginDescriptor.pluginId
                        )
                    }
                } catch (ignore: ExtensionNotApplicableException) {
                }
            }
        })

        taskExecutor {
            resultList.addAll(extensionPoint.extensionList)
        }

        return resultList
    }
}