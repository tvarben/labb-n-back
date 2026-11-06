package mobappdev.example.nback_cimpl.domain

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Phonetically distinct letter constants.
 *
 * Selected to avoid the rhyming 'E' group (B, C, D, E, G, P, T, V, Z)
 * which often cause auditory confusion during working memory tests.
 * Phonetically distinct consonant and vowel structures:
 * C (/siː/), H (/eɪtʃ/), K (/keɪ/), L (/ɛl/), Q (/kjuː/), R (/ɑːr/), S (/ɛs/), T (/tiː/)
 */
val PHONETIC_LETTERS: List<Char> = listOf('C', 'H', 'K', 'L', 'Q', 'R', 'S', 'T')

/**
 * Stimulus event for a single time tick.
 *
 * @property index 0-based index of the event in the round.
 * @property visualPosition 1-based index (1..gridSize*gridSize) for the grid cell, or -1 if audio-only.
 * @property audioLetter Spoken character, or null if visual-only.
 * @property isVisualMatch True if this visual stimulus matches the one n-steps earlier.
 * @property isAudioMatch True if this audio stimulus matches the one n-steps earlier.
 */
data class StimulusEvent(
    val index: Int,
    val visualPosition: Int = -1, //-1 means audio-only
    val audioLetter: Char? = null,
    val isVisualMatch: Boolean = false,
    val isAudioMatch: Boolean = false
)

/**
 * Parameters to configure a round of N-back.
 *
 * @property nBack The 'n' in n-back (e.g. 1, 2, 3).
 * @property eventCount Total number of events/stimuli in the round.
 * @property eventDurationMs Interval between stimuli in milliseconds.
 * @property gridSize Dimension of grid (e.g. 3 for 3x3).
 * @property targetMatchPercentage Target percentage of matches (e.g. 30 for 30%).
 * @property letterCount Number of distinct letters to sample from (up to PHONETIC_LETTERS.size).
 */
data class GameSettings(
    val nBack: Int = 2,
    val eventCount: Int = 20,
    val eventDurationMs: Long = 2000L,
    val gridSize: Int = 3,
    val targetMatchPercentage: Int = 30,
    val letterCount: Int = 8
)

/**
 * Scoring breakdown for a round.
 *
 * Formula: max(0, (hits * 10) - (falseAlarms * 5) - (misses * 2))
 */
data class ScoreResult(
    val hits: Int = 0,
    val misses: Int = 0,
    val falseAlarms: Int = 0,
    val totalMatches: Int = 0
) {
    //12 and coerce guarantees it never gets below 0
    val finalScore: Int
        get() = (hits * 10 - falseAlarms * 5 - misses * 2).coerceAtLeast(0)
}

/**
 * Sequence generator implementing custom N-Back sequence generation.
 *
 * Guarantees:
 * 1. The first `n` events can NEVER be matches (indices 0 until n).
 * 2. Match rate is rigorously enforced within ~20% - 40% (default target 30%).
 * 3. Non-match positions are guaranteed not to match the stimulus n steps earlier.
 */
object NBackSequenceGenerator {

    /**
     * Generates a 1-based integer sequence for grid positions (1..combinations).
     *
     * @param size Total number of events in the sequence.
     * @param combinations Total distinct possibilities (e.g. 9 for 3x3 grid).
     * @param targetPercentage Target match percentage (default 30, strictly clamped 20..40).
     * @param nBack The n-back distance.
     * @param random Random instance for reproducibility in unit tests.
     * @return Array of size elements with values in 1..combinations.
     */
    //1. fills array with random numbers from 1 to combinations
    // select exactly targetMatches distinct positions from nBack until size to be matches
    // fill elements from nBack until size
    fun generateSequence(
        size: Int,
        combinations: Int,
        targetPercentage: Int = 30,
        nBack: Int = 2,
        random: Random = Random.Default
    ): IntArray {
        require(size > nBack) { "Size ($size) must be strictly greater than nBack ($nBack)" }
        require(combinations >= 2) { "Combinations ($combinations) must be at least 2" }

        //2 take percentage, round it
        val clampedPercentage = targetPercentage.coerceIn(20, 40)
        val eligibleCount = size - nBack //how many turns can actually be a match
        val targetMatches = (eligibleCount * (clampedPercentage / 100.0)).roundToInt().coerceIn(1, eligibleCount)
        //turn percentage into coefficient and multiply with eligibleCount to get number of matches

        val result = IntArray(size)

        // Step 1: Assign the first n elements randomly (1..combinations).
        // First n elements can never be matches because there are no elements n-steps prior.
        for (i in 0 until nBack) {
            result[i] = random.nextInt(1, combinations + 1)
        }

        // Step 2: Choose exactly targetMatches distinct positions from nBack until size to be matches.

        //3 from nBack until size, skips nBack which is number of turns
        val eligibleIndices = (nBack until size).shuffled(random)
        val matchIndices = eligibleIndices.take(targetMatches).toSet() //create set and use scrambled list numbers (based on targetMatches)

        // Step 3: Fill elements from nBack until size
        for (i in nBack until size) {
            val previousValue = result[i - nBack] //if this turn is a match, copy grid square from N turns ago
            if (i in matchIndices) { //
                // Must be a match: exact duplicate of the value n steps prior
                result[i] = previousValue
            } else { //otherwise generate a random value
                // Must NOT be a match: pick any value 1..combinations EXCEPT previousValue
                var pick = random.nextInt(1, combinations)
                if (pick >= previousValue) {
                    pick += 1
                }
                result[i] = pick
            }
        }

        return result
    }

