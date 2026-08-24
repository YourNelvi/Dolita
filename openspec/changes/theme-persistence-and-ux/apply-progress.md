# Apply Progress: theme-persistence-and-ux

## Phase 1: Foundation — DataStore, Preferences, Repository
- [ ] 1.1 Add DataStore dependency to build.gradle.kts
- [ ] 1.2 Create data/ThemePreferences.kt
- [ ] 1.3 Create data/ThemeRepository.kt
- [ ] 1.4 Create data/DolarUiState.kt with sealed Error

## Phase 2: Core Implementation — ViewModel, BottomSheet
- [ ] 2.1 Modify ui/DolarViewModel.kt with ThemeRepository
- [ ] 2.2 Create ui/ThemeBottomSheet.kt
- [ ] 2.3 Update DolarViewModel error mapping
- [ ] 2.4 Update USDT graceful degradation

## Phase 3: Integration — MainActivity, DolarScreen
- [ ] 3.1 Modify MainActivity.kt with ThemeRepository flows
- [ ] 3.2 Modify DolarScreen.kt with ThemeBottomSheet
- [ ] 3.3 Verify dynamicColor default

## Phase 4: Testing
- [ ] 4.1 RED: ThemePreferences tests
- [ ] 4.2 RED: ThemeRepository tests
- [ ] 4.3 RED: DolarViewModel error mapping tests
- [ ] 4.4 GREEN: Implement fixes
- [ ] 4.5 Integration: theme persists after process death