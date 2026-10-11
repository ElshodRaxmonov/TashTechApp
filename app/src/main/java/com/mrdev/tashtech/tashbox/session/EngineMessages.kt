package com.mrdev.tashtech.tashbox.session

import com.mrdev.tashtech.tashbox.engine.EngineEvent
import com.mrdev.tashtech.tashbox.network.GameFinished
import com.mrdev.tashtech.tashbox.network.NetworkMessage
import com.mrdev.tashtech.tashbox.network.PlayerAnswered
import com.mrdev.tashtech.tashbox.network.RoundResult
import com.mrdev.tashtech.tashbox.network.RoundStart
import com.mrdev.tashtech.tashbox.network.StartGame

/**
 * Turns engine events into the SAME messages the host sends over the network. Solo, host and
 * clients therefore all drive their UI through one code path (SessionUiModel).
 */
internal object EngineMessages {
    fun toMessage(event: EngineEvent, totalRounds: Int): NetworkMessage = when (event) {
        is EngineEvent.CountdownStarted -> StartGame(event.millis, totalRounds)
        is EngineEvent.RoundStarted -> RoundStart(
            round = event.round,
            totalRounds = totalRounds,
            puzzle = event.puzzle.redacted(),
            flashMillis = event.puzzle.flashMillis,
            answerMillis = event.puzzle.answerMillis
        )
        is EngineEvent.AnswerAccepted -> PlayerAnswered(event.round, event.playerId)
        is EngineEvent.RoundEnded -> RoundResult(event.result)
        is EngineEvent.MatchFinished -> GameFinished(event.result.copy(finishedAtEpochMillis = System.currentTimeMillis()))
    }
}
