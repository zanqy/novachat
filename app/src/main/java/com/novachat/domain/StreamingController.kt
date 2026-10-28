package com.novachat.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * Runs exactly one generation at a time within the owner's [scope].
 * Starting new work cancels the previous job; [stop] cancels immediately.
 */
class StreamingController(private val scope: CoroutineScope) {
    private var job: Job? = null

    val running: Boolean
        get() = job?.isActive == true

    fun start(work: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch { work() }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun join() {
        job?.join()
    }

    suspend fun stopAndJoin() {
        job?.cancelAndJoin()
        job = null
    }
}
