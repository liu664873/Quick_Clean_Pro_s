package com.quickcleanpro.phonecleaner.use.core.ads

import java.util.concurrent.atomic.AtomicBoolean

fun once(block: () -> Unit): () -> Unit {
    val called = AtomicBoolean(false)
    return {
        if (called.compareAndSet(false, true)) {
            block()
        }
    }
}
