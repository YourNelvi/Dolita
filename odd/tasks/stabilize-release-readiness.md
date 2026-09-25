# Stabilize release readiness

## Objective

Close the five defects that block public distribution and leave the app with a
trustworthy test baseline: a real sampling-policy gate, externalized signing
credentials, a foreground service that does not crash, a real notification
permission flow, and unit-testable currency math.

## Problem

An architecture, security and distribution audit found the app is not safe to
publish as-is. Five of the findings are hard blockers with concrete user-visible
or credential-level impact:

1. The release signing password is committed in plaintext.
2. The floating-bubble foreground service throws `SecurityException` on launch.
3. The overlay foreground service throws `ForegroundServiceDidNotStartInTimeException`.
4. `POST_NOTIFICATIONS` is declared but never requested, so every notification
   silently no-ops on API 33+.
5. The currency conversion math is unreachable from any test, and the sampling
   policy has a dead parameter that a red test already caught.

Why now: the app has never been published, so the signing key can be rotated
cleanly. Once it ships, the key becomes an immutable app identity and this
cleanup becomes far more expensive.

## Why

A release build that crashes on the overlay path, never delivers a
notification, and ships a leaked signing password is not distributable. The
sampling bug is already in production behaviour, not a hypothetical.

## Scope

Authorized by the user as a single unit on 2026-09-25.

In scope:
- Implement the `usdtSampledThisSession` gate in `RateSamplingPolicy.shouldSample`
  so the existing red test passes and the declared parameter has behaviour.
- Move keystore credentials out of `app/build.gradle.kts` into an external
  source, rotate the signing key, and fail the build loudly when credentials
  are absent.
- Declare the foreground-service permissions and the `specialUse` subtype
  property, and use the typed `startForeground` overload.
- Request `POST_NOTIFICATIONS` at runtime, with a graceful denial path.
- Extract the calculator conversion math from `@Composable` local functions
  into a pure object and cover it with JVM unit tests.

Explicitly out of scope (found by the audit, deferred):
- `applicationId` rename from `com.example.erp`.
- Re-enabling R8 / resource shrinking.
- Fixing the 5 `androidTest` compile errors.
- Wiring up or deleting the unreachable overlay feature.
- Consolidating the two history directories.
- `RateWidgetProvider` scope leak and missing `goAsync()`.
- Privacy policy, Data Safety declaration, store listing, CI.

## Constraints

- `JAVA_HOME` is unset and `java` is not on `PATH`. Every Gradle command must
  set `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` explicitly.
- Verification command is `.\gradlew.bat testDebugUnitTest` and
  `.\gradlew.bat lintDebug` (NOT `test` / `connectedAndroidTest`).
- Lint currently reports 2 pre-existing errors in `@Preview` functions in
  `DolarScreen.kt` (`ViewModelConstructorInComposable`). These are known
  baseline failures; a task is not allowed to hide them.
- One unit test is red at baseline: `RateSamplingPolicyTest#usdt sampled this
  session is skipped`.
- Do not add AI attribution or `Co-Authored-By` to any commit.
- Conventional commit messages, matching the existing repo style (`type: text`,
  no scope).

## Tasks

- [x] T0. Commit the pre-existing in-flight theme-accent work on `master` as its
      own unit before branching. Evidence: `851afe1`.
- [ ] T1. Implement the `usdtSampledThisSession` gate in
      `RateSamplingPolicy.shouldSample` so `RateSamplingPolicyTest` goes green.
      Route: delegated writer. Check: `testDebugUnitTest` has 0 failures.
- [ ] T2. Externalize signing credentials and rotate the release keystore.
      Route: delegated writer. Check: `build.gradle.kts` contains no literal
      password; build fails with a clear message when credentials are absent.
- [ ] T3. Fix the foreground-service crash path: declare the FGS permissions,
      add the `specialUse` subtype property, use the typed `startForeground`
      overload, and give `OverlayService` a `foregroundServiceType` plus its own
      `startForeground` call so `ForegroundServiceDidNotStartInTimeException`
      cannot fire.
      Route: delegated writer. Check: `assembleDebug` succeeds; manifest
      contains the FGS permissions and the subtype property.
- [ ] T4. Request `POST_NOTIFICATIONS` at runtime from `MainActivity` before
      `setContent`, with a graceful denial path and no crash when denied.
      Route: delegated writer. Check: `assembleDebug` succeeds; the request is
      reachable and the denied path is handled.
