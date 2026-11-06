# Architecture and Code Style Guide

## Architecture Pattern
- **Pattern:** MVVM (Model-View-ViewModel) is strictly required. MVC is acceptable, but MVVM is the target for Jetpack Compose.
- **Anti-Pattern:** A single "god-class" is strictly forbidden. Logic must be separated appropriately.
- **Separation of Concerns:** Unit-testable sequence and scoring logic MUST be separated from the Android UI/Compose layer. UI should only observe state and emit intents/events.

## State Management
- Use `StateFlow` (or Compose's `State`) within ViewModels to expose observable state to the Compose UI.
- ViewModels should not handle direct navigation; they should expose navigation events for the UI layer to handle.

## Naming Conventions
- Class, method, and field names must clearly and accurately describe their purpose (e.g., `generateNBackSequence`, `MatchEnforcementGuard`, `onMatchButtonPressed`).
- Avoid generic names like `Manager`, `Helper`, or `Data`.

## Documentation
- Code must be thoroughly documented.
- Add KDoc comments to all major classes, view models, and complex algorithmic functions (especially the sequence generation and scoring formulas) so another developer (or an examiner) can easily understand the flow.

## Concurrency
- Use Kotlin Coroutines and appropriate Dispatchers (e.g., `Dispatchers.Default` for sequence calculation, `Dispatchers.Main` for UI updates) to ensure no heavy lifting is done on the main thread.