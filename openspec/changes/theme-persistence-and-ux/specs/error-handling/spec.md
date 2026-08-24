# Error Handling Specification (Delta)

## Purpose

Extend existing data-fetching capability to distinguish NetworkError, ApiError, and ParseError in UI state.

## Modified Requirements

### REQ-EH-001: Distinguish error types in UI state

**Scenario: Network failure**
- GIVEN DolarViewModel fetches quotes
- WHEN network failure occurs (timeout, no connectivity)
- THEN DolarUiState.error MUST be NetworkError with user-facing message
- AND retry action SHALL be offered in UI

**Scenario: API error**
- GIVEN API returns non-2xx or malformed JSON
- WHEN parsing fails or BCV endpoint errors
- THEN DolarUiState.error MUST be ApiError with status/code
- AND error message SHALL indicate source (BCV / Binance)

**Scenario: Parse error**
- GIVEN JSON parsing succeeds but required fields missing
- WHEN DolarQuote construction fails
- THEN DolarUiState.error MUST be ParseError with field name
- AND partial data SHALL NOT be shown

### REQ-EH-002: Graceful degradation for USDT

**Scenario: USDT fails, BCV succeeds**
- GIVEN BCV fetch succeeds but USDT fetch fails
- WHEN getQuotes() completes
- THEN quotes list MUST contain BCV quotes only
- AND error SHALL be logged but NOT surfaced to UI