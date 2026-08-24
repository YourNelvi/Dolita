# Dynamic Color Control Specification

## Purpose

User-controllable Dynamic Color (Material You) with Android 12+ default-on behavior and opt-out toggle.

## Requirements

### REQ-DC-001: Dynamic Color enabled by default on Android 12+

**Scenario: Android 12+ uses Dynamic Color**
- GIVEN device runs Android 12+ (API 31+)
- WHEN ERPTheme composable renders
- THEN dynamicColor parameter MUST default to true
- AND system wallpaper-based palette SHALL be used automatically

**Scenario: Pre-Android 12 ignores Dynamic Color**
- GIVEN device runs Android < 12
- WHEN ERPTheme composable renders
- THEN dynamicColor MUST be ignored
- AND selected AppTheme light/dark palette SHALL apply

### REQ-DC-002: User can override Dynamic Color

**Scenario: User disables Dynamic Color**
- GIVEN user opens theme selector on Android 12+
- WHEN user toggles "Use Dynamic Color" OFF
- THEN selected AppTheme palette SHALL apply immediately
- AND preference MUST persist across restarts

**Scenario: User enables Dynamic Color**
- GIVEN user toggles "Use Dynamic Color" ON
- WHEN Dynamic Color was previously disabled
- THEN system palette SHALL apply immediately
- AND AppTheme selection SHALL be ignored while Dynamic Color is active