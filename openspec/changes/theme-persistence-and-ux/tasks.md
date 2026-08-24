# Tasks: Theme Persistence and UX Improvements

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350–420 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | DataStore + repo + errors | PR 1 | `./gradlew testDebugUnitTest --tests "*ThemePreferences*"` | N/A (no runtime boundary) | `build.gradle.kts`, `ThemePreferences.kt`, `ThemeRepository.kt`, `DolarUiState.kt` |
| 2 | ViewModel + BottomSheet + wiring | PR 2 | `./gradlew connectedDebugAndroidTest --tests "*ThemeBottomSheet*"` | Launch app, change theme, kill process, relaunch | `DolarViewModel.kt`, `ThemeBottomSheet.kt`, `MainActivity.kt`, `DolarScreen.kt` |
| 3 | Tests + polish | PR 3 | `./gradlew testDebugUnitTest connectedDebugAndroidTest` | Full flow verification | Test files only |

## Phase 1: Foundation — DataStore, Preferences, Repository

- [ ] 1.1 Add `androidx.datastore:datastore-preferences:1.1.5` to `app/build.gradle.kts`
- [ ] 1.2 Create `data/ThemePreferences.kt` with DataStore schema (keys: selected_theme, theme_mode, dynamic_color_enabled)
- [ ] 1.3 Create `data/ThemeRepository.kt` with interface + `DataStoreThemeRepository` implementation
- [ ] 1.4 Create `data/DolarUiState.kt` with sealed `Error` hierarchy (NetworkError, ApiError, ParseError)

## Phase 2: Core Implementation — ViewModel, BottomSheet

- [ ] 2.1 Modify `ui/DolarViewModel.kt`: inject ThemeRepository, expose theme/mode/dynamic flows, add callbacks (setTheme, setMode, setDynamicColor)
- [ ] 2.2 Create `ui/ThemeBottomSheet.kt`: ModalBottomSheet with 5 theme chips (color preview + checkmark), mode radio (System/Light/Dark), dynamic color toggle (Android 12+)
- [ ] 2.3 Update `ui/DolarViewModel.kt` getQuotes() to populate new error types (NetworkError, ApiError, ParseError)
- [ ] 2.4 Update `ui/DolarViewModel.kt` getQuotes() USDT failure → log only, keep BCV quotes (REQ-EH-002)

## Phase 3: Integration — MainActivity, DolarScreen

- [ ] 3.1 Modify `MainActivity.kt`: load ThemeRepository flows, pass to ERPTheme, wire callbacks to ViewModel
- [ ] 3.2 Modify `ui/DolarScreen.kt`: replace DropdownMenu with ThemeBottomSheet (icon Palette), pass callbacks
- [ ] 3.3 Verify dynamicColor default true on Android 12+, toggle disables it

## Phase 4: Testing — RED → GREEN

- [ ] 4.1 RED: Unit test `ThemePreferences` read/write defaults + persistence
- [ ] 4.2 RED: Unit test `ThemeRepository` flows emit correct values, updates persist
- [ ] 4.3 RED: Unit test `DolarViewModel` error mapping (network/API/parse → sealed classes)
- [ ] 4.4 GREEN: Implement fixes for failing tests
- [ ] 4.5 Integration test: launch app → change theme → kill process → relaunch → theme restored

## Phase 5: Cleanup / Documentation

- [ ] 5.1 Update `DolarScreenPreview` previews to test multiple themes
- [ ] 5.2 Remove any temporary/unused code
- [ ] 5.3 Verify all existing tests still pass