    /**
     * Generates a letter sequence for auditory stimuli from the phonetically distinct letters.
     */
    fun generateLetterSequence(
        size: Int,
        letterCount: Int,
        targetPercentage: Int = 30,
        nBack: Int = 2,
        random: Random = Random.Default
    ): List<Char> {
        val availableLetters = PHONETIC_LETTERS.take(letterCount.coerceIn(2, PHONETIC_LETTERS.size)) //19 - dont know, random. If they are distinct it might be eaiser to memorize
        val indices = generateSequence(
            size = size,
            combinations = availableLetters.size,
            targetPercentage = targetPercentage,
            nBack = nBack,
            random = random
        )
        return indices.map { availableLetters[it - 1] }
    }
}

/**
 * Domain engine tracking round progress, double-press prevention, and scoring.
 * Completely independent of Android UI, making it 100% unit-testable.
 *
 */
//acts as the live game referee
class NBackRoundEngine(
    val visualSequence: IntArray?,
    val audioSequence: List<Char>?,
    val nBack: Int
) {
    val totalEvents: Int = visualSequence?.size ?: audioSequence?.size ?: 0

    var currentEventIndex: Int = -1
        private set

    var isRoundActive: Boolean = false
        private set

    var isRoundFinished: Boolean = false
        private set

    // Flags to ensure double-press prevention: only 1 press accepted per stimulus tick
    var hasPressedVisualThisTick: Boolean = false
        private set

    var hasPressedAudioThisTick: Boolean = false
        private set

    // Scoring counters
    var hits: Int = 0
        private set
    var misses: Int = 0
        private set
    var falseAlarms: Int = 0
        private set

    fun start() {
        currentEventIndex = -1
        isRoundActive = true
        isRoundFinished = false
        hits = 0
        misses = 0
        falseAlarms = 0
        hasPressedVisualThisTick = false
        hasPressedAudioThisTick = false
    }

    /**
     * Advances to the next stimulus tick.
     * Computes any misses from the previous tick if a match was present but unpressed.
     *
     * @return The next StimulusEvent, or null if the round is finished.
     */
fun advanceTick(): StimulusEvent? {
    // 1. Guard Clause: Don't advance if game is stopped, paused, or finished
    if (!isRoundActive) return null

    // 2. Evaluate previous tick's misses:
    // Only evaluate turns at or past nBack (indices 0 until nBack are warm-up turns with no possible matches)
    if (currentEventIndex >= nBack) {
        // Check if the tick that JUST completed contained a visual match
        val hadVisualMatch = visualSequence != null &&
                visualSequence[currentEventIndex] == visualSequence[currentEventIndex - nBack]

        //10.2 och tolv
        // Match was present, but user didn't tap the visual button -> Count as Miss
        if (hadVisualMatch && !hasPressedVisualThisTick) {
            misses++
        }

        // Check if the tick that JUST completed contained an audio match
        val hadAudioMatch = audioSequence != null &&
                audioSequence[currentEventIndex] == audioSequence[currentEventIndex - nBack]

        // Match was present, but user didn't tap the audio button -> Count as Miss
        if (hadAudioMatch && !hasPressedAudioThisTick) {
            misses++
        }
    }

    // 3. Move turn counter forward to the current tick
    currentEventIndex++

    // 4. End-of-Round Check: Stop engine if we reached the final event in the sequence
    if (currentEventIndex >= totalEvents) {
        isRoundActive = false
        isRoundFinished = true
        return null
    }

    // 5. Reset per-turn input locks so the player can press buttons for this new tick
    hasPressedVisualThisTick = false
    hasPressedAudioThisTick = false

    // 6. Compute match flags for the NEW current turn (to be passed to UI/listeners)
    val isVisualMatch = currentEventIndex >= nBack &&
            visualSequence != null &&
            visualSequence[currentEventIndex] == visualSequence[currentEventIndex - nBack]

    val isAudioMatch = currentEventIndex >= nBack &&
            audioSequence != null &&
            audioSequence[currentEventIndex] == audioSequence[currentEventIndex - nBack]

    // 7. Construct and return the state data for this turn
    return StimulusEvent(
        index = currentEventIndex,
        visualPosition = visualSequence?.get(currentEventIndex) ?: -1, // Returns -1 if visual sequence is null
        audioLetter = audioSequence?.get(currentEventIndex),          // Returns null if audio sequence is null
        isVisualMatch = isVisualMatch,
        isAudioMatch = isAudioMatch
    )
}
    /**
     * Registers a visual match press.
     *
     * Rules:
     * - Ignored if round not active or finished.
     * - Ignored during the first n events (indices 0 until nBack).
     * - Ignored if already pressed this tick (double-press prevention).
     *
     * @return Result: null if ignored, true if hit, false if false alarm.
     */
    fun registerVisualPress(): Boolean? {
        //8 - 2
        if (!isRoundActive || isRoundFinished) return null
        if (currentEventIndex < nBack) return null // First n events guarantee
        //9
        if (hasPressedVisualThisTick) return null // Double-press prevention

        hasPressedVisualThisTick = true

        val isMatch = visualSequence != null &&
                visualSequence[currentEventIndex] == visualSequence[currentEventIndex - nBack]

        //10
        if (isMatch) {
            hits++
            return true
        } else {
            falseAlarms++
            return false
        }
    }

  /**
 * Registers an audio match press when the user taps the Audio button.
 *
 * @return true if correct hit, false if false alarm, or null if press was ignored.
 */
fun registerAudioPress(): Boolean? {
    // 1. Guard Clause: Ignore press if the round hasn't started or has already ended
    if (!isRoundActive || isRoundFinished) return null

    // 2. Guard Clause: Ignore press during warm-up turns (indices 0 until nBack have no N-Back reference)
    if (currentEventIndex < nBack) return null

    // 3. Guard Clause: Prevent double-tapping within the same tick interval
    if (hasPressedAudioThisTick) return null

    // Lock audio pressing for the remainder of this tick
    hasPressedAudioThisTick = true

    // 4. Evaluate whether the current audio letter matches the letter from N turns ago
    val isMatch = audioSequence != null &&
            audioSequence[currentEventIndex] == audioSequence[currentEventIndex - nBack]

    // 5. Update scoring counters based on evaluation result
    if (isMatch) {
        hits++        // User correctly identified a match -> increment Hit counter
        return true   // Return true for UI feedback (e.g., success sound/green flash)
    } else {
        falseAlarms++ // User pressed when there was no match -> increment False Alarm counter
        return false  // Return false for UI feedback (e.g., error vibration/red flash)
    }
}

    /**
     * Finishes the round cleanly and returns the final score result.
     */
    fun finish(): ScoreResult {
        // Account for any unpressed match on the final tick
        if (currentEventIndex in nBack until totalEvents) {
            val hadVisualMatch = visualSequence != null &&
                    visualSequence[currentEventIndex] == visualSequence[currentEventIndex - nBack]
            if (hadVisualMatch && !hasPressedVisualThisTick) {
                misses++
            }

            val hadAudioMatch = audioSequence != null &&
                    audioSequence[currentEventIndex] == audioSequence[currentEventIndex - nBack]
            if (hadAudioMatch && !hasPressedAudioThisTick) {
                misses++
            }
        }

        isRoundActive = false
        isRoundFinished = true

        var totalMatches = 0
        if (visualSequence != null) {
            for (i in nBack until totalEvents) {
                if (visualSequence[i] == visualSequence[i - nBack]) totalMatches++
            }
        }
        if (audioSequence != null) {
            for (i in nBack until totalEvents) {
                if (audioSequence[i] == audioSequence[i - nBack]) totalMatches++
            }
        }

        return ScoreResult(
            hits = hits,
            misses = misses,
            falseAlarms = falseAlarms,
            totalMatches = totalMatches
        )
    }
}
