package com.evgenysobko.diploma.tracer

import com.intellij.util.ui.EDT
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureNanoTime

object CallTreeManager {

    private class ThreadState(val isEdt: Boolean) {
        var busy = false
        val lock = ReentrantLock()
        var callTreeBuilder = CallTreeBuilder()
    }

    private val allThreadState = CopyOnWriteArrayList<ThreadState>()

    private val threadState: ThreadLocal<ThreadState> =
        ThreadLocal.withInitial {
            val state = ThreadState(EDT.isCurrentThreadEdt())
            allThreadState.add(state)
            state
        }

    fun enter(tracepoint: Tracepoint) {
        val state = threadState.get()
        doPreventingRecursion(state) {
            doWithLockAndAdjustOverhead(state) {
                state.callTreeBuilder.push(tracepoint)
            }
        }
    }

    fun leave() {
        val state = threadState.get()
        doPreventingRecursion(state) {
            doWithLockAndAdjustOverhead(state) {
                state.callTreeBuilder.pop()
            }
        }
    }

    fun getCallTreeSnapshotAllThreadsMerged(): CallTree {
        val mergedTree = MutableCallTree(Tracepoint.ROOT)
        for (threadState in allThreadState) {
            threadState.lock.withLock {
                val localTree = threadState.callTreeBuilder.borrowUpToDateTree()
                mergedTree.accumulate(localTree)
            }
        }
        return mergedTree
    }

    fun clearCallTrees() {
        for (threadState in allThreadState) {
            threadState.lock.withLock {
                threadState.callTreeBuilder.clear()
            }
        }
    }

    private inline fun doWithLockAndAdjustOverhead(state: ThreadState, action: () -> Unit) {
        if (!state.lock.tryLock()) {
            val overhead = measureNanoTime { state.lock.lock() }
            state.callTreeBuilder.subtractOverhead(overhead)
        }
        try {
            action()
        } finally {
            state.lock.unlock()
        }
    }

    private inline fun doPreventingRecursion(state: ThreadState, action: () -> Unit) {
        if (!state.busy) {
            state.busy = true
            try {
                action()
            }
            finally {
                state.busy = false
            }
        }
    }
}
