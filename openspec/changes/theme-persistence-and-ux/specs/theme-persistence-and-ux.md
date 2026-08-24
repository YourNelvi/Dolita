# Delta Specs — theme-persistence-and-ux

---

## [NEW] theme-persistence

### REQ-TP-001: Persist selected theme across restarts
**Given** user selects a theme from the selector  
**When** app process restarts  
**Then** the previously selected theme MUST be restored automatically  
**And** no default fallback SHALL occur unless stored value is invalid

**Given** app launches for the first time (no stored preference)  
**When** ThemeRepository initializes  
**Then** DOLAR_VERDE SHALL be used as default  
**And** preference MUST be persisted for future launches

### REQ-TP-002: ThemeRepository abstraction
**Given** ThemeRepository interface exists  
**When** ViewModel requests current theme  
**Then** repository SHALL return Flow<AppTheme> emitting stored value  
**And** repository SHALL expose suspend function to update theme

**Given** DataStore migration from legacy SharedPreferences (if any)  
**When** ThemeDataStore initializes  
**Then** existing preference SHALL be migrated transparently  
**And** no user-visible reset SHALL occur

---

## [NEW] dynamic-color-control

### REQ-DC-001: Dynamic Color enabled by default on Android 12+
**Given** device runs Android 12+ (API 31+)  
**When** ERPTheme composable renders  
**Then** dynamicColor parameter MUST default to true  
**And** system wallpaper-based palette SHALL be used automatically

**Given** device runs Android < 12  
**When** ERPTheme composable renders  
**Then** dynamicColor MUST be ignored  
**And** selected AppTheme light/dark palette SHALL apply

### REQ-DC-002: User can override Dynamic Color
**Given** user opens theme selector on Android 12+  
**When** user toggles "Use Dynamic Color" OFF  
**Then** selected AppTheme palette SHALL apply immediately  
**And** preference MUST persist across restarts

**Given** user toggles "Use Dynamic Color" ON  
**When** Dynamic Color was previously disabled  
**Then** system palette SHALL apply immediately  
**And** AppTheme selection SHALL be ignored while Dynamic Color is active

---

## [DELTA] error-handling (extends data-fetching)

### REQ-EH-001: Distinguish error types in UI state
**Given** DolarViewModel fetches quotes  
**When** network failure occurs (timeout, no connectivity)  
**Then** DolarUiState.error MUST be NetworkError with user-facing message  
**And** retry action SHALL be offered in UI

**Given** API returns non-2xx or malformed JSON  
**When** parsing fails or BCV endpoint errors  
**Then** DolarUiState.error MUST be ApiError with status/code  
**And** error message SHALL indicate source (BCV / Binance)

**Given** JSON parsing succeeds but required fields missing  
**When** DolarQuote construction fails  
**Then** DolarUiState.error MUST be ParseError with field name  
**And** partial data SHALL NOT be shown

### REQ-EH-002: Graceful degradation for USDT
**Given** BCV fetch succeeds but USDT fetch fails  
**When** getQuotes() completes  
**Then** quotes list MUST contain BCV quotes only  
**And** error SHALL be logged but NOT surfaced to UI

---

## [DELTA] theme-selector-ux (extends ui-theme)

### REQ-TS-001: BottomSheet with color previews
**Given** user taps theme selector in TopAppBar  
**When** BottomSheet opens  
**Then** all 5 AppTheme options MUST show colored preview swatches  
**And** active theme SHALL display checkmark indicator

**Given** Dynamic Color is available (Android 12+)  
**When** BottomSheet renders  
**Then** "Dynamic Color" toggle MUST appear at top  
**And** when enabled, AppTheme list SHALL be disabled/grayed out

### REQ-TS-002: Light / Dark / System mode toggle
**Given** user opens theme selector  
**When** mode selector (Light / Dark / System) is visible  
**Then** current mode SHALL be indicated  
**And** selection MUST persist via ThemeRepository

**Given** user selects "System" mode  
**When** system theme changes (user toggles OS dark mode)  
**Then** app theme SHALL follow system automatically  
**And** no app restart SHALL be required