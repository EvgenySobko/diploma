package com.evgenysobko.diploma.agent;

public final class TracerTrampoline {
    private static TracerHook hook = null;

    public static void installHook(TracerHook newHook) {
        hook = newHook;
    }

    public static void enter(int methodId, Object[] args) {
        hook.enter(methodId, args);
    }

    public static void leave() {
        hook.leave();
    }
}
