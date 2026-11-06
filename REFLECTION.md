# Reflection: Pair-Programming with AI (Lab B:1)

## One Thing That Surprised Me About Working with AI
The most surprising aspect of working with an AI coding partner was how critical clear contextual boundaries and constraint files (`SPEC.md`, `style_guide.md`, `context_constraints.md`) are to the quality of the generated code. 

When provided with strict architectural guidelines (such as mandating pure Kotlin separation for unit tests instead of relying on Android framework classes or JNI), the AI did not just generate code—it structured the architecture in an idiomatic MVVM pattern that made writing unit tests straightforward. 

However, another surprise was how subtle build toolchain errors can be when using AI. The LLM accidentally generated two companion objects in a single Kotlin file. Instead of receiving a clear Kotlin compiler message, the error cascaded down into Gradle's annotation processing (`kapt`) and reported a cryptic `duplicate class` error during stub generation. Diagnosing this highlighted that while AI excels at writing feature logic, a deep understanding of the language semantics and build tools is indispensable for the developer.

---

## One Thing I Would Do Differently Next Time
Next time, I would write the automated unit tests *before* asking the AI to generate any UI or ViewModel code (adopting strict Test-Driven Development). 

In this lab, having unit tests for sequence generation, match rate enforcement, first-$n$ immunity, double-press prevention, and scoring gave immediate, objective confidence that the core game rules were 100% correct. If I had defined the test suite first, the domain contracts (`NBackSequenceGenerator` and `NBackRoundEngine`) would have been locked in place earlier, eliminating redundant refactoring between the ViewModel and domain layers. 

Additionally, I would configure and verify the Android Gradle Plugin, JDK toolchain, and Room dependencies before generating any screens, ensuring that compiler and annotation-processing tools are completely verified before expanding UI composables.
