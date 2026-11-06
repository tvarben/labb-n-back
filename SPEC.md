# Project Specification: Dual N-Back Mobile App

## Platform
- **OS:** Android
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose

## Core Gameplay Mechanics
- **N-Back Task:** A continuous performance task measuring working memory.
- **Stimuli:** 
  - **Visual:** A $3\times3$ grid where one cell highlights per event.
  - **Auditory:** Spoken letters from a fixed, phonetically distinct set.
- **Sequence Logic:**
  - Match rate must be rigorously controlled to ~20-40%.
  - Guarantee: The first `n` events of any round can *never* be matches.
  - Logic must be custom-built, not delegated to an external library.

## Screens & UI Requirements
1. **Home Screen:**
   - Displays the current high score.
   - Controls to select stimulus type: Visual only, Auditory only, or Dual (both).
   - Displays current settings: value of `n`, event duration, and number of events per round.
   - Button to navigate to the Game screen and start a round.
2. **Game Screen:**
   - Displays the $3\times3$ grid (visual) and/or plays spoken letters (auditory).
   - Shows live game state: current event number and number of correct responses so far.
   - Includes a "Match" button to register an n-back match.
   - Provides non-offensive animated feedback (e.g., color or size change on the button) for incorrect presses.
3. **Settings Screen (Persistent):**
   - Controls to adjust: number of events per round, event duration, value of `n`, grid size, and number of distinct spoken letters.
4. **Score History Screen:**
   - Lists all saved scores sorted by name, score, and date.

## Game Lifecycle & Interaction Rules
- A match press is ignored before the round starts, after it ends, and during the first `n` events.
- Multiple match presses on the same event are ignored after the first.
- The round timer must be cancelled cleanly when the round ends or if the player leaves the screen mid-round.

## Android Specific Implementation Details
- **Audio:** Use Android's `TextToSpeech` API. Initialize in the ViewModel and release in `onCleared()`. Include a TTS fallback message if no voice data is available on the device.
- **Responsive Layout:** Must support portrait and landscape orientations, adapting correctly to both phone and tablet screen sizes. Keep the grid square.
- **Accessibility:** Ensure the match button meets minimum touch-target size requirements.