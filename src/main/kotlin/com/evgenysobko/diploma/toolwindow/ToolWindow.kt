package com.evgenysobko.diploma.toolwindow

import com.evgenysobko.diploma.AgentLoader
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.wm.ToolWindow
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.plaf.PanelUI

class ToolWindow(
    private val toolWindow: ToolWindow
) {

    lateinit var content: JPanel

    fun init() {
        content = JPanel()
        content.setUI(
            object : PanelUI() {
                override fun installUI(c: JComponent?) {
                    val button = JButton("Trace")
                    button.addActionListener {
                        if (AgentLoader.ensureTracerHooksInstalled) {
                            Logger.getInstance(this::class.java).info("hooks installed")
                        }
                        if (AgentLoader.ensureJavaAgentLoaded) {
                            Logger.getInstance(this::class.java).info("agent loaded")
                        }
                    }
                    c?.add(button)
                }
            }
        )
    }
}