# Spec Delta: Dependency Injection Layer

## ADDED Requirements

### Requirement: Hilt Configuration
WHEN the Android application starts,
the system SHALL use Hilt for dependency injection.

#### Scenario: Application Initialization
GIVEN the Android application process
WHEN the app starts
THEN `ERPApplication` class is annotated with `@HiltAndroidApp`
AND Hilt initializes the dependency graph

#### Scenario: Activity Injection
GIVEN a `ComponentActivity` in the app
WHEN the activity is created
THEN Hilt injects dependencies into the activity
AND dependencies are available throughout the activity lifecycle

### Requirement: Repository Module
WHEN the app needs repository instances,
the system SHALL provide them through Hilt modules.

#### Scenario: DolarRepository Provision
GIVEN a component that needs `DolarRepository`
WHEN requesting the dependency
THEN Hilt provides an `ApiDolarRepository` instance
AND the instance is properly configured

#### Scenario: Repository Scoping
GIVEN multiple components requesting `DolarRepository`
WHEN dependencies are resolved
THEN a single instance is shared (singleton scope)
AND the instance is thread-safe

### Requirement: Network Module
WHEN the app needs network clients,
the system SHALL provide them through Hilt modules.

#### Scenario: OkHttpClient Provision
GIVEN a component that needs `OkHttpClient`
WHEN requesting the dependency
THEN Hilt provides a configured `OkHttpClient`
AND the client has proper timeouts (15s connect, 15s read)

#### Scenario: Network Configuration
GIVEN the `NetworkModule`
WHEN configuring the HTTP client
THEN it includes proper interceptors
AND logging is configured for debug builds

### Requirement: Storage Module
WHEN the app needs storage components,
the system SHALL provide them through Hilt modules.

#### Scenario: RateHistoryStore Provision
GIVEN a component that needs `RateHistoryStore`
WHEN requesting the dependency
THEN Hilt provides a `FileHistoryStore` instance
AND the instance uses the app's files directory

#### Scenario: ThemeRepository Provision
GIVEN a component that needs `ThemeRepository`
WHEN requesting the dependency
THEN Hilt provides a `ThemeRepositoryImpl` instance
AND the instance uses SharedPreferences

### Requirement: ViewModel Injection
WHEN ViewModels need dependencies,
the system SHALL inject them through Hilt.

#### Scenario: DolarViewModel Constructor
GIVEN a `DolarViewModel` instance
WHEN the ViewModel is created
THEN dependencies are injected via `@Inject constructor`
AND no manual instantiation occurs in the ViewModel

#### Scenario: ViewModel Factory
GIVEN a Composable that needs `DolarViewModel`
WHEN using `viewModel()` function
THEN Hilt provides the ViewModel with all dependencies
AND the ViewModel is properly scoped to the activity

### Requirement: Test Module Support
WHEN running tests that need dependency injection,
the system SHALL support test-specific modules.

#### Scenario: Fake Repository in Tests
GIVEN a test that needs `DolarRepository`
WHEN running with Hilt test runner
THEN the test can provide a `FakeDolarRepository`
AND the fake is used instead of the real implementation

#### Scenario: Test Configuration
GIVEN the test configuration
WHEN running `@HiltAndroidTest`
THEN test modules override production modules
AND tests run with isolated dependencies

## MODIFIED Requirements

### Requirement: ViewModel Dependencies
**Previous**: ViewModel manually instantiates dependencies
**Updated**: ViewModel receives dependencies via injection

#### Scenario: Dependency Resolution
GIVEN a `DolarViewModel` with injected dependencies
WHEN the ViewModel is created
THEN all dependencies are provided by Hilt
AND no `ApiDolarRepository()` calls exist in ViewModel code

## REMOVED Requirements

### Requirement: Manual Dependency Creation
**Previous**: Components create their own dependencies
**Updated**: Dependencies are provided by Hilt container

#### Scenario: Eliminated Manual Instantiation
GIVEN any component that previously created dependencies
WHEN the component needs a dependency
THEN it requests it from Hilt instead of creating it
AND no `new` or constructor calls for dependencies exist
