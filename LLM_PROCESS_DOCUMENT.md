# LLM Process Document: Dual N-Back Mobile App (Lab B:1)

## 1. AI Tools & Environment
- **LLM Tool:** Antigravity AI Coding Assistant (powered by Google DeepMind's Gemini 3.8 Flash model).
- **Development Environment:** Android Studio (JBR 17 / Microsoft OpenJDK 17 LTS), Jetpack Compose, Gradle 8.0, Kotlin 1.8.10.
- **Context Files Provided to LLM:**
  - `SPEC.md`: Full functional specifications, screen flows, audio requirements, accessibility rules.
  - `style_guide.md`: Architectural guidelines (MVVM strictly enforced, separation of domain logic, StateFlow observable state, KDoc documentation).
  - `context_constraints.md`: Lab constraints (zero external libraries, DataStore for settings, Room with sorted `@Query` for scores, unit testing mandate).
  - `Lab B:1 Building a Native N-Back Mobile App (PDF)`: Core aim, 30 oral examination questions, basic and higher-level score criteria.

---

## 2. Representative Prompts & Outputs

### Prompt 1: Pure Kotlin Domain Engine & Unit Test Suite
- **Prompt:**
  > *"Create a pure-Kotlin unit-testable domain engine (`NBackEngine.kt`) separated from the Android framework. Implement sequence generation with a first-n guarantee (indices 0..<n can never be matches), match rate enforcement strictly within 20%–40%, phonetically distinct spoken letters, double-press prevention per stimulus tick, and non-negative scoring. Then write JUnit unit tests covering all 5 mandate components."*
- **What It Produced:**
  - `NBackSequenceGenerator` and `NBackRoundEngine` in `mobappdev.example.nback_cimpl.domain`.
  - `NBackEngineTest.kt` in `src/test/java`, containing 9 automated tests verifying sequence length, phonetic subset validity, match rate distribution across 50 random seeds, first-$n$ match immunity, double-tap rejection, and score flooring at 0.

### Prompt 2: Room Persistence & DataStore Preferences
- **Prompt:**
  > *"Implement persistent storage following course constraints without external libraries. Create `ScoreDatabase.kt` using AndroidX Room with an entity `ScoreRecord`, DAO with sorted `@Query` methods (by score descending, player name ascending, date descending), and expand `UserPreferencesRepository.kt` with DataStore Preferences to persist nBack, round event count, event duration, grid size, and letter set."*
- **What It Produced:**
  - `ScoreRecord` entity and `ScoreDao` with reactive `Flow<List<ScoreRecord>>` queries.
  - Room database singleton `AppDatabase`.
  - `UserPreferencesRepository` with full preference keys and flow mappings for `GameSettings`.

### Prompt 3: Responsive Compose UI with Animated Feedback & Accessibility
- **Prompt:**
  > *"Implement `GameScreen.kt`, `SettingsScreen.kt`, and `ScoreHistoryScreen.kt` in Jetpack Compose following MVVM. In `GameScreen`, ensure the visual grid stays square across portrait, landscape, phones, and tablets using `BoxWithConstraints` and `aspectRatio(1f)`. Provide non-offensive animated feedback on incorrect presses (smooth color pulse and subtle shake). Ensure match buttons exceed 48dp touch targets. Include TTS fallback messaging."*
- **What It Produced:**
  - Responsive layouts with separate adaptive orientations (`PortraitGameLayout` and `LandscapeGameLayout`).
  - Tactile non-offensive feedback using `animateColorAsState` and keyframe `Animatable` shake.
  - Full Compose navigation host in `MainActivity.kt`.

---

## 3. What Went Wrong & Corrections Made

During development, three technical issues arose that required intervention and correction:

1. **Stale Skeleton CXX Path from Course Author:**
   - *Issue:* The initial build failed on `:app:configureCMakeDebug` because the skeleton code contained committed cache files in `app/.cxx` referencing the original author's local file path (`/Users/yeetivity/Documents/2023/...`).
   - *Correction:* Purged `app/.cxx` and let Gradle re-configure CMake cleanly for the local environment.

2. **JVM Target / Toolchain Mismatch with kapt:**
   - *Issue:* When adding Room compiler (`kapt`), Gradle threw an error: `'compileDebugJavaWithJavac' task (current target is 1.8) and 'kaptGenerateStubsDebugKotlin' task (current target is 17) jvm target compatibility should be set to the same Java version.`
   - *Correction:* Updated `compileOptions` in `app/build.gradle.kts` to `JavaVersion.VERSION_17` and `kotlinOptions { jvmTarget = "17" }`, aligning javac and kapt on Java 17.

3. **Malformed Kotlin Code with Multiple Companion Objects:**
   - *Issue:* The LLM placed two separate `companion object` blocks inside `GameVM.kt` (one private for `TAG` at the top and one for `Factory` at the bottom). While syntactically prohibited in Kotlin, it manifested as `duplicate class: ScoreSortOrder` during kapt stub compilation.
   - *Correction:* Merged both blocks into a single cohesive `companion object` at the bottom of `GameVM`, resolving the kapt compilation error immediately.
