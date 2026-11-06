# Course Context & Constraints

## Strict Dependencies
- **No External Libraries:** Do not use external libraries beyond the standard Android platform SDK, Kotlin standard library, and official Jetpack libraries (Compose, Room, DataStore, ViewModel, Coroutines).
- **Persistence:** 
  - Settings must be stored using `DataStore<Preferences>`.
  - Scores must be stored using a `Room` database with a `@Query` that returns sorted results.

## AI Usage Rules
- All AI-generated code will be subject to an oral examination. The developer must be able to explain *every single line of code*.
- AI should be used as a pair-programmer, but the final architectural assembly and logic verification rests on the developer.
- Do not generate code that obscures the core logic (e.g., overly clever one-liners that are hard to explain).

## File Structure Preferences
- While Android projects naturally require multiple files, keep related logic cohesive and avoid unnecessary file fragmentation where a single well-structured file for a specific feature module would suffice (adhering to the "single-file preferred" hint from the lab brief where practical for domain logic).

## Unit Testing Mandate
Unit tests must be written and runnable on the submitted code. You must test at least **three** of the following core logic components:
1. Sequence generation.
2. Match-rate enforcement (~20-40%).
3. First-`n` guarantee (first `n` events cannot be matches).
4. Double-press prevention (registering a second tap before the next stimulus arrives).
5. Scoring formula (including preventing a negative score).