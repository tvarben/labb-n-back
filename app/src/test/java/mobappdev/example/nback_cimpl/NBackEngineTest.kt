package mobappdev.example.nback_cimpl

import mobappdev.example.nback_cimpl.domain.NBackRoundEngine
import mobappdev.example.nback_cimpl.domain.NBackSequenceGenerator
import mobappdev.example.nback_cimpl.domain.PHONETIC_LETTERS
import mobappdev.example.nback_cimpl.domain.ScoreResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

//17. separation IS easy because the logic classes are pure Kotlin classes without any android dependencies, so they can be tested in without needing an emulator
/**
 * Unit tests covering core logic requirements for Lab B:1.
 * Tests all 5 core components:
 * 1. Sequence generation
 * 2. Match-rate enforcement (~20-40%)
 * 3. First-n guarantee
 * 4. Double-press prevention
 * 5. Scoring formula (including floor at zero)
 */
class NBackEngineTest {

    // =========================================================================
    // Component 1: Sequence Generation
    // =========================================================================

    @Test
    fun testSequenceGeneration_validDimensionsAndRanges() {
        val size = 20
        val combinations = 9
        val nBack = 2
        val sequence = NBackSequenceGenerator.generateSequence(
            size = size,
            combinations = combinations,
            targetPercentage = 30,
            nBack = nBack,
            random = Random(42)
        )

        assertEquals("Sequence length must match requested size", size, sequence.size)
        for (value in sequence) {
            assertTrue("Every value must be within 1..combinations (1..$combinations)", value in 1..combinations)
        }
    }

    @Test
    fun testAudioLetterSequenceGeneration_usesPhoneticSet() {
        val size = 25
        val letterCount = 6
        val nBack = 2
        val letterSequence = NBackSequenceGenerator.generateLetterSequence(
            size = size,
            letterCount = letterCount,
            targetPercentage = 30,
            nBack = nBack,
            random = Random(123)
        )

        assertEquals(size, letterSequence.size)
        val validSubset = PHONETIC_LETTERS.take(letterCount).toSet()
        for (letter in letterSequence) {
            assertTrue("Letter '$letter' must belong to the phonetically distinct subset", letter in validSubset)
        }
    }

    // =========================================================================
    // Component 2: Match-Rate Enforcement (~20-40%)
    // =========================================================================

    //2.3
    @Test
    fun testMatchRateEnforcement_strictlyWithinTargetRange() {
        val size = 100
        val combinations = 9
        val nBack = 2
        val targetPercentage = 30

        // Test over 50 random seeds to guarantee statistical enforcement
        for (seed in 1..50) {
            val sequence = NBackSequenceGenerator.generateSequence(
                size = size,
                combinations = combinations,
                targetPercentage = targetPercentage,
                nBack = nBack,
                random = Random(seed)
            )

            var actualMatches = 0
            for (i in nBack until size) {
                if (sequence[i] == sequence[i - nBack]) {
                    actualMatches++
                }
            }

            val eligiblePositions = size - nBack // 98
            val actualPercentage = (actualMatches.toDouble() / eligiblePositions) * 100.0

            assertTrue(
                "Match percentage ($actualPercentage%) must be between 20% and 40% (matches: $actualMatches/$eligiblePositions, seed $seed)",
                actualPercentage in 20.0..40.0
            )
        }
    }

    // =========================================================================
    // Component 3: First-n Guarantee
    // =========================================================================

    //8.2
    @Test
    fun testFirstNGuarantee_firstNEventsCanNeverBeMatches() {
        val nBack = 3
        val size = 15
        val combinations = 4
        val sequence = NBackSequenceGenerator.generateSequence(
            size = size,
            combinations = combinations,
            targetPercentage = 30,
            nBack = nBack,
            random = Random(999)
        )

        val engine = NBackRoundEngine(
            visualSequence = sequence,
            audioSequence = null,
            nBack = nBack
        )
        engine.start()

        // Tick through the first n events
        for (i in 0 until nBack) {
            val event = engine.advanceTick()
            assertNotNull("Event $i should be generated", event)
            assertFalse(
                "Stimulus at index $i (< nBack $nBack) can NEVER be marked as a match",
                event!!.isVisualMatch
            )

            // Any match button tap during the first n events MUST be ignored (return null)
            val pressResult = engine.registerVisualPress()
            assertNull("Match button press at index $i (< nBack $nBack) must be ignored", pressResult)
        }
    }

    // =========================================================================
    // Component 4: Double-Press Prevention
    // =========================================================================

