package com.evgenysobko.diploma.agent;

public interface TracerHook {
    void enter(int methodId, Object[] args);
    void leave();
}
