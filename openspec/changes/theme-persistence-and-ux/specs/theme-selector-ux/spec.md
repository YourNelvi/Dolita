# Theme Selector UX Specification (Delta)

## Purpose

Extend existing ui-theme capability to replace DropdownMenu with BottomSheet featuring color previews, active indicator, mode toggle, and Dynamic Color control.

## Modified Requirements

### REQ-TS-001: BottomSheet with color previews

**Scenario: BottomSheet opens with previews**
- GIVEN user taps theme selector in TopAppBar
- WHEN BottomSheet opens
- THEN all 5 AppTheme options MUST show colored preview swatches
- AND active theme SHALL display checkmark indicator

**Scenario: Dynamic Color toggle on Android 12+**
- GIVEN Dynamic Color is available (Android 12+)
- WHEN BottomSheet renders
- THEN "Dynamic Color" toggle MUST appear at top
- AND when enabled, AppTheme list SHALL be disabled/grayed out

### REQ-TS-002: Light / Dark / System mode toggle

**Scenario: Mode selector visible**
- GIVEN user opens theme selector
- WHEN mode selector (Light / Dark / System) is visible
- THEN current mode SHALL be indicated
- AND selection MUST persist via ThemeRepository

**Scenario: System mode follows OS**
- GIVEN user selects "System" mode
- WHEN system theme changes (user toggles OS dark mode)
- THEN app theme SHALL follow system automatically
- AND no app restart SHALL be required