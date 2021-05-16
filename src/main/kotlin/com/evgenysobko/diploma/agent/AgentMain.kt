package com.evgenysobko.diploma.agent

import java.lang.instrument.Instrumentation

class AgentMain {

    companion object {

        lateinit var savedInstrumentationInstance: Instrumentation

        fun premain(agentArgs: String, instrumentation: Instrumentation) = agentMain(agentArgs, instrumentation)

        fun agentMain(agentArgs: String, instrumentation: Instrumentation) {
            savedInstrumentationInstance = instrumentation
        }
    }
}