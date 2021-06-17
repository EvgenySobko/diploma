package com.evgenysobko.diploma.util

import com.intellij.diagnostic.PluginException
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.extensions.impl.ExtensionPointImpl
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.serviceContainer.ComponentManagerImpl
import java.util.function.BiConsumer
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

    fun getExtendedPoints(): MutableList<EPWithPluginName> {
        val epList = mutableSetOf<EPWithPluginName>()

        //epList.addAll(checkContainer(ApplicationManager.getApplication() as ComponentManagerImpl, taskExecutor))

        ProjectUtil.getOpenProjects().forEach {
            epList.addAll(checkContainer(it as ComponentManagerImpl, taskExecutor))
        }

        return epList.toMutableList()
    }

    private fun checkContainer(container: ComponentManagerImpl, taskExecutor: (task: () -> Unit) -> Unit): MutableSet<EPWithPluginName> {
        val resultSet = mutableSetOf<EPWithPluginName>()
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
    ): MutableList<EPWithPluginName> {
        var pluginName = ""
        val resultList = mutableListOf<EPWithPluginName>()
        extensionPoint.processImplementations(false, BiConsumer { supplier, pluginDescriptor ->
            var extensionClass: Class<out Any> by Delegates.notNull()
            taskExecutor {
                extensionClass = extensionPoint.extensionClass
            }

            pluginName = pluginDescriptor.name

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
            resultList.add(EPWithPluginName(pluginName, extensionPoint.extensionList.toMutableSet()))
        }

        return resultList
    }
}

data class EPWithPluginName(
    val name: String,
    val epList: MutableSet<Any>
)