# Design: Theme Persistence and UX Improvements

## Technical Approach

Introduce DataStore-backed ThemeRepository for theme persistence, extend DolarViewModel with theme state/callbacks, replace DropdownMenu with ThemeBottomSheet, and upgrade DolarUiState.error to sealed class hierarchy for network/API/parse error distinction.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|----------|--------|--------------|-----------|
| Persistence | DataStore Preferences | SharedPreferences | Modern API, Flow support, type-safe, coroutines-native |
| Repository pattern | Interface + DataStore impl | Direct DataStore in ViewModel | Consistency with ApiDolarRepository pattern, testability |
| Error modeling | Sealed interface Error | String? / enum class | Extensible, type-safe when, matches Kotlin best practices |
| Theme selector | ModalBottomSheet | DropdownMenu / Dialog | Better UX: color previews, scrollable, Material3 standard |
| Dynamic Color default | true on Android 12+ | false / user choice | Material You design intent; user can opt out via toggle |

## Data Flow

```
User taps theme icon
       │
       ▼
ThemeBottomSheet (Composable)
       │
       ├─ onThemeChange ───► DolarViewModel.setTheme() ──► ThemeRepository.setTheme() ──► DataStore
       ├─ onModeChange ───► DolarViewModel.setMode() ───► ThemeRepository.setMode() ───► DataStore
       └─ onDynamicChange ► DolarViewModel.setDynamic() ──► ThemeRepository.setDynamic() ──► DataStore
       │
       ▼
MainActivity (LaunchedEffect)
       │
       ├─ ThemeRepository.theme.collect() ──────► ERPTheme(theme=...)
       ├─ ThemeRepository.themeMode.collect() ──► ERPTheme(darkTheme=...)
       └─ ThemeRepository.dynamicColor.collect() ► ERPTheme(dynamicColor=...)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `build.gradle.kts` | Modify | Add DataStore dependency |
| `data/ThemePreferences.kt` | Create | DataStore schema + serialization |
| `data/ThemeRepository.kt` | Create | Interface + DataStoreThemeRepository impl |
| `data/DolarUiState.kt` | Create | Sealed Error hierarchy (or add to existing) |
| `ui/DolarViewModel.kt` | Modify | ThemeRepository dependency, theme state, callbacks |
| `ui/DolarScreen.kt` | Modify | Replace DropdownMenu with ThemeBottomSheet |
| `ui/ThemeBottomSheet.kt` | Create | New composable with previews, mode, dynamic toggle |
| `MainActivity.kt` | Modify | Load prefs, apply to ERPTheme, wire callbacks |

## Interfaces / Contracts

```kotlin
// data/ThemeRepository.kt
enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface ThemeRepository {
    val theme: Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    val dynamicColorEnabled: Flow<Boolean>
    suspend fun setDynamicColorEnabled(enabled: Boolean)
}

// data/DolarUiState.kt (new sealed error hierarchy)
sealed interface Error {
    data class NetworkError(val message: String) : Error
    data class ApiError(val source: String, val code: Int, val message: String) : Error
    data class ParseError(val field: String, val message: String) : Error
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | ThemePreferences read/write | Test DataStore with in-memory context |
| Unit | ThemeRepository flows emit correct values | Turbine or collectAsState in test coroutine |
| Unit | DolarViewModel error mapping | Fake repository, verify error types |
| Integration | Theme persists across process death | Launch app, change theme, kill process, relaunch |
| UI | ThemeBottomSheet shows active theme | Compose test with semantics tree |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required (fresh DataStore). Defaults: `DOLAR_VERDE`, `ThemeMode.SYSTEM`, `dynamicColorEnabled=true`.

## Open Questions

None — design complete.