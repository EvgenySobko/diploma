package com.evgenysobko.diploma.tracer

class CallTreeBuilder(clock: Clock = SystemClock) {
    private val clock = ClockWithOverheadAdjustment(clock)
    private var root = Tree(Tracepoint.ROOT, parent = null)
    private var currentNode = root

    interface Clock {
        fun sample(): Long
    }

    private object SystemClock : Clock {
        override fun sample(): Long = System.nanoTime()
    }

    private class ClockWithOverheadAdjustment(val delegate: Clock) : Clock {
        var overhead: Long = 0
        override fun sample(): Long = delegate.sample() - overhead
    }

    class Tree(
        override val tracepoint: Tracepoint,
        val parent: Tree?
    ): CallTree {
        override var callCount: Long = 0L
        override var wallTime: Long = 0L
        override var maxWallTime: Long = 0L
        override val children: MutableMap<Tracepoint, Tree> = LinkedHashMap()

        var startWallTime: Long = 0
        var continueWallTime: Long = 0
        var wallTimeMeasured: Boolean = false

        init {
            require(parent != null || tracepoint == Tracepoint.ROOT) {
                "Only the root node can have a null parent"
            }
        }
    }

    fun push(tracepoint: Tracepoint) {
        val parent = currentNode
        val child = parent.children.getOrPut(tracepoint) { Tree(tracepoint, parent) }

        child.callCount++

        if (tracepoint.measureWallTime) {
            val now = clock.sample()
            child.startWallTime = now
            child.continueWallTime = now
            child.wallTimeMeasured = true
        } else {
            child.wallTimeMeasured = false
        }

        

        currentNode = child
    }

    fun pop() {
        val child = currentNode
        val parent = child.parent

        check(parent != null) { "The root node should never be popped" }

        if (child.wallTimeMeasured) {
            val now = clock.sample()
            child.wallTime += now - child.continueWallTime
            child.maxWallTime = maxOf(child.maxWallTime, now - child.startWallTime)
        }

        currentNode = parent
    }

    fun subtractOverhead(overhead: Long) {
        clock.overhead += overhead
    }

    fun borrowUpToDateTree(): CallTree {
        val now = clock.sample()
        val stack = generateSequence(currentNode, Tree::parent)
        for (node in stack) {
            if (node.tracepoint != Tracepoint.ROOT && node.wallTimeMeasured) {
                node.wallTime += now - node.continueWallTime
                node.maxWallTime = maxOf(node.maxWallTime, now - node.startWallTime)
                node.continueWallTime = now
            }
        }

        return root
    }

    fun clear() {
        val stack = generateSequence(currentNode, Tree::parent)
        val pathFromRoot = stack.toList().asReversed()
        val tracepoints = pathFromRoot.drop(1).map(Tree::tracepoint)

        root = Tree(Tracepoint.ROOT, parent = null)
        currentNode = root
        tracepoints.forEach(::push)
    }
}
