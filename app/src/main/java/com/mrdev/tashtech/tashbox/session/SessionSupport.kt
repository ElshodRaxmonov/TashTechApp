package com.mrdev.tashtech.tashbox.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/** Single-threaded dispatcher: the engine and room are not thread-safe, so only this thread touches them. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun newEngineDispatcher(): CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

/** A child scope whose cancellation never touches the parent (e.g. the ViewModel's scope). */
internal fun childScope(parent: CoroutineScope): Pair<Job, CoroutineScope> {
    val job = SupervisorJob(parent.coroutineContext[Job])
    return job to CoroutineScope(parent.coroutineContext + job)
}
