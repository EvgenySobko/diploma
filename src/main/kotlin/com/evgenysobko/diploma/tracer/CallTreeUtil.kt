package com.evgenysobko.diploma.tracer

/** Encapsulates aggregate statistic for a single tracepoint. */
class TracepointStats(
    val tracepoint: Tracepoint,
    var callCount: Long = 0L,
    var wallTime: Long = 0L,
    var maxWallTime: Long = 0L
)

object CallTreeUtil {
    /**
     * Computes aggregate statistics for each tracepoint,
     * being careful not to double-count the time spent in recursive calls.
     */
    fun computeFlatTracepointStats(root: CallTree): List<TracepointStats> {
        val allStats = mutableMapOf<Tracepoint, TracepointStats>()
        val ancestors = mutableSetOf<Tracepoint>()

        fun dfs(node: CallTree) {
            val nonRecursive = node.tracepoint !in ancestors
            val stats = allStats.getOrPut(node.tracepoint) { TracepointStats(node.tracepoint) }
            stats.callCount += node.callCount
            if (nonRecursive) {
                stats.wallTime += node.wallTime
                stats.maxWallTime = maxOf(stats.maxWallTime, node.maxWallTime)
                ancestors.add(node.tracepoint)
            }
            for (child in node.children.values) {
                dfs(child)
            }
            if (nonRecursive) {
                ancestors.remove(node.tracepoint)
            }
        }

        dfs(root)
        assert(ancestors.isEmpty())

        allStats.remove(Tracepoint.ROOT)
        return allStats.values.toList()
    }

    /** Estimates total tracing overhead based on call counts in the given tree. */
    fun estimateTracingOverhead(root: CallTree): Long {
        // TODO: Can we provide a more accurate estimate (esp. for param tracing)?
        var tracingOverhead = 0L
        root.forEachNodeInSubtree { node ->
            tracingOverhead += when (node.tracepoint) {
                is MethodTracepointWithArgs -> 1024 * node.callCount // A complete guess.
                else -> 256 * node.callCount // See TracerIntegrationTest.testTracingOverhead.
            }
        }
        return tracingOverhead
    }
}
