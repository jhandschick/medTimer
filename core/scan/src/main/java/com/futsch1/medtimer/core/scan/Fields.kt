package com.futsch1.medtimer.core.scan

/** Repeated keys are tolerated only with identical values; a conflict means the code cannot be trusted. */
internal fun List<Pair<String, String>>.toConsistentMap(): Map<String, String>? {
    val map = mutableMapOf<String, String>()
    for ((key, value) in this) {
        val previous = map.putIfAbsent(key, value)
        if (previous != null && previous != value) return null
    }
    return map
}
