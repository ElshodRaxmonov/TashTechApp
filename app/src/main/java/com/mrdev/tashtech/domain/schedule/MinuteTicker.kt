package com.mrdev.tashtech.domain.schedule

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDateTime

/** Emits immediately, then on every wall-clock minute boundary. */
fun minuteTicker(clock: Clock): Flow<LocalDateTime> = flow {
    while (true) {
        val now = LocalDateTime.now(clock)
        emit(now)
        delay(60_000L - (now.second * 1_000L + now.nano / 1_000_000L))
    }
}