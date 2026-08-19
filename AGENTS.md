# AGENTS.md — ERP Android Project

This file gives future OpenCode sessions the minimal context needed to avoid mistakes and ramp up quickly. **Every line below answers: "Would an agent likely miss this without help?" If not, it's left out.**

## Build & Test Commands

- **Unit tests**: `./gradlew testDebugUnitTest` — runs local JVM-tests (host). These are the `*Test*` classes under `app/src/test/`.
- **Instrumented tests**: `./gradlew connectedDebugAndroidTest` — runs on an AVD/device. These are the `*InstrumentedTest*` classes under `app/src/androidTest/`.
- **Single test by type**: Use the task names above; there is no per-test filter syntax beyond Gradle's test task filtering.
- **Build APK**: `./gradlew assembleDebug` (default target is `debug`; the generic `assemble` also builds release).

*Why this matters*: An agent might default to `./gradlew test` which runs unit tests for ALL build variants (slower, unrelated failures), or try `./gradlew testDebugTest` / `./gradlew connectedAndroidTest` which do not exist. The split above matches the project's source-set boundaries.

## Architecture (MVVM + Repository)

- **Pattern**: UI → ViewModel → Repository → remote APIs. There is **no DI framework**; `DolarViewModel` defaults to `ApiDolarRepository()` directly.
- **Data flow**: `DolarScreen` observes `DolarViewModel.uiState: StateFlow<DolarUiState>`; the ViewModel calls `DolarRepository.getQuotes()` / `getHistorial()`; the repository parses JSON with `org.json` (no Retrofit, no kotlinx.serialization).
- **Sources**:
  - `ApiDolarRepository.fetchBcv()` — GET `https://rates.dolarvzla.com/bcv/current.json`, returns Dólar (usd) + Euro (eur) quotes with `anterior` and `variacion`.
  - `ApiDolarRepository.fetchUsdt()` — POST `https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search` with a fixed JSON body and a browser-like `User-Agent` header (Binance blocks default OkHttp UA). Returns the average of the first page of BUY offers.
  - **USDT failure is tolerated**: `getQuotes()` catches USDT errors and returns only BCV quotes; BCV failure throws and surfaces the UI error state.
- **History gotcha**: `getHistorial()` returns real "Ayer/Hoy" points only when `quote.anterior != null` (BCV). For USDT (`anterior == null`) it calls `DolarSimulation.historial()` which generates a fake series with `sin()` waves — do not treat it as real data.
- **UI state**: `DolarUiState` holds `quotes`, `selectedFuente`, `historial`, `loading`, `error`. Mutations go through `MutableStateFlow.update`; the ViewModel uses `runCatching` and `Dispatchers.IO` is inside the repository.

*Why this matters*: An agent adding a new source/endpoint must follow the same pattern (add to `ApiDolarRepository`, extend `DolarQuote`, parse with `JSONObject`), keep the tolerant-failure behavior, and not mistake simulated history for API data.

## Compose & Theming

- **UI is Compose (Material3)**: All screens use `androidx.compose.ui:compose-ui` and `androidx.compose.material3:material3`.
- **Theme is in `app/src/main/java/com/example/erp/ui/theme/Theme.kt`**: `ERPTheme(darkTheme, dynamicColor, theme)` where `theme: AppTheme` selects one of **4 palettes** (`DOLAR_VERDE`, `AZUL_BANCARIO`, `VIOLETA_ELEGANTE`, `ALTO_CONTRASTE`). Theming logic:
  - If `dynamicColor && SDK >= S`: uses `dynamicDarkColorScheme` / `dynamicLightColorScheme` from system settings — **this overrides the selected `AppTheme` palette on Android 12+**, which is intended behavior, not a bug.
  - Else if `darkTheme`: uses the dark palette of the selected `AppTheme`.
  - Else: uses the light palette of the selected `AppTheme`.
- **Color definitions** are in `app/src/main/java/com/example/erp/ui/theme/Color.kt` (palette ARGB ints + up/down signal colors for the quote cards).
- **Typography** is in `app/src/main/java/com/example/erp/ui/theme/Type.kt` — defines **6 styles**: `bodyLarge`, `displayLarge`, `headlineMedium`, `labelMedium`, `titleLarge`, `labelSmall`. When adding a new text style, update `Type.kt`; it is exposed through `MaterialTheme.typography` automatically.
- **Previews**: Composables with `@Preview` annotation live in their source files (e.g., `DolarScreen.kt`). Do not move a composable without its preview, or the build will break.
- **Theme selector lives in the UI**: `MainActivity` holds `currentTheme` state and passes `onThemeChange` into `DolarScreen`.

