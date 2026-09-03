# Spec Delta: Error Handling Layer

## ADDED Requirements

### Requirement: Granular Error Types
WHEN the app encounters errors,
the system SHALL classify them into specific, actionable categories.

#### Scenario: Network Error Classification
GIVEN a network-related error
WHEN the error occurs
THEN it is classified as one of:
- `ConnectionError` (no internet, DNS failure)
- `TimeoutError` (request timeout)
- `ServerError` (HTTP 5xx)
- `ClientError` (HTTP 4xx)

#### Scenario: Cache Error Classification
GIVEN a cache-related error
WHEN the error occurs
THEN it is classified as:
- `CacheReadError` (failed to read from cache)
- `CacheWriteError` (failed to write to cache)
- `CacheExpiredError` (cache data is stale)

#### Scenario: Parsing Error Classification
GIVEN a data parsing error
WHEN the error occurs
THEN it is classified as:
- `JsonParseError` (invalid JSON structure)
- `MissingFieldError` (required field absent)
- `InvalidFormatError` (field has wrong format)

### Requirement: Retry Policy
WHEN transient errors occur,
the system SHALL automatically retry operations with exponential backoff.

#### Scenario: Retry on Network Error
GIVEN a network request fails with `ConnectionError`
WHEN the retry policy is configured
THEN the system retries up to 3 times
AND delay increases exponentially (1s, 2s, 4s)
AND success on retry returns the result

#### Scenario: No Retry on Permanent Error
GIVEN a request fails with `ClientError` (4xx)
WHEN the retry policy evaluates the error
THEN the system does not retry
AND the error is immediately propagated

#### Scenario: Retry Exhaustion
GIVEN all retry attempts fail
WHEN the maximum retries are reached
THEN the system returns the last error
AND logs the retry failure

### Requirement: Centralized Error Handler
WHEN errors occur anywhere in the app,
the system SHALL process them through a central handler.

#### Scenario: Error Mapping
GIVEN an exception from any layer
WHEN `ErrorHandler` processes it
THEN it maps to a typed `Error` object
AND logs the error with context

#### Scenario: Error Aggregation
GIVEN multiple errors in a session
WHEN errors are collected
THEN they are available for debugging
AND no error is silently lost

#### Scenario: Error Reporting
GIVEN a critical error
WHEN `ErrorHandler` processes it
THEN it logs to Android's logging system
AND includes relevant context (component, action, timestamp)

### Requirement: Enhanced UI Error Display
WHEN errors occur,
the system SHALL display user-friendly error messages with actions.

#### Scenario: Network Error UI
GIVEN a `ConnectionError`
WHEN the error is displayed
THEN the message says "Sin conexión a internet"
AND a "Reintentar" button is shown
AND the button triggers a retry

#### Scenario: Server Error UI
GIVEN a `ServerError`
WHEN the error is displayed
THEN the message says "Error del servidor"
AND shows the HTTP status code
AND provides a "Reportar problema" option

#### Scenario: Parsing Error UI
GIVEN a `JsonParseError`
WHEN the error is displayed
THEN the message says "Error en los datos recibidos"
AND suggests checking for app updates
AND provides a "Contactar soporte" option

### Requirement: Error Context Preservation
WHEN errors are handled,
the system SHALL preserve context for debugging.

#### Scenario: Error Metadata
GIVEN an error occurs
WHEN it is captured
THEN it includes:
- Timestamp of occurrence
- Component where error originated
- User action that triggered the error
- Stack trace (in debug builds)

#### Scenario: Error History
GIVEN errors occur during a session
WHEN debugging is needed
THEN recent errors are available
AND can be exported for support

## MODIFIED Requirements

### Requirement: Error State in UI
**Previous**: Simple error display with generic message
**Updated**: Rich error display with specific messages and actions

#### Scenario: Improved Error UX
GIVEN an error state in `DolarUiState`
WHEN the UI renders the error
THEN it shows a specific message based on error type
AND provides relevant action buttons
AND maintains error context for retry

## REMOVED Requirements

### Requirement: Generic Error Handling
**Previous**: All errors treated the same way
**Updated**: Errors are classified and handled specifically

#### Scenario: Eliminated Generic Errors
GIVEN the old error handling system
WHEN errors occur
THEN they are no longer lumped into generic categories
AND each error type has specific handling logic
