package com.evgenysobko.diploma.agent

object TracerTrampoline {
    private var hook: TracerHook? = null

    fun installHook(newHook: TracerHook?) {
        hook = newHook
    }

    fun enter(methodId: Int, args: Array<Any?>?) = hook!!.enter(methodId, args)

    fun leave() = hook!!.leave()
}