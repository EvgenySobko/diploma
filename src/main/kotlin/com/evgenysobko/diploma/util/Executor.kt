package com.evgenysobko.diploma.util

import com.intellij.util.concurrency.AppExecutorUtil

class Executor(name: String, maxThreads: Int) {
    private val backend = AppExecutorUtil.createBoundedScheduledExecutorService(name, maxThreads)

    fun execute(task: () -> Unit) {
        backend.execute(task)
    }

    fun shutdownNow(): List<Runnable> {
        return backend.shutdownNow()
    }
}
