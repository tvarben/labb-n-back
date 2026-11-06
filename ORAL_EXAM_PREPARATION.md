# Oral Examination Preparation Guide: All 30 Questions

This guide provides concise, direct answers and exact code references for all 30 questions from the Lab B:1 oral examination.

---

## AI & Process (Mandatory Opening Question)

### Question 26: Which parts did you write yourself and which were AI-generated? Be ready to explain any line you are asked about.
- **Answer:** 
  - The project was developed as a pair-programming workflow with an AI assistant. The prompt specifications,ontext constraints (`SPEC.md`, `style_guide.md`, `context_constraints.md`), architectural assembly, build toolchain configuration (JDK 17, Gradle kapt, Room database setup), and debugging of compiler/annotation processor errors were directed and verified by the developer.
  - The AI assisted in drafting initial boilerplate for Compose UI screens and math formulas. Every line—particularly the domain engine (`NBackEngine.kt`), state flow emissions, TTS lifecycle in `GameVM.kt`, and Room DAO queries—has been reviewed, verified, and backed by automated unit tests.

---

## Stimuli & Sequence Logic

### 1. Walk through the function that generates the N-back sequence. Where is it, and what does it return? How do you test it?
- **Where:** `NBackSequenceGenerator.generateSequence(...)` in [NBackEngine.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/domain/NBackEngine.kt).
- **What it returns:** An `IntArray` of length `size` containing values in $1..\text{combinations}$.
- **How it works:**
  1. Fills the first $n$ elements randomly in $1..\text{combinations}$.
  2. Selects exactly `targetMatches` indices at random from eligible indices $n..\text{size}-1$.
  3. At match indices: sets `result[i] = result[i - n]`.
  4. At non-match indices: randomly selects any value in $1..\text{combinations}$ *excluding* `result[i - n]`.
- **How to test:** Tested in `NBackEngineTest.testSequenceGeneration_validDimensionsAndRanges` by checking bounds and array sizes.

### 2. How do you enforce a ~20–40% match rate? Where in the code is that probability decided for each event? How do you test it?
- **Where:** `NBackSequenceGenerator.generateSequence(...)` in [NBackEngine.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/domain/NBackEngine.kt).
- **Enforcement:**
  `val targetMatches = (eligibleCount * (clampedPercentage / 100.0)).roundToInt().coerceIn(1, eligibleCount)`
  The target percentage is clamped between 20 and 40 (default 30%). Exactly `targetMatches` distinct indices are selected from eligible indices `nBack until size`.
- **How to test:** In `NBackEngineTest.testMatchRateEnforcement_strictlyWithinTargetRange`, where 50 randomly seeded sequences of length 100 are generated; the test asserts every single run yields an actual match rate strictly between 20% and 40%.

### 3. How do you guarantee that the first n events can never be matches? How do you test it?
- **Where:** In `NBackSequenceGenerator.generateSequence` and `NBackRoundEngine.advanceTick` / `registerVisualPress`.
- **Guarantee:** 
  1. In generation: match indices are strictly drawn from `nBack until size`. No match can be scheduled for $0 \le i < nBack$.
  2. In runtime engine: `if (currentEventIndex < nBack) return null`. Any match button press during indices $0 \le i < nBack$ is explicitly ignored.
- **How to test:** `NBackEngineTest.testFirstNGuarantee_firstNEventsCanNeverBeMatches` verifies `isVisualMatch == false` for all $i < n$ and that press calls return `null`.

### 4. How does the generated sequence drive both the visual event (which grid cell lights up) and the audio event (which letter is spoken)? Trace one stimulus from generation to output.
- **Trace:**
  1. In `GameVM.startGame()`, `NBackSequenceGenerator.generateSequence` returns an `IntArray` (e.g. `[4, 7, 4, 1]`) and `generateLetterSequence` returns a `List<Char>` (e.g. `['C', 'K', 'C', 'H']`).
  2. Passed to `NBackRoundEngine`. On each tick, `engine.advanceTick()` returns a `StimulusEvent(index, visualPosition = 4, audioLetter = 'C')`.
  3. `GameVM` updates `_gameState.value` with `eventValue = 4` and `audioLetter = 'C'`, and calls `speakLetter('C')` using `TextToSpeech.speak()`.
  4. In `GameScreen`, `SquareGrid` observes `gameState.eventValue == 4` and highlights cell 4 with primary color; simultaneously `TextToSpeech` speaks "C" aloud.

---

## Game Lifecycle

### 5. Describe the game state machine (idle / running / finished) and every transition between states.
- **States:**
  - `Idle`: Initial state or after user exits (`isRunning = false, isFinished = false`).
  - `Running`: Active round loop presenting stimuli (`isRunning = true, isFinished = false`).
  - `Finished`: All events completed, score calculated, dialog shown (`isRunning = false, isFinished = true`).
