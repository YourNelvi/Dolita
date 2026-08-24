# Proposal: Theme Persistence and UX Improvements

## Intent

Solve 4 high-priority UX gaps in Dolita app:
1. Theme resets to `DOLAR_VERDE` on restart (no persistence)
2. Dynamic Color hardcoded `false` in MainActivity (disables Material You on Android 12+)
3. No independent light/dark/system mode toggle
4. Theme dropdown lacks active indicator, color previews, and mode/dynamic-color controls

## Scope

### In Scope
- Theme persistence via DataStore (selected theme + mode + dynamic color preference)
- Dynamic Color enabled by default on Android 12+; user can opt out
- Error handling: distinguish NetworkError / ApiError / ParseError in UI
- Theme selector: BottomSheet with color previews, active indicator, mode toggle (System/Light/Dark), Dynamic Color toggle

### Out of Scope
- Per-theme light/dark overrides beyond global mode
- Theme import/export
- Animations/transitions between themes

## Capabilities

### New Capabilities
- `theme-persistence`: DataStore-backed theme preferences with ThemeRepository abstraction
- `dynamic-color-control`: User toggle for Dynamic Color with Android 12+ default-on behavior

### Modified Capabilities
- `error-handling`: Extend existing data-fetching to distinguish NetworkError, ApiError, ParseError
- `theme-selector-ux`: Extend existing ui-theme to replace DropdownMenu with BottomSheet + mode toggle

## Approach

1. Add `datastore-preferences` dependency
2. Create `ThemePreferences` (DataStore wrapper) + `ThemeRepository` interface/impl
3. Initialize `ERPTheme` params from repository in `MainActivity`
4. Extend `DolarUiState.error` to sealed class hierarchy
5. Build `ThemeBottomSheet` composable with preview chips, mode radios, dynamic toggle
6. Wire `onThemeChange` / `onModeChange` / `onDynamicColorChange` through ViewModel → Repository → DataStore

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `build.gradle.kts` | Modified | Add `androidx.datastore:datastore-preferences:1.1.5` |
| `data/ThemePreferences.kt` | New | DataStore schema + serialization |
| `data/ThemeRepository.kt` | New | Interface + `DataStoreThemeRepository` impl |
| `ui/theme/Theme.kt` | Reference | Used by new init flow |
| `MainActivity.kt` | Modified | Load persisted prefs, pass to `ERPTheme`, expose callbacks |
| `ui/DolarViewModel.kt` | Modified | Hold theme state, expose change callbacks |
| `ui/DolarScreen.kt` | Modified | Replace DropdownMenu with `ThemeBottomSheet` |
| `ui/theme/Color.kt` | Reference | Provide preview colors for theme chips |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| DataStore migration from no-persistence | Low | Defaults to `DOLAR_VERDE`, System mode, dynamicColor=true |
| Dynamic Color on Android 12+ conflicts with fixed theme | Medium | Toggle disables dynamic when fixed theme selected; repo enforces |
| BottomSheet UX regression on small screens | Low | Use `ModalBottomSheet` with scrollable content |

## Rollback Plan

1. Revert `build.gradle.kts` dependency
2. Delete `ThemePreferences.kt`, `ThemeRepository.kt`
3. Restore `MainActivity.kt` to hardcoded `dynamicColor=false`, `DOLAR_VERDE`
4. Restore `DolarScreen.kt` DropdownMenu
5. Revert `DolarUiState.error` to String

## Success Criteria

- [ ] Theme persists across process death / app restart
- [ ] Dynamic Color works on Android 12+ by default; toggle disables it
- [ ] Light/Dark/System mode respected independently of theme
- [ ] BottomSheet shows 5 theme previews with active indicator
- [ ] Error states show "Sin conexión" / "Error del servidor" / "Datos inválidos" distinctly
- [ ] All existing previews compile and render

## Dependencies

- `androidx.datastore:datastore-preferences:1.1.5`