# Theme Persistence Specification

## Purpose

DataStore-backed theme preferences with ThemeRepository abstraction for persisting user theme selections across app restarts.

## Requirements

### REQ-TP-001: Persist selected theme across restarts

The system MUST restore the previously selected theme automatically when the app process restarts.
No default fallback SHALL occur unless stored value is invalid.

**Scenario: Theme restored after restart**
- GIVEN user selected AZUL_BANCARIO theme
- WHEN app process restarts
- THEN AZUL_BANCARIO theme MUST be applied automatically
- AND no default fallback SHALL occur

**Scenario: First launch uses default**
- GIVEN app launches for the first time (no stored preference)
- WHEN ThemeRepository initializes
- THEN DOLAR_VERDE SHALL be used as default
- AND preference MUST be persisted for future launches

### REQ-TP-002: ThemeRepository abstraction

The system SHALL provide a ThemeRepository interface with Flow-based observation and suspend update functions.

**Scenario: ViewModel observes theme**
- GIVEN ThemeRepository interface exists
- WHEN ViewModel requests current theme
- THEN repository SHALL return Flow<AppTheme> emitting stored value
- AND repository SHALL expose suspend function to update theme

**Scenario: Transparent migration**
- GIVEN DataStore migration from legacy SharedPreferences (if any)
- WHEN ThemeDataStore initializes
- THEN existing preference SHALL be migrated transparently
- AND no user-visible reset SHALL occur