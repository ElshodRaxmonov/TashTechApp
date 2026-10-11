package com.mrdev.tashtech.tashbox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mrdev.tashtech.tashbox.leaderboard.GameResult
import com.mrdev.tashtech.tashbox.leaderboard.TashBoxStats
import com.mrdev.tashtech.tashbox.network.LocalNetwork
import com.mrdev.tashtech.tashbox.network.TcpHostTransport
import com.mrdev.tashtech.tashbox.puzzle.Difficulty
import com.mrdev.tashtech.tashbox.room.QrPayload
import com.mrdev.tashtech.tashbox.session.ClientSession
import com.mrdev.tashtech.tashbox.session.GameSession
import com.mrdev.tashtech.tashbox.session.GameUiState
import com.mrdev.tashtech.tashbox.session.HostSession
import com.mrdev.tashtech.tashbox.session.SessionRole
import com.mrdev.tashtech.tashbox.session.SoloSession
import com.mrdev.tashtech.tashbox.session.UiPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TashBoxViewModel(app: Application) : AndroidViewModel(app) {

    enum class Route { TUTORIAL, HOME, FRIENDS, SCAN, SESSION }

    private val stats = TashBoxStats(app)

    /** First launch opens the tutorial; afterwards it opens Home. */
    private val _route = MutableStateFlow(if (stats.tutorialDone) Route.HOME else Route.TUTORIAL)
    val route: StateFlow<Route> = _route.asStateFlow()

    private val _game = MutableStateFlow<GameUiState?>(null)
    val game: StateFlow<GameUiState?> = _game.asStateFlow()

    private val _summary = MutableStateFlow<TashBoxStats.Summary?>(null)
    val summary: StateFlow<TashBoxStats.Summary?> = _summary.asStateFlow()

    private val _playerName = MutableStateFlow(stats.playerName)
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _level = MutableStateFlow(stats.lastLevel)
    val level: StateFlow<Difficulty> = _level.asStateFlow()

    private val _bestByLevel = MutableStateFlow(stats.bestSoloAll())
    val bestByLevel: StateFlow<Map<Difficulty, Int>> = _bestByLevel.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val _finished = MutableSharedFlow<Pair<GameResult, String>>(extraBufferCapacity = 4)
    /** Emits (result, myPlayerId) once per finished match: connect this to your leaderboard. */
    val finishedEvents: SharedFlow<Pair<GameResult, String>> = _finished.asSharedFlow()

    private var session: GameSession? = null
    private var collectJob: Job? = null
    private var recorded = false

    // ---- navigation -----------------------------------------------------------------------

    fun openFriends() { _route.value = Route.FRIENDS }
    fun openScan() { _scanError.value = null; _route.value = Route.SCAN }
    fun openTutorial() { _route.value = Route.TUTORIAL }
    fun finishTutorial() { stats.tutorialDone = true; _route.value = Route.HOME }
    fun goHome() { closeSession(); _route.value = Route.HOME }

    fun onBack(exitTashBox: () -> Unit) {
        when (_route.value) {
            Route.HOME -> exitTashBox()
            Route.TUTORIAL -> finishTutorial()
            Route.FRIENDS -> _route.value = Route.HOME
            Route.SCAN -> _route.value = Route.FRIENDS
            Route.SESSION -> goHome()
        }
    }

    fun setPlayerName(name: String) {
        _playerName.value = name.take(20)
        stats.playerName = name.trim().take(20)
    }

    /** Pre-fills the name from the host app if the player has not chosen one yet. */
    fun seedPlayerName(name: String) {
        if (stats.playerName.isBlank() && name.isNotBlank()) setPlayerName(name)
    }

    /** Home: picks the solo difficulty. Lobby (host): changes the room's difficulty for everybody. */
    fun setLevel(level: Difficulty) {
        _level.value = level
        stats.lastLevel = level
        (session as? HostSession)?.setLevel(level)
    }

    // ---- sessions -------------------------------------------------------------------------

    fun startSolo() {
        attach(SoloSession(viewModelScope, playerName = _playerName.value, level = _level.value))
    }

    fun createRoom() {
        viewModelScope.launch {
            val ip = withContext(Dispatchers.IO) { LocalNetwork.findLocalIpv4() }
            if (ip == null) {
                closeSession()
                _game.value = GameUiState(
                    role = SessionRole.HOST, phase = UiPhase.CONNECT_FAILED,
                    errorMessage = "Connect to a Wi-Fi network first,\nor turn on your phone's hotspot\nand ask friends to join it."
                )
                _route.value = Route.SESSION
                return@launch
            }
            val transport = try {
                TcpHostTransport.open(viewModelScope)
            } catch (e: Exception) {
                _game.value = GameUiState(
                    role = SessionRole.HOST, phase = UiPhase.CONNECT_FAILED,
                    errorMessage = "Could not open a room on this device.\nTry again."
                )
                _route.value = Route.SESSION
                return@launch
            }
            val name = _playerName.value.trim().ifBlank { "Host" }
            attach(HostSession(viewModelScope, transport, ip, transport.port, name, level = _level.value))
        }
    }

    fun joinFromQr(text: String) {
        val payload = QrPayload.parse(text)
        if (payload == null) {
            _scanError.value = "That isn't a TashBox room code."
            return
        }
        attach(ClientSession(viewModelScope, payload, _playerName.value.trim().ifBlank { "Player" }))
    }

    fun startMatch() { (session as? HostSession)?.startMatch() }

    fun playAgain() {
        when (val s = session) {
            is SoloSession -> startSolo()
            is HostSession -> s.rematch()
            else -> Unit // clients return to the lobby when the host starts a rematch
        }
    }

    fun submitAnswer(index: Int) { session?.submitAnswer(index) }
    fun continueNext() { session?.continueToNext() }

    // ---- internals ------------------------------------------------------------------------

    private fun attach(s: GameSession) {
        closeSession()
        session = s
        recorded = false
        _summary.value = null
        _game.value = s.state.value
        _route.value = Route.SESSION
        collectJob = viewModelScope.launch {
            s.state.collect { st ->
                _game.value = st
                val isFinished = st.phase == UiPhase.FINISHED
                val matchOver = isFinished || (st.phase == UiPhase.HOST_LEFT && st.result != null)
                if (matchOver && !recorded) {
                    recorded = true
                    st.result?.let { result ->
                        _summary.value = stats.record(result, st.myPlayerId)
                        _bestByLevel.value = stats.bestSoloAll()
                        if (isFinished) _finished.tryEmit(result to st.myPlayerId)
                    }
                }
            }
        }
    }

    private fun closeSession() {
        collectJob?.cancel()
        collectJob = null
        session?.close()
        session = null
        _game.value = null
    }

    override fun onCleared() {
        closeSession()
    }
}
