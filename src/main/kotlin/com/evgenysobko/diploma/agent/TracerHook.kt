package com.evgenysobko.diploma.agent

interface TracerHook {
    fun enter(methodId: Int, args: Array<*>?)
    fun leave()
}