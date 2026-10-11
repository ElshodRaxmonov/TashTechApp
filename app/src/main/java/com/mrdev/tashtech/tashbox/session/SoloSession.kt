package com.mrdev.tashtech.tashbox.session

import com.mrdev.tashtech.tashbox.engine.EngineEvent
import com.mrdev.tashtech.tashbox.engine.EnginePhase
import com.mrdev.tashtech.tashbox.engine.EnginePlayer
import com.mrdev.tashtech.tashbox.engine.GameEngine
import com.mrdev.tashtech.tashbox.leaderboard.GameMode
import com.mrdev.tashtech.tashbox.network.LevelChange
import com.mrdev.tashtech.tashbox.network.PlayerList
import com.mrdev.tashtech.tashbox.engine.EngineConfig
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.room.LobbyPlayer
import com.mrdev.tashtech.tashbox.timer.MonotonicClock
import com.mrdev.tashtech.tashbox.timer.SystemMonotonicClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** 5 rounds on one phone. Same engine as multiplayer, no network at all. */
class SoloSession(
    parentScope: CoroutineScope,
    playerName: String = "You",
    seed: Long = Random.nextLong(),
    level: Difficulty = Difficulty.MEDIUM,
    private val clock: MonotonicClock = SystemMonotonicClock
) : GameSession {

    private val jobAndScope = childScope(parentScope)
    private val job = jobAndScope.first
    private val scope = jobAndScope.second
    private val dispatcher = newEngineDispatcher()

    private val me = EnginePlayer("you", playerName.trim().ifBlank { "You" })
    private val engine = GameEngine(listOf(me), seed, GameMode.SOLO, EngineConfig(level = level, waitForContinue = true))
    private val model = SessionUiModel(SessionRole.SOLO, clock, me.id)

    override val state: StateFlow<GameUiState> = model.state

    init {
        model.onMessage(LevelChange(level))
        model.onMessage(PlayerList(listOf(LobbyPlayer(me.id, me.name, ready = true, connected = true))))
        scope.launch(dispatcher) {
            dispatch(engine.start(clock.nowMillis()))
            while (isActive && engine.phase != EnginePhase.FINISHED) {
                dispatch(engine.tick(clock.nowMillis()))
                model.tick()
                delay(TICK_MS)
            }
        }
    }

    private fun dispatch(events: List<EngineEvent>) {
        events.forEach { model.onMessage(EngineMessages.toMessage(it, engine.totalRounds)) }
    }

    override fun submitAnswer(optionIndex: Int) {
        if (!model.selectLocal(optionIndex)) return
        val round = model.state.value.round
        scope.launch(dispatcher) {
            dispatch(engine.submitAnswer(me.id, round, optionIndex, clock.nowMillis()).events)
        }
    }

    override fun continueToNext() {
        scope.launch(dispatcher) { dispatch(engine.continueNext(clock.nowMillis())) }
    }

    override fun close() { job.cancel() }
}