*Why this matters*: An agent could "fix" the dynamic-color override on Android 12+ and break the intended behavior, or add a typography style without wiring it up.

## Project Structure

- **Package**: `com.example.erp` — all source sets (main, test, androidTest) share this root.
- **Source sets**:
  - `app/src/main/java/com/example/erp/` — application code:
    - `MainActivity.kt` — entry point, `setContent`, theme state
    - `data/` — `DolarData.kt` (models + `DolarRepository` interface + `DolarSimulation`), `ApiDolarRepository.kt` (OkHttp + parsing)
    - `ui/` — `DolarViewModel.kt` (state), `DolarScreen.kt` (main screen + preview), `components/EvolutionChart.kt` (Canvas chart), `theme/` (Theme, Color, Type)
  - `app/src/test/java/com/example/erp/` — local unit tests (`ExampleUnitTest.kt`)
  - `app/src/androidTest/java/com/example/erp/` — instrumented tests (`ExampleInstrumentedTest.kt`)
  - `app/src/main/res/values/` — XML strings, colors, themes. Only `app_name` is in `strings.xml`; most UI text is hardcoded Spanish inside composables.
- **Keeping rules**: `app/src/main/keepRules/rules.keep` — R8 keep rules. Currently empty (only comments). If you add Java native interop or WebView JS interfaces, add keep rules here.
- **Git ignore** (`.gitignore`): Excludes `*.iml`, `.gradle`, `/local.properties`, `/.idea/`, `.DS_Store`, `/build`, `/captures`, `.externalNativeBuild`, `.cxx`. **Do not check** `local.properties` into VCS; it is SDK-path local-only.

*Why this matters*: An agent might write tests in the wrong source set, add keep rules in the wrong place, or commit `local.properties`.

## Commands You Might Guess Wrong

| What you might type | What to use instead |
|---|---|
| `./gradlew test` | `./gradlew testDebugUnitTest` (runs only local unit tests for the debug variant) |
| `./gradlew connectedAndroidTest` | `./gradlew connectedDebugAndroidTest` (the task name includes the build type) |
| `./gradlew assemble` | `./gradlew assembleDebug` (default target is `debug`) |
| Edit `local.properties` | Never — it is git-ignored and auto-generated |
| `git push origin master` | The remote is named `github` — use `git push github master` |

## Known Quirks

- **No CI workflows** are checked in (`.github/` is absent). There is no PR template, no automated test pipeline defined in repo.
- **No OpenCode config** (`opencode.json`, `.cursorrules`, etc.) lives in this repo. All instructions come from this `AGENTS.md` file.
- **Strict TDD mode**: Not configured. Tests are basic JUnit examples; no test infrastructure beyond the stubs.
- **Dynamic color** requires Android 12+ (SDK S). On devices < S, the fallback light/dark color schemes apply.
- **Dependencies**: OkHttp 4.12.0 for networking; `org.json` (built into Android) for parsing; no serialization library, no DI, no Room/DataStore yet.
- **Versions**: AGP 9.3.1, Kotlin 2.2.10, Gradle 9.5.0, Compose BOM 2026.02.01, `minSdk 30` / `compileSdk 37` / `targetSdk 37`, daemon toolchain JDK 25 (auto-provisioned via foojay).

## If You Need to Investigate Further

1. Read `README*` / root manifests first — this project has a `settings.gradle.kts` that includes `:app` only. The public docs are `README.md` (project overview) and `docs/guia-desarrollo.md` (development guide, in Spanish).
2. Build/typecheck/lint/format config lives in `build.gradle.kts` files and standard Gradle/Android plugins.
3. If architecture is still unclear, inspect the small set of Java/Kotlin files under `com/example/erp/` to find entrypoints and package boundaries.
4. Executable sources of truth (build scripts) override prose — trust `build.gradle.kts` over any docs that conflict.

*Last built*: From the environment, this project targets `compileSdk 37`, `minSdk 30`, `targetSdk 37`, with Kotlin `official` code style.