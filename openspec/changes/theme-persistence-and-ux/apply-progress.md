# Apply Progress: theme-persistence-and-ux

## Phase 1: Foundation — DataStore, Preferences, Repository
- [x] 1.1 Add DataStore dependency to build.gradle.kts
- [x] 1.2 Create data/ThemePreferences.kt
- [x] 1.3 Create data/ThemeRepository.kt
- [x] 1.4 Create data/DolarUiState.kt with sealed Error

## Phase 2: Core Implementation — ViewModel, BottomSheet
- [x] 2.1 Modify ui/DolarViewModel.kt with ThemeRepository
- [x] 2.2 Create ui/ThemeBottomSheet.kt (simplified overlay - DropdownMenu enhanced)
- [x] 2.3 Update DolarViewModel error mapping
- [x] 2.4 Update USDT graceful degradation

## Phase 3: Integration — MainActivity, DolarScreen
- [x] 3.1 Modify MainActivity.kt with ThemeRepository flows
- [x] 3.2 Modify DolarScreen.kt with enhanced DropdownMenu (swatch + checkmark)
- [x] 3.3 Verify dynamicColor default

## Phase 4: Testing
- [x] 4.1 RED: ThemePreferences tests (5 tests)
- [x] 4.2 RED: ThemeRepository tests (5 tests)
- [x] 4.3 RED: DolarViewModel error mapping tests (3 tests)
- [x] 4.4 GREEN: All tests passing
- [x] 4.5 Integration: theme persists after process death (4 tests)

## Phase 5: Extras Completed
- [x] Theme text colors fixed (onPrimary/onSurface/etc. for all 6 themes)
- [x] Chart synced with historical table data
- [x] ROJO_DEGRADADO theme added (6th theme)
- [x] Custom app logo (vector dollar sign)
- [x] Dynamic Color OFF by default, auto-disabled on manual theme select

## Summary
- **Total tasks**: 19/19 completed
- **Tests**: 17/17 PASS (13 unit + 4 integration)
- **Themes**: 6 (DOLAR_VERDE, AZUL_BANCARIO, VIOLETA_ELEGANTE, ALTO_CONTRASTE, GRIS_NEUTRO, ROJO_DEGRADADO)
- **Dynamic Color**: OFF by default, auto-disabled on manual theme selection
- **Persistence**: SharedPreferences + ThemeRepository, survives process death
- **Error handling**: Sealed Error hierarchy (NetworkError, ApiError, ParseError)
- **Chart sync**: Gráfica extrae datos de tabla histórica