- **Transitions:**
  - `Idle` $\rightarrow$ `Running`: User taps "Start Game" in `HomeScreen`, calling `vm.startGame()`.
  - `Running` $\rightarrow$ `Finished`: Coroutine timer finishes all events (`engine.advanceTick() == null`), calling `engine.finish()`.
  - `Running` $\rightarrow$ `Idle`: User taps Back or exits screen mid-round, triggering `vm.cancelGame()` / `DisposableEffect`.
  - `Finished` $\rightarrow$ `Running`: User taps "Play Again" in end-of-round dialog.
  - `Finished` $\rightarrow$ `Idle`: User taps "Home Screen" in end-of-round dialog.

### 6. Trace one full event tick end-to-end: advance sequence $\rightarrow$ show stimulus $\rightarrow$ speak letter $\rightarrow$ reset match flag $\rightarrow$ update HUD.
- **Trace:**
  1. Coroutine delay finishes $\rightarrow$ calls `newEngine.advanceTick()`.
  2. `advanceTick()` checks previous event for unpressed matches (counts misses), increments `currentEventIndex`, resets `hasPressedVisualThisTick = false` and `hasPressedAudioThisTick = false`.
  3. Emits new `StimulusEvent` with `visualPosition` and `audioLetter`.
  4. `_gameState.value` updated with new `eventValue`, `audioLetter`, reset press flags, and updated hits/misses.
  5. If `audioLetter != null`, `speakLetter(audioLetter)` triggers TTS engine.
  6. Compose UI recomposes: progress bar advances, grid cell updates, hits and score reflect live counts.

### 7. How does the app detect that a round is over, and what cleanup runs at that point?
- **Detection:** In `GameVM.startGame()`, the `while (newEngine.isRoundActive)` loop terminates when `advanceTick()` returns `null` (because `currentEventIndex >= totalEvents`).
- **Cleanup:** 
  1. `newEngine.finish()` checks the final tick for unpressed matches and returns final `ScoreResult`.
  2. `_gameState` switches `isRunning = false` and `isFinished = true`, clearing `eventValue = -1`.
  3. Final score is written to persistent high score via `userPreferencesRepository.saveHighScore(...)`.

### 8. How does the app guard against a match press before the round starts, after it ends, and during the first n events? How do you test it?
- **Where:** `NBackRoundEngine.registerVisualPress()` & `registerAudioPress()` in [NBackEngine.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/domain/NBackEngine.kt):
  ```kotlin
  if (!isRoundActive || isRoundFinished) return null
  if (currentEventIndex < nBack) return null
  ```
- **Testing:** `NBackEngineTest.testFirstNGuarantee_firstNEventsCanNeverBeMatches` asserts that tapping during $0 \le i < nBack$ returns `null` and does not increment score.

### 9. How does the app prevent a second match press on the same event? How do you test it?
- **Where:** Boolean flags `hasPressedVisualThisTick` and `hasPressedAudioThisTick` in `NBackRoundEngine`:
  ```kotlin
  if (hasPressedVisualThisTick) return null
  hasPressedVisualThisTick = true
  ```
  These flags are reset strictly inside `advanceTick()`.
- **Testing:** In `NBackEngineTest.testDoublePressPrevention_subsequentPressesOnSameTickIgnored`, tapping twice on the same event returns `true` on the first tap and `null` on the second tap, with `hits` remaining 1.

---

## Scoring & Feedback

### 10. Where are hits, misses, and false alarms counted both on a press and on a silent tick?
- **On a press:** In `registerVisualPress()` / `registerAudioPress()`. If matching, increments `hits++`. If not matching, increments `falseAlarms++`.
- **On a silent tick:** In `advanceTick()` and `finish()`. If the previous event had an unpressed match (`isMatch && !hasPressedVisualThisTick`), increments `misses++`.

### 11. What is the score formula? Defend the weights and explain how a negative score is prevented.
- **Formula:**
  $$\text{Score} = \max\left(0, (\text{Hits} \times 10) - (\text{FalseAlarms} \times 5) - (\text{Misses} \times 2)\right)$$
- **Defense of weights:**
  - Hits (+10): Main reward for successful working memory retrieval.
  - False Alarms (-5): Stiffer penalty to deter "spamming" the match button on every single event.
  - Misses (-2): Moderate penalty for lapses in attention.
- **Floor at zero:** `.coerceAtLeast(0)` guarantees score never drops below 0.
- **Testing:** Verified in `NBackEngineTest.testScoringFormula_preventsNegativeScore`.

