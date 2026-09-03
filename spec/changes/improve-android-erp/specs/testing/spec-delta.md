# Spec Delta: Testing Layer

## ADDED Requirements

### Requirement: Unit Test Coverage
WHEN the development team writes code changes,
the system SHALL have unit tests covering at least 60% of the data layer logic.

#### Scenario: Test Coverage Measurement
GIVEN a codebase with data layer components
WHEN running `./gradlew testDebugUnitTest`
THEN the test suite completes successfully
AND coverage reports show ≥60% line coverage in data layer

### Requirement: Fake Implementations
WHEN testing components that depend on external services,
the system SHALL provide fake implementations that simulate real behavior.

#### Scenario: Fake Dolar Repository
GIVEN a test that needs to verify ViewModel behavior
WHEN using `FakeDolarRepository`
THEN the fake returns predictable test data
AND the fake tracks method calls for verification

#### Scenario: Fake Rate History Store
GIVEN a test that needs to verify persistence logic
WHEN using `FakeRateHistoryStore`
THEN the fake stores data in memory
AND the fake simulates file system behavior

### Requirement: API Response Parsing Tests
WHEN the app receives JSON responses from external APIs,
the system SHALL have tests verifying correct parsing.

#### Scenario: BCV Response Parsing
GIVEN a valid BCV JSON response
WHEN `ApiDolarRepository.parseBcv()` processes it
THEN the method returns correct `DolarQuote` objects
AND all fields are populated correctly

#### Scenario: Binance Response Parsing
GIVEN a valid Binance P2P JSON response
WHEN `ApiDolarRepository.parseUsdt()` processes it
THEN the method calculates correct average price
AND returns a `DolarQuote` with proper metadata

#### Scenario: Malformed JSON Handling
GIVEN a malformed or incomplete JSON response
WHEN the parser processes it
THEN the system throws appropriate exceptions
AND error messages are descriptive

### Requirement: ViewModel State Tests
WHEN the ViewModel processes user actions and data,
the system SHALL have tests verifying state transitions.

#### Scenario: Loading State
GIVEN the ViewModel in idle state
WHEN `load()` is called
THEN the UI state shows `loading = true`
AND error state is cleared

#### Scenario: Success State
GIVEN successful API response
WHEN data is loaded
THEN the UI state contains quotes
AND `loading = false`
AND `error = null`

#### Scenario: Error State
GIVEN API failure
WHEN `load()` encounters an error
THEN the UI state shows appropriate error
AND `loading = false`
AND quotes remain unchanged

### Requirement: Persistence Layer Tests
WHEN the app stores and retrieves rate history,
the system SHALL have tests verifying data integrity.

#### Scenario: Write and Read Samples
GIVEN a list of `RateSample` objects
WHEN `FileHistoryStore.append()` is called
AND `readCurrentYear()` is called
THEN the stored samples match the original data
AND samples are sorted by timestamp

#### Scenario: Atomic Write Operation
GIVEN concurrent write operations
WHEN multiple threads access the store
THEN writes are serialized with Mutex
AND no data corruption occurs

#### Scenario: Corrupt File Recovery
GIVEN a corrupted rates JSON file
WHEN `readCurrentYear()` is called
THEN the method returns an empty list
AND no exception is thrown to the caller

## MODIFIED Requirements

### Requirement: Repository Interface Contract
**Previous**: `DolarRepository` interface with basic methods
**Updated**: `DolarRepository` interface with additional caching methods

#### Scenario: Cache-Aware Repository
GIVEN a `DolarRepository` implementation
WHEN `getQuotes()` is called
THEN the repository checks cache first
AND returns cached data if valid
AND fetches from API only if cache expired

## REMOVED Requirements

None - this proposal only adds new testing capabilities.
