package com.evgenysobko.diploma.agent;

import java.lang.instrument.Instrumentation;

public class AgentMain {

    public static Instrumentation savedInstrumentationInstance;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        agentmain(agentArgs, instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        savedInstrumentationInstance = instrumentation;
    }
}