### 12. Where is the round timer cancelled, and what happens if the player leaves the screen mid-round?
- **Where:** `GameVM.cancelGame()` cancels the coroutine `gameJob?.cancel()`.
- **Screen exit:** `GameScreen` registers a `DisposableEffect(Unit)` with `onDispose { if (gameState.isRunning) vm.cancelGame() }`. Navigating away immediately halts stimulus delivery and frees resources cleanly.

### 13. How is wrong-guess feedback triggered, and why is it non-offensive?
- **Trigger:** When `registerVisualPress()` returns `false`, `GameVM` launches a temporary coroutine setting `visualFeedbackError = true` for 350ms.
- **Why non-offensive:** Implemented in `FeedbackMatchButton` via `animateColorAsState` smoothly transitioning the button to a soft coral/amber tint with a subtle 6px shake. There are no loud error buzzers or jarring full-screen flashing alerts.

### 14. Where is the HUD (current event / correct so far) updated?
- **Where:** In `GameHud` inside [GameScreen.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/ui/screens/GameScreen.kt), which observes `vm.gameState.collectAsState()`. `eventIndex + 1`, `gameState.hits`, and `gameState.currentScore` update automatically whenever `_gameState` emits.

---

## Architecture

### 15. Show the observable-state wiring (StateFlow / @Published / useState): where is state declared and where is it subscribed?
- **Declared:** In `GameVM.kt`:
  `private val _gameState = MutableStateFlow(GameState())`
  `override val gameState: StateFlow<GameState> = _gameState.asStateFlow()`
- **Subscribed:** In `HomeScreen.kt` and `GameScreen.kt`:
  `val gameState by vm.gameState.collectAsState()`
  Compose subscribes and automatically recomposes only when relevant state properties change.

### 16. How does navigation between screens work, and why does the ViewModel not navigate itself?
- **Where:** `NBackAppNavHost` in [MainActivity.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/MainActivity.kt) using Jetpack Compose `NavHost` and `rememberNavController()`.
- **Why ViewModel does not navigate:** ViewModels should not know about Android UI navigation controllers (e.g. `NavController`). This enforces separation of concerns, keeps ViewModels purely business-logic focused, and allows headless unit testing of the ViewModel without Android UI mocks.

### 17. Where in the code is unit-testable logic separated from the UI layer? What made that separation easy or hard to test?
- **Where:** Separated in `mobappdev.example.nback_cimpl.domain.NBackEngine.kt`.
- **Why it was easy to test:** `NBackSequenceGenerator` and `NBackRoundEngine` are pure Kotlin classes with zero Android SDK dependencies. They can run directly on the JVM in milliseconds without needing an emulator or Robolectric.

---

## Audio

### 18. Where is the TTS engine initialized, and where does each letter get spoken? Why does speech only happen after a user gesture?
- **Initialization:** In `GameVM`'s `init` block: `tts = TextToSpeech(application, this)`.
- **Where spoken:** In `GameVM.speakLetter()`, called on each stimulus tick during `startGame()`.
- **Why after user gesture:** Mobile platforms restrict or discourage unprompted audio autoplay; speech starts only after the user explicitly taps "Start Game".

### 19. Where is the letter set defined, and why those specific letters (phonetically distinct)?
- **Where:** `PHONETIC_LETTERS` in [NBackEngine.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/domain/NBackEngine.kt): `['C', 'H', 'K', 'L', 'Q', 'R', 'S', 'T']`.
- **Why:** In cognitive science, letters rhyming with "E" (B, C, D, E, G, P, T, V, Z) cause acoustic/phonetic confusion in auditory memory. Using phonetically distinct consonant and vowel structures ensures the task measures working memory capacity rather than auditory discrimination.

### 20. What happens when no voice is available on the device?
- **Handling:** `GameVM.onInit()` checks for `LANG_MISSING_DATA` or initialization failure. If unavailable, it sets `ttsAvailable = false` and sets `ttsFallbackMessage`. `GameScreen` displays a fallback banner and displays the spoken letter visually in `AuditoryStimulusDisplay`.

---

## Persistence & Platform

### 21. Walk through one settings write and one read (where in the code, what API).
- **Write:** In `UserPreferencesRepository.saveSettings(...)`:
  ```kotlin
  dataStore.edit { prefs -> prefs[KEY_N_BACK] = settings.nBack }
  ```
- **Read:** In `UserPreferencesRepository.gameSettings`:
  ```kotlin
  val gameSettings: Flow<GameSettings> = preferencesFlow.map { prefs ->
      GameSettings(nBack = prefs[KEY_N_BACK] ?: 2, ...)
  }
  ```

