package mobappdev.example.nback_cimpl.ui.viewmodels

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.data.ScoreDao
import mobappdev.example.nback_cimpl.data.ScoreRecord
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.domain.GameSettings
import mobappdev.example.nback_cimpl.domain.NBackRoundEngine
import mobappdev.example.nback_cimpl.domain.NBackSequenceGenerator
import java.util.Locale

/**
 * Supported game stimulus modes.
 */
enum class GameType {
    Audio,
    Visual,
    AudioVisual
}

/**
 * Sort criteria for score history screen.
 */
enum class ScoreSortOrder {
    BY_SCORE,
    BY_NAME,
    BY_DATE
}

/**
 * Consolidated UI state representing live game state, stimuli, scores, and feedback animations.
 */
data class GameState(
    val gameType: GameType = GameType.Visual,
    val isRunning: Boolean = false, // gamestate
    val isFinished: Boolean = false, //gamestate
    val eventIndex: Int = -1,
    val totalEvents: Int = 20,
    val eventValue: Int = -1,
    val audioLetter: Char? = null,
    val hits: Int = 0,
    val misses: Int = 0,
    val falseAlarms: Int = 0,
    val currentScore: Int = 0,
    val finalScore: Int = 0,
    val visualFeedbackError: Boolean = false,
    val audioFeedbackError: Boolean = false,
    val visualPressedThisTick: Boolean = false,
    val audioPressedThisTick: Boolean = false,
    val ttsAvailable: Boolean = true,
    val ttsFallbackMessage: String? = null
)

/**
 * ViewModel interface for testing and preview fakes.
 */
interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val settings: StateFlow<GameSettings>
    val scoresList: StateFlow<List<ScoreRecord>>
    val sortOrder: StateFlow<ScoreSortOrder>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun setScoreSortOrder(order: ScoreSortOrder)
    fun updateSettings(newSettings: GameSettings)
    fun startGame()
    fun cancelGame()
    fun onVisualMatchPress()
    fun onAudioMatchPress()
    fun checkMatch() // legacy alias
    fun saveScore(playerName: String)
}

/**
 * Primary ViewModel managing N-Back game lifecycle, TTS engine, persistence, and state.
 */
