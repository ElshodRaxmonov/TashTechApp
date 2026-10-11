package com.mrdev.tashtech.tashbox.timer

/** Millisecond clock that never jumps (unlike wall-clock time). Injected so tests can control time. */
fun interface MonotonicClock {
    fun nowMillis(): Long
}

object SystemMonotonicClock : MonotonicClock {
    override fun nowMillis(): Long = System.nanoTime() / 1_000_000L
}
