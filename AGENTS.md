# AGENTS.md — ERP Android Project

This file gives future OpenCode sessions the minimal context needed to avoid mistakes and ramp up quickly. **Every line below answers: "Would an agent likely miss this without help?" If not, it's left out.**

## Build & Test Commands

- **Unit tests**: `./gradlew testDebugUnitTest` — runs local JVM-tests (host). These are the `*Test*` classes under `app/src/test/`.
- **Instrumented tests**: `./gradlew connectedDebugAndroidTest` — runs on an AVD/device. These are the `*InstrumentedTest*` classes under `app/src/androidTest/`.
- **Single test by type**: Use the task names above; there is no per-test filter syntax beyond Gradle's test task filtering.

*Why this matters*: An agent might default to `./gradlew test` which runs ALL tests including slow instrumented ones, or try `./gradlew testDebugTest` which may not exist. The split above matches the project's source-set boundaries.

## Compose & Theming

- **UI is Compose (Material3)**: All screens use `androidx.compose.ui:compose-ui` and `androidx.compose.material3:material3`.
- **Theme is in `app/src/main/java/com/example/erp/ui/theme/Theme.kt`**: `ERPTheme()` accepts `darkTheme` and `dynamicColor` params. Theming logic:
  - If `dynamicColor && SDK >= S`: uses `dynamicDarkColorScheme` / `dynamicLightColorScheme` from system settings.
  - Else if `darkTheme`: uses the hard-coded `DarkColorScheme` (Purple80/PurpleGrey80/Pink80).
  - Else: uses `LightColorScheme` (Purple40/PurpleGrey40/Pink40).
- **Color definitions** are in `app/src/main/java/com/example/erp/ui/theme/Color.kt` (Purple80, PurpleGrey80, Pink80, Purple40, PurpleGrey40, Pink40 as ARGB ints).
- **Typography** is in `app/src/main/java/com/example/erp/ui/theme/Type.kt` — only `bodyLarge` is defined; do not add new text styles without also updating `Theme.kt` if you want them exposed through `MaterialTheme.typography`.
- **Previews**: Composables with `@Preview` annotation live in their source files (e.g., `MainActivity.kt`). Do not move a composable without its preview, or the build will break.

*Why this matters*: An agent could break the dark/light/dynamic theming chain by misreading the `when` block in `ERPTheme()` or by adding a typography style that isn't wired up.

## Project Structure

- **Package**: `com.example.erp` — all source sets (main, test, androidTest) share this root.
- **Source sets**:
  - `app/src/main/java/com/example/erp/` — application code + `MainActivity.kt` + `ui/theme/`
  - `app/src/test/java/com/example/erp/` — local unit tests (`ExampleUnitTest.kt`)
  - `app/src/androidTest/java/com/example/erp/` — instrumented tests (`ExampleInstrumentedTest.kt`)
  - `app/src/main/res/values/` — XML strings, colors, themes
- **Keeping rules**: `app/src/main/keepRules/rules.keep` — R8 keep rules. Currently empty (only comments). If you add Java native interop or WebView JS interfaces, add keep rules here.
- **Git ignore** (`.gitignore`): Excludes `*.iml`, `.gradle`, `/local.properties`, `/.idea/`, `.DS_Store`, `/build`, `/captures`, `.externalNativeBuild`, `.cxx`. **Do not check** `local.properties` into VCS; it is SDK-path local-only.

*Why this matters*: An agent might write tests in the wrong source set, add keep rules in the wrong place, or commit `local.properties`.

## Commands You Might Guess Wrong

| What you might type | What to use instead |
|---|---|
| `./gradlew test` | `./gradlew testDebugUnitTest` (runs only local unit tests) |
| `./gradlew connectedAndroidTest` | `./gradlew connectedDebugAndroidTest` (the task name includes the build type) |
| `./gradlew assemble` | `./gradlew assembleDebug` (default target is `debug`) |
| Edit `local.properties` | Never — it is git-ignored and auto-generated |

## Known Quirks

- **No CI workflows** are checked in (`.github/` is absent). There is no PR template, no automated test pipeline defined in repo.
- **No OpenCode config** (`opencode.json`, `.cursorrules`, etc.) lives in this repo. All instructions come from this `AGENTS.md` file.
- **Strict TDD mode**: Not configured. Tests are basic JUnit examples; no test infrastructure beyond the stubs.
- **Dynamic color** requires Android 12+ (SDK S). On devices < S, the fallback light/dark color schemes apply.

## If You Need to Investigate Further

1. Read `README*` / root manifests first — this project has a `settings.gradle.kts` that includes `:app` only.
2. Build/typecheck/lint/format config lives in `build.gradle.kts` files and standard Gradle/Android plugins.
3. If architecture is still unclear, inspect the small set of Java/Kotlin files under `com/example/erp/` to find entrypoints and package boundaries.
4. Executable sources of truth (build scripts) override prose — trust `build.gradle.kts` over any docs that conflict.

*Last built*: From the environment, this project targets `compileSdk 37`, `minSdk 30`, `targetSdk 37`, with Kotlin `official` code style.