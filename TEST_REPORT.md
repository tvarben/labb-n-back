# Test Report: N-Back Mobile App (Lab B:1)

## Overview
This document details the automated unit testing strategy and test results for the Native Dual N-Back mobile application implemented in Kotlin. All unit tests run on the JVM without requiring an Android emulator or device, ensuring rapid, reliable verification of core domain logic.

Test source file: `app/src/test/java/mobappdev/example/nback_cimpl/NBackEngineTest.kt`

---

## 1. Tested Components & Mandate Coverage

The course specification mandates testing at least **three** of the five core logic components. Our suite tests **all five** components:

| # | Component | Requirement | Status |
|---|---|---|---|
| 1 | **Sequence Generation** | Generates valid 1-based indices within dimensions and subsets from phonetically distinct letters. | ✅ Passed |
| 2 | **Match-Rate Enforcement** | Enforces match frequency strictly within the 20%–40% range across multiple seeds. | ✅ Passed |
| 3 | **First-`n` Guarantee** | The first $n$ events of any round can *never* be matches; button taps during this window are ignored. | ✅ Passed |
| 4 | **Double-Press Prevention** | Registering a second tap before the next stimulus arrives is ignored; resets on new tick. | ✅ Passed |
| 5 | **Scoring Formula** | Defensible weighted scoring (`hits * 10 - falseAlarms * 5 - misses * 2`) strictly floored at 0. | ✅ Passed |

---

## 2. Test Cases and Assertions

### Component 1: Sequence Generation
- **`testSequenceGeneration_validDimensionsAndRanges`**
  - *Assertion:* Checks that the output sequence length strictly equals the requested size ($N = 20$), and that every element falls within the range $[1, \text{combinations}]$ ($1 \le x \le 9$).
- **`testAudioLetterSequenceGeneration_usesPhoneticSet`**
  - *Assertion:* Verifies that all generated auditory stimulus characters belong to the phonetically distinct letter set (`C`, `H`, `K`, `L`, `Q`, `R`, `S`, `T`).

### Component 2: Match-Rate Enforcement (~20%–40%)
- **`testMatchRateEnforcement_strictlyWithinTargetRange`**
  - *Method:* Evaluates 50 independently seeded sequences of length 100 ($n = 2$, 9 combinations, 30% target).
  - *Assertion:* For all 50 seeds, the calculated match percentage $\frac{\text{actualMatches}}{\text{size} - n} \times 100$ must fall within $[20.0\%, 40.0\%]$.

### Component 3: First-`n` Guarantee
- **`testFirstNGuarantee_firstNEventsCanNeverBeMatches`**
  - *Method:* Simulates round execution for $n = 3$. Ticks through indices $0, 1, 2$.
  - *Assertion:* Asserts that `event.isVisualMatch == false` for all indices $i < n$. Asserts that calling `registerVisualPress()` returns `null` (ignored).

### Component 4: Double-Press Prevention
- **`testDoublePressPrevention_subsequentPressesOnSameTickIgnored`**
  - *Method:* Feeds a deterministic sequence `[1, 2, 1, 4, 1]` with $n = 2$ and advances to index 2 (a match with index 0).
  - *Assertion:* 
    1. First press returns `true` (Hit) $\rightarrow$ `hits` becomes 1.
    2. Second press on the exact same tick returns `null` (Ignored) $\rightarrow$ `hits` remains 1.
    3. Third press returns `null` $\rightarrow$ `hits` remains 1.
- **`testDoublePressPrevention_resetsOnNextStimulusTick`**
  - *Method:* Presses on tick 2, verifies second press is blocked, advances to tick 3, and verifies that pressing on tick 3 is now accepted again.

### Component 5: Scoring Formula & Non-Negative Floor
- **`testScoringFormula_standardCalculation`**
  - *Weights:* $\text{Score} = (\text{Hits} \times 10) - (\text{FalseAlarms} \times 5) - (\text{Misses} \times 2)$.
  - *Assertion:* For 4 hits, 2 false alarms, and 1 miss: $(4 \times 10) - (2 \times 5) - (1 \times 2) = 40 - 10 - 2 = 28$.
- **`testScoringFormula_preventsNegativeScore`**
  - *Assertion:* For 0 hits, 10 false alarms, and 5 misses ($0 - 50 - 10 = -60$), asserts that the final score is strictly floored at `0` via `coerceAtLeast(0)`.
- **`testRoundEngine_fullRoundScoringEndToEnd`**
  - *Method:* Full simulation of a 4-event sequence (`[1, 2, 1, 2]`, $n = 2$). User hits on tick 2, but leaves tick 3 unpressed (miss).
  - *Assertion:* Hits = 1, Misses = 1, False Alarms = 0, Total Matches = 2, Final Score = 8.

---

## 3. Test Execution Summary

Command executed:
```powershell
./gradlew testDebugUnitTest
```

Results:
```
BUILD SUCCESSFUL
Tasks executed: :app:testDebugUnitTest
Test Results: 9 tests completed, 9 passed, 0 failed, 0 skipped.
```
All automated unit tests passed cleanly with 100% success rate.
