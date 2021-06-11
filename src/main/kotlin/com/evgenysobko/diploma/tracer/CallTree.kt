package com.evgenysobko.diploma.tracer

interface CallTree {
    val tracepoint: Tracepoint
    val callCount: Long
    val wallTime: Long
    val maxWallTime: Long
    val children: Map<Tracepoint, CallTree>

    fun forEachNodeInSubtree(action: (CallTree) -> Unit) {
        action(this)
        for (child in children.values) {
            child.forEachNodeInSubtree(action)
        }
    }

    fun allNodesInSubtree(): Sequence<CallTree> {
        val nodes = mutableListOf<CallTree>()
        forEachNodeInSubtree { nodes.add(it) }
        return nodes.asSequence()
    }

    fun copy(): CallTree {
        val copy = MutableCallTree(Tracepoint.ROOT)
        copy.accumulate(this)
        return copy
    }
}

class MutableCallTree(
    override val tracepoint: Tracepoint
): CallTree {
    override var callCount: Long = 0L
    override var wallTime: Long = 0L
    override var maxWallTime: Long = 0L
    override val children: MutableMap<Tracepoint, MutableCallTree> = LinkedHashMap()

    /** Accumulates the data from another call tree into this one. */
    fun accumulate(other: CallTree) {
        require(other.tracepoint == tracepoint) {
            "Doesn't make sense to sum call tree nodes representing different tracepoints"
        }

        callCount += other.callCount
        wallTime += other.wallTime
        maxWallTime = maxOf(maxWallTime, other.maxWallTime)

        for ((childTracepoint, otherChild) in other.children) {
            val child = children.getOrPut(childTracepoint) { MutableCallTree(childTracepoint) }
            child.accumulate(otherChild)
        }
    }
}