- [ ] T5. Extract the calculator conversion math into a pure object and add JVM
      unit tests for it.
      Route: delegated writer. Check: new tests pass; `CalculatorCard` no longer
      owns the math as composable-local functions.
- [ ] T6. Make the history chart colour follow the active theme. Added 2026-09-25
      on user request; `EvolutionChart` currently hardcodes its colours and does
      not follow the palette. Reuse the `accent` theme role introduced in
      `851afe1` and the existing semantic up/down colours, so all six
      `AppTheme` palettes and both light and dark modes render correctly.
      Route: delegated writer. Check: chart reads colours from
      `MaterialTheme` / the theme roles, contains no hardcoded colour literals,
      `assembleDebug` succeeds, and both `@Preview`s in the file render.

## Authorized scope

Source under `app/src/main/java/com/example/erp/`, `app/src/test/`, plus
`app/build.gradle.kts`, `AndroidManifest.xml`, `.gitignore` and the signing
credential file. No changes to `gradle/libs.versions.toml`, no dependency
additions, no version bumps.

## Acceptance criteria

1. `.\gradlew.bat testDebugUnitTest` reports 0 failures.
2. `.\gradlew.bat assembleDebug` succeeds.
3. `app/build.gradle.kts` contains no hardcoded keystore password.
4. The manifest declares the foreground-service permissions and the
   `specialUse` subtype property.
5. A runtime notification-permission request exists.
6. The calculator math has direct unit-test coverage.
7. Lint does not regress beyond the 2 known baseline preview errors.

## Applicable checks

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --console=plain
.\gradlew.bat lintDebug --console=plain
.\gradlew.bat assembleDebug --console=plain
```

## Delivery strategy

`ask-on-risk` (default). Forecast is well under the ~400 authored changed-line
delivery budget across all five tasks, so no split is expected. RDD is enabled
globally; each work-unit commit is assessed and reviewed only if the native
assessment reports the change is due.

## Progress

Branch `stabilize-release-readiness` created from `master` at `851afe1`.
Baseline established before any task: 27 tests, 1 failure; lint 2 errors,
74 warnings.

| Task | Commit | Status |
|---|---|---|
| T0 | `851afe1` | done — pre-existing theme-accent work committed on `master` first |
| T1 | `7ad781e` | done — `usdtSampledThisSession` gate implemented |
| T2 | `31068cc` | done — signing credentials externalized, key rotated |
| T3 | `90551c8` | done — FGS permissions, typed service types, notification IDs |
| T4 | — | pending — runtime `POST_NOTIFICATIONS` request |
| T5 | — | pending — extract calculator math, add tests |
| T6 | — | pending — history chart follows the theme palette (added on request) |

## Verification evidence

- T0: no code change of ours; committed the user's in-flight theme work intact.
- T1: `testDebugUnitTest` — 29 tests, 0 failures, 0 skipped. One new test file
  `RateSamplingPolicySessionGateTest.kt` (2 tests) added because no existing
  test covered the interaction of the session gate with BCV quotes; a blunt
  `return emptyList()` would have passed all 8 original tests while silently
  killing BCV sampling for the rest of the session. Confirmed the call site
  (`DolarViewModel.kt:55,274,278`) is a closed latch, so the bug was live.
- T2: `assembleDebug` BUILD SUCCESSFUL with no credentials. `assembleRelease`
  BUILD FAILED with the new guard when credentials are absent, and BUILD
  SUCCESSFUL (41s) with the rotated keystore. `apksigner verify` shows the new
  signer SHA-256 differs from the legacy key. `keystore.properties` and both
  `.jks` files confirmed gitignored and never staged.
- T3: `assembleDebug` BUILD SUCCESSFUL. `testDebugUnitTest` — 29 tests,
  0 failures. FGS permissions verified in the MERGED manifest, not just the
  source. Could NOT verify at runtime: no device or emulator, so
  `SecurityException` / `MissingForegroundServiceTypeException` /
  `ForegroundServiceDidNotStartInTimeException` are addressed by construction
  and static inspection only. Play Console acceptance of the subtype strings
  is a human step outside the repo.

## Known caveats carried forward

- Lint still reports its 2 baseline `ViewModelConstructorInComposable` errors in
  `DolarScreen.kt` `@Preview` functions. Deferred with the overlay feature.
- The old plaintext signing password `dolita2026` remains in git history.
  Scrubbing it requires a history rewrite plus a force-push and was not
  authorized. The app was never published, so rotation is complete and the
  leaked value now protects nothing.

## Next step

Execute T4, T5 and T6 in order, one delegated writer per task, one work-unit
commit each.