    //9.2
    @Test
    fun testDoublePressPrevention_subsequentPressesOnSameTickIgnored() {
        // Construct a known sequence where event at index 2 matches event at index 0 (n=2)
        val visualSequence = intArrayOf(1, 2, 1, 4, 1)
        val engine = NBackRoundEngine(
            visualSequence = visualSequence,
            audioSequence = null,
            nBack = 2
        )
        engine.start()

        // Advance to event 0, 1, 2
        engine.advanceTick() // index 0 (val 1)
        engine.advanceTick() // index 1 (val 2)
        engine.advanceTick() // index 2 (val 1) -> Match with index 0!

        // First press should succeed as a Hit (returns true)
        val firstPress = engine.registerVisualPress()
        assertEquals("First tap on a match must register as hit (true)", true, firstPress)
        assertEquals("Hits count should be 1", 1, engine.hits)

        // Second press on the exact same tick MUST be ignored (returns null)
        val secondPress = engine.registerVisualPress()
        assertNull("Second tap on the same stimulus must be ignored by the guard", secondPress)
        assertEquals("Hits count must remain 1 (no double increment)", 1, engine.hits)

        // Third press on the exact same tick MUST also be ignored
        val thirdPress = engine.registerVisualPress()
        assertNull("Subsequent taps on the same stimulus must be ignored", thirdPress)
        assertEquals("Hits count must remain 1", 1, engine.hits)
    }

    @Test
    fun testDoublePressPrevention_resetsOnNextStimulusTick() {
        val visualSequence = intArrayOf(1, 2, 1, 2, 1) // index 2 matches 0, index 3 matches 1
        val engine = NBackRoundEngine(
            visualSequence = visualSequence,
            audioSequence = null,
            nBack = 2
        )
        engine.start()

        engine.advanceTick() // 0
        engine.advanceTick() // 1
        engine.advanceTick() // 2 (match with 0)
        assertTrue(engine.registerVisualPress() == true)
        assertNull(engine.registerVisualPress()) // blocked on tick 2

        // Advance to tick 3 (match with 1)
        val event3 = engine.advanceTick()
        assertTrue(event3!!.isVisualMatch)

        // New tick: user should now be able to press again!
        val pressResultTick3 = engine.registerVisualPress()
        assertEquals("Press on newly advanced tick must be accepted", true, pressResultTick3)
        assertEquals(2, engine.hits)
    }

    // =========================================================================
    // Component 5: Scoring Formula (Defended Weights & Non-Negative Floor)
    // =========================================================================

    @Test
    fun testScoringFormula_standardCalculation() {
        // Score = (hits * 10) - (falseAlarms * 5) - (misses * 2)
        val scoreResult = ScoreResult(
            hits = 4,        // 4 * 10 = 40
            falseAlarms = 2, // 2 * 5  = 10
            misses = 1,      // 1 * 2  = 2
            totalMatches = 5
        )
        // Expected: 40 - 10 - 2 = 28
        assertEquals(28, scoreResult.finalScore)
    }

    @Test
    fun testScoringFormula_preventsNegativeScore() {
        // When penalties exceed rewards, the score must be floored at 0
        val terriblePerformance = ScoreResult(
            hits = 0,
            falseAlarms = 10, // -50
            misses = 5,       // -10
            totalMatches = 5
        )
        // Without coerceAtLeast(0), this would be -60
        assertEquals("Score must be bounded at 0 and never drop below zero", 0, terriblePerformance.finalScore)
    }

    @Test
    fun testRoundEngine_fullRoundScoringEndToEnd() {
        // Sequence: [1, 2, 1, 2] with nBack = 2
        // index 0: 1
        // index 1: 2
        // index 2: 1 (Match with 0) -> user will Hit
        // index 3: 2 (Match with 1) -> user will Miss (does not press)
        val engine = NBackRoundEngine(
            visualSequence = intArrayOf(1, 2, 1, 2),
            audioSequence = null,
            nBack = 2
        )
        engine.start()

        engine.advanceTick() // 0
        engine.advanceTick() // 1
        engine.advanceTick() // 2 -> hit
        engine.registerVisualPress()
        engine.advanceTick() // 3 -> miss
        // No press on index 3

        val result = engine.finish()
        assertEquals("Expected 1 hit", 1, result.hits)
        assertEquals("Expected 1 miss", 1, result.misses)
        assertEquals("Expected 0 false alarms", 0, result.falseAlarms)
        assertEquals("Expected total matches = 2", 2, result.totalMatches)
        // Score = 1 * 10 - 1 * 2 = 8
        assertEquals(8, result.finalScore)
    }
}
