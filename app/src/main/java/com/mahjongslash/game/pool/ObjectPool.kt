package com.mahjongslash.game.pool

/**
 * Generic fixed-size object pool. Objects are pre-allocated at init time
 * and recycled — no allocation in the hot path.
 */
class ObjectPool<T>(
    capacity: Int,
    private val factory: () -> T,
    private val reset: (T) -> Unit = {},
) {
    private val pool = ArrayDeque<T>(capacity)
    private val capacity = capacity

    init {
        repeat(capacity) {
            pool.addLast(factory())
        }
    }

    fun obtain(): T? {
        return if (pool.isNotEmpty()) pool.removeFirst() else null
    }

    fun recycle(obj: T) {
        reset(obj)
        if (pool.size < capacity) {
            pool.addLast(obj)
        }
    }
}