class GameVM(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scoreDao: ScoreDao
) : AndroidViewModel(application), GameViewModel, TextToSpeech.OnInitListener {

    private val _gameState = MutableStateFlow(GameState()) //15. inside HomeScreen,GameScreen with collectAsState
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    private val _settings = MutableStateFlow(GameSettings())
    override val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _sortOrder = MutableStateFlow(ScoreSortOrder.BY_SCORE)
    override val sortOrder: StateFlow<ScoreSortOrder> = _sortOrder.asStateFlow()

    private var engine: NBackRoundEngine? = null
    private var gameJob: Job? = null

    // Android TextToSpeech engine
    private var tts: TextToSpeech? = null
    private var isTtsInitialized: Boolean = false

    override val nBack: Int
        get() = _settings.value.nBack

    // Room reactive score list sorted based on _sortOrder
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val scoresList: StateFlow<List<ScoreRecord>> = _sortOrder.flatMapLatest { order ->
        when (order) {
            ScoreSortOrder.BY_SCORE -> scoreDao.getScoresSortedByScoreDesc()
            ScoreSortOrder.BY_NAME -> scoreDao.getScoresSortedByNameAsc()
            ScoreSortOrder.BY_DATE -> scoreDao.getScoresSortedByDateDesc()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect persistent settings
        viewModelScope.launch {
            userPreferencesRepository.gameSettings.collect { loadedSettings ->
                _settings.value = loadedSettings
            }
        }

        // Collect persistent highscore
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect { loadedHighscore ->
                _highscore.value = loadedHighscore
            }
        }

        // Initialize Android TextToSpeech
        try {
            tts = TextToSpeech(application, this) //18
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
            _gameState.value = _gameState.value.copy(
                ttsAvailable = false,
                ttsFallbackMessage = "Text-to-speech could not be initialized."
            )
        }
    }

    //20 check for LANG_MISSING_DATA if unavailable ttsAvailable gamestate is set to false so it cannot cause a crash
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS language missing or not supported on this device.")
                _gameState.value = _gameState.value.copy(
                    ttsAvailable = false,
                    ttsFallbackMessage = "English voice data not installed on this device."
                )
            } else {
                isTtsInitialized = true
                _gameState.value = _gameState.value.copy(ttsAvailable = true, ttsFallbackMessage = null)
            }
        } else {
            Log.e(TAG, "TTS initialization failed with code: $status")
            _gameState.value = _gameState.value.copy(
                ttsAvailable = false,
                ttsFallbackMessage = "Text-to-speech engine failed to initialize."
            )
        }
    }

    private fun speakLetter(letter: Char) {
        if (isTtsInitialized && tts != null) {
            tts?.speak(letter.toString(), TextToSpeech.QUEUE_FLUSH, null, "nback_tts_${System.currentTimeMillis()}")
        }
    }

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun setScoreSortOrder(order: ScoreSortOrder) {
        _sortOrder.value = order
    }

    override fun updateSettings(newSettings: GameSettings) {
        _settings.value = newSettings
        viewModelScope.launch {
            userPreferencesRepository.saveSettings(newSettings)
        }
    }

    override fun startGame() {
        // Cleanly cancel any existing round timer
        cancelGame()

        val currentSettings = _settings.value
        val gameType = _gameState.value.gameType
        val totalCombinations = currentSettings.gridSize * currentSettings.gridSize

        // Generate sequences off the main thread


        //4. In `GameVM.startGame()`, `NBackSequenceGenerator.generateSequence` returns an `IntArray` (e.g. `[4, 7, 4, 1]`) and `generateLetterSequence` returns a `List<Char>` (e.g. `['C', 'K', 'C', 'H']`).  Passed to `NBackRoundEngine`. On each tick, `engine.advanceTick()` returns a `StimulusEvent(index, visualPosition = 4, audioLetter = 'C')`. `GameVM` updates `_gameState.value` with `eventValue = 4` and `audioLetter = 'C'`, and calls `speakLetter('C')` using `TextToSpeech.speak()`. In `GameScreen`, `SquareGrid` observes `gameState.eventValue == 4` and highlights cell 4 with primary color; simultaneously `TextToSpeech` speaks "C" aloud.
        viewModelScope.launch {
            val (visualSeq, audioSeq) = withContext(Dispatchers.Default) { //23 sequence generation thread
                //23.3 UI is updated on Dispatchers.Main
                val vSeq = if (gameType != GameType.Audio) {
                    NBackSequenceGenerator.generateSequence(
                        size = currentSettings.eventCount,
                        combinations = totalCombinations,
                        targetPercentage = currentSettings.targetMatchPercentage,
                        nBack = currentSettings.nBack
                    )
                } else null

                val aSeq = if (gameType != GameType.Visual) {
                    NBackSequenceGenerator.generateLetterSequence(
                        size = currentSettings.eventCount,
                        letterCount = currentSettings.letterCount,
                        targetPercentage = currentSettings.targetMatchPercentage,
                        nBack = currentSettings.nBack
                    )
                } else null

                Pair(vSeq, aSeq)
            }

            val newEngine = NBackRoundEngine(
                visualSequence = visualSeq,
                audioSequence = audioSeq,
                nBack = currentSettings.nBack
            )
            engine = newEngine
            newEngine.start()

            _score.value = 0
            _gameState.value = _gameState.value.copy(
                isRunning = true,
                isFinished = false,
                eventIndex = -1,
                totalEvents = currentSettings.eventCount,
                eventValue = -1,
                audioLetter = null,
                hits = 0,
                misses = 0,
                falseAlarms = 0,
                currentScore = 0,
                finalScore = 0,
                visualFeedbackError = false,
                audioFeedbackError = false,
                visualPressedThisTick = false,
                audioPressedThisTick = false
            )

            // Game loop coroutine
            gameJob = launch {
                while (newEngine.isRoundActive) {
                    val event = newEngine.advanceTick() //returns null and game ends when event is null because currentEventIndex >= totalEvents
                    //and event value is sen to -1
                    if (event == null) {
                        break
                    }

                    _gameState.value = _gameState.value.copy(
                        eventIndex = event.index,
                        eventValue = event.visualPosition,
                        audioLetter = event.audioLetter,
                        hits = newEngine.hits,
                        misses = newEngine.misses,
                        falseAlarms = newEngine.falseAlarms,
                        visualPressedThisTick = false,
                        audioPressedThisTick = false
                    )

                    // Auditory stimulus output
                    //18.2
                    //18.3 don't know
                    if (event.audioLetter != null) {
                        speakLetter(event.audioLetter)
                    }

                    delay(currentSettings.eventDurationMs)
                }

                // Round complete
                val result = newEngine.finish()
                val earnedScore = result.finalScore
                _score.value = earnedScore
                _gameState.value = _gameState.value.copy(
                    isRunning = false,
                    isFinished = true,
                    eventValue = -1,
                    audioLetter = null,
                    hits = result.hits,
                    misses = result.misses,
                    falseAlarms = result.falseAlarms,
                    finalScore = earnedScore
                )

                // Update persistent high score if exceeded
                userPreferencesRepository.saveHighScore(earnedScore)
            }
        }
    }

    //12
    override fun cancelGame() {
        gameJob?.cancel() //cancels the coroutine
        gameJob = null
        engine = null
        _gameState.value = _gameState.value.copy(
            isRunning = false,
            eventValue = -1,
            audioLetter = null
        )
    }

    override fun onVisualMatchPress() {
        val currentEngine = engine ?: return
        //13 returns false, GameVM launches temporary coroutine to animate error
        val result = currentEngine.registerVisualPress() ?: return // Ignored if first-n, inactive, or double-press

        _gameState.value = _gameState.value.copy(
            hits = currentEngine.hits,
            falseAlarms = currentEngine.falseAlarms,
            visualPressedThisTick = true
        )
        _score.value = (currentEngine.hits * 10 - currentEngine.falseAlarms * 5 - currentEngine.misses * 2).coerceAtLeast(0)

        if (!result) {
            // Trigger non-offensive animated feedback on incorrect press
            triggerVisualFeedbackError()
        }
    }

    override fun onAudioMatchPress() {
        val currentEngine = engine ?: return
        val result = currentEngine.registerAudioPress() ?: return // Ignored if first-n, inactive, or double-press

        _gameState.value = _gameState.value.copy(
            hits = currentEngine.hits,
            falseAlarms = currentEngine.falseAlarms,
            audioPressedThisTick = true
        )
        _score.value = (currentEngine.hits * 10 - currentEngine.falseAlarms * 5 - currentEngine.misses * 2).coerceAtLeast(0)

        if (!result) {
            // Trigger non-offensive animated feedback on incorrect press
            triggerAudioFeedbackError()
        }
    }

    override fun checkMatch() {
        // Skeleton code compatibility alias
        onVisualMatchPress()
    }

    private fun triggerVisualFeedbackError() {
        viewModelScope.launch {
            _gameState.value = _gameState.value.copy(visualFeedbackError = true)
            delay(350L)
            _gameState.value = _gameState.value.copy(visualFeedbackError = false)
        }
    }

    private fun triggerAudioFeedbackError() {
        viewModelScope.launch {
            _gameState.value = _gameState.value.copy(audioFeedbackError = true)
            delay(350L)
            _gameState.value = _gameState.value.copy(audioFeedbackError = false)
        }
    }

    override fun saveScore(playerName: String) {
        val trimmed = playerName.trim().ifEmpty { "Anonymous" }
        val state = _gameState.value
        val record = ScoreRecord(
            playerName = trimmed,
            score = state.finalScore,
            hits = state.hits,
            misses = state.misses,
            falseAlarms = state.falseAlarms,
            nBack = _settings.value.nBack,
            gameType = state.gameType.name,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) { //23.2 database thread
            scoreDao.insertScore(record)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel round timer cleanly
        cancelGame()

        // Release Android TextToSpeech engine
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
    }

    companion object {
        private const val TAG = "GameVM"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as GameApplication)
                GameVM(
                    application = app,
                    userPreferencesRepository = app.userPreferencesRepository,
                    scoreDao = app.scoreDao
                )
            }
        }
    }
}

/**
 * Preview / mock fake ViewModel for Compose previews and testing.
 */
class FakeVM : GameViewModel {
    override val gameState: StateFlow<GameState> = MutableStateFlow(GameState(eventValue = 5, audioLetter = 'K'))
    override val score: StateFlow<Int> = MutableStateFlow(30)
    override val highscore: StateFlow<Int> = MutableStateFlow(42)
    override val settings: StateFlow<GameSettings> = MutableStateFlow(GameSettings())
    override val scoresList: StateFlow<List<ScoreRecord>> = MutableStateFlow(emptyList())
    override val sortOrder: StateFlow<ScoreSortOrder> = MutableStateFlow(ScoreSortOrder.BY_SCORE)
    override val nBack: Int = 2

    override fun setGameType(gameType: GameType) {}
    override fun setScoreSortOrder(order: ScoreSortOrder) {}
    override fun updateSettings(newSettings: GameSettings) {}
    override fun startGame() {}
    override fun cancelGame() {}
    override fun onVisualMatchPress() {}
    override fun onAudioMatchPress() {}
    override fun checkMatch() {}
    override fun saveScore(playerName: String) {}
}