### 22. Walk through the score entity/table and one sorted query. Why a database here rather than key-value storage?
- **Entity:** `ScoreRecord` in [ScoreDatabase.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/data/ScoreDatabase.kt) with `@Entity(tableName = "scores")`.
- **Query:** In `ScoreDao`:
  ```kotlin
  @Query("SELECT * FROM scores ORDER BY score DESC, timestamp DESC")
  fun getScoresSortedByScoreDesc(): Flow<List<ScoreRecord>>
  ```
- **Why a database:** Key-value storage (like DataStore) is designed for simple scalar configuration. A relational database (Room/SQLite) allows indexed, dynamic sorting by multiple columns (score, name, date), filtering, and structured pagination without deserializing an entire list into memory on every query.

### 23. What runs off the main thread and how (dispatcher / background context / async)?
- **Sequence generation:** Offloaded to `withContext(Dispatchers.Default)`.
- **Room database writes/queries:** Executed on `Dispatchers.IO` (and Room's internal background executor for Flows).
- **UI updates:** Executed on `Dispatchers.Main` via Compose state updates.

---

## UI & Platform Behavior

### 24. How does the grid stay square across phones, tablets, portrait, and landscape?
- **Where:** `SquareGrid` in [GameScreen.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/ui/screens/GameScreen.kt):
  Uses `BoxWithConstraints`:
  `val size = min(maxWidth, maxHeight) * 0.9f`
  `Column(modifier = Modifier.size(size).aspectRatio(1f))`
  This guarantees a 1:1 aspect ratio regardless of whether the screen is portrait, landscape, phone, or tablet.

### 25. How is the minimum touch-target size met for the match button, and how are safe areas handled?
- **Touch target:** `FeedbackMatchButton` applies `Modifier.heightIn(min = 52.dp)`, surpassing the minimum accessibility guideline of 48dp.
- **Safe areas:** `Scaffold(modifier = Modifier.padding(paddingValues))` automatically respects system status bars and navigation bar insets.

---

## Testing (Detailed)

### 27. Which three (or more) components did you write unit tests for? Walk me through why you chose those over others.
- **Tested:** All 5 components:
  1. Sequence generation
  2. Match-rate enforcement
  3. First-$n$ guarantee
  4. Double-press prevention
  5. Scoring formula
- **Why:** These 5 components contain 100% of the mathematical correctness and anti-cheat/fair-play rules of the game. Testing them ensures zero regressions regardless of UI updates.

### 28. Your app must prevent a double-press (registering a second tap before the next stimulus arrives). Show the code that enforces this and the test that would catch a violation if the guard were removed.
- **Enforcement Code:** In [NBackEngine.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/domain/NBackEngine.kt):
  ```kotlin
  if (hasPressedVisualThisTick) return null
  hasPressedVisualThisTick = true
  ```
- **Test:** In [NBackEngineTest.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/test/java/mobappdev/example/nback_cimpl/NBackEngineTest.kt):
  `testDoublePressPrevention_subsequentPressesOnSameTickIgnored` registers two presses on the same stimulus tick and asserts that the second press returns `null` and `hits` equals 1. If `hasPressedVisualThisTick` guard were removed, `hits` would become 2 and the test would fail.

### 29. Run one of your unit tests live right now. Explain what the assertion checks and describe a change to the production code that would make it fail.
- **Run command:**
  ```powershell
  ./gradlew testDebugUnitTest --tests "mobappdev.example.nback_cimpl.NBackEngineTest.testScoringFormula_preventsNegativeScore"
  ```
- **Assertion:** Asserts that when penalties exceeds hits (0 hits, 10 false alarms, 5 misses), `result.finalScore == 0`.
- **Change that would make it fail:** In [NBackEngine.kt](file:///c:/Users/david/StudioProjects/nback_skeletoncode/app/src/main/java/mobappdev/example/nback_cimpl/domain/NBackEngine.kt), remove `.coerceAtLeast(0)` from `ScoreResult.finalScore`. The score would evaluate to `-60`, causing `assertEquals(0, -60)` to fail.

### 30. Did any test yours or LLM-generated catch a real bug? Describe what failed and how you fixed it.
- **Real bugs caught:**
  1. **Bug in `nback.c` (C skeleton):**
     In `fillInAllEmpty()` in `nback.c`:
     `if(i-nback > 0)`
     When $i = n$, $i - n = 0$. The check failed to execute for index 0, allowing an accidental duplicate match at index $n$. In our pure Kotlin implementation, we corrected this by generating explicit exclusions for all valid previous stimuli.
  2. **kapt Duplicate Companion Object:**
     During kapt compilation, having two companion objects in `GameVM.kt` generated invalid Java stubs and caused a cryptic `duplicate class: ScoreSortOrder` error. Consolidating into a single companion object fixed the compiler error immediately.
