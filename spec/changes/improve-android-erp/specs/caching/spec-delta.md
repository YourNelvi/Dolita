# Spec Delta: Caching Layer

## ADDED Requirements

### Requirement: Memory Cache
WHEN the app needs fast access to frequently used data,
the system SHALL maintain an in-memory cache.

#### Scenario: Cache Initialization
GIVEN the app starts
WHEN memory cache is initialized
THEN it creates an LruCache with appropriate size limits
AND the cache is ready to store data

#### Scenario: Cache Hit
GIVEN data exists in memory cache
WHEN the app requests that data
THEN the cache returns it immediately
AND no network request is made

#### Scenario: Cache Miss
GIVEN data does not exist in memory cache
WHEN the app requests that data
THEN the cache returns null
AND the app fetches from network

#### Scenario: Cache Expiration
GIVEN data in cache has exceeded TTL
WHEN the app requests that data
THEN the cache treats it as a miss
AND the app fetches fresh data

### Requirement: Persistent Cache
WHEN the app needs data that survives app restarts,
the system SHALL maintain a persistent cache.

#### Scenario: Cache Persistence
GIVEN data is stored in persistent cache
WHEN the app is closed and reopened
THEN the cached data is still available
AND can be used immediately

#### Scenario: Cache Storage
GIVEN the persistent cache implementation
WHEN storing data
THEN it uses Room or DataStore
AND data is stored in a structured format

#### Scenario: Cache Size Management
GIVEN the persistent cache reaches size limit
WHEN new data needs to be stored
THEN old data is evicted based on LRU policy
AND the cache stays within size limits

### Requirement: Cache Integration
WHEN the Repository needs to provide data,
the system SHALL use cache as the primary source.

#### Scenario: Cache-First Strategy
GIVEN a request for quotes
WHEN `getQuotes()` is called
THEN the repository checks memory cache first
AND returns cached data if valid
AND fetches from network only if cache misses or expires

#### Scenario: Cache Update on Fetch
GIVEN fresh data is fetched from network
WHEN the fetch succeeds
THEN the repository updates both memory and persistent cache
AND the new data is available immediately

#### Scenario: Fallback to Cache
GIVEN a network request fails
WHEN the repository has cached data
THEN it returns the cached data
AND indicates the data may be stale

### Requirement: Cache Invalidation
WHEN data becomes stale or invalid,
the system SHALL invalidate cache appropriately.

#### Scenario: Time-Based Invalidation
GIVEN cached data with TTL
WHEN the TTL expires
THEN the cache marks the data as invalid
AND next request fetches fresh data

#### Scenario: Manual Invalidation
GIVEN the user triggers a refresh
WHEN the refresh button is pressed
THEN the cache is invalidated for that data
AND fresh data is fetched immediately

#### Scenario: Source-Specific Invalidation
GIVEN multiple data sources (BCV, Binance)
WHEN one source fails
THEN only that source's cache is invalidated
AND other sources remain cached

### Requirement: Offline Support
WHEN the device has no internet connection,
the system SHALL provide cached data.

#### Scenario: Offline Mode
GIVEN the device is offline
WHEN the app tries to fetch data
THEN it returns cached data immediately
AND shows an indicator that data may be stale

#### Scenario: Offline Refresh Attempt
GIVEN the device is offline
WHEN the user pulls to refresh
THEN the app attempts to fetch
AND shows appropriate offline error
AND retains cached data

## MODIFIED Requirements

### Requirement: Repository Data Flow
**Previous**: Repository always fetches from network
**Updated**: Repository uses cache-first strategy

#### Scenario: Updated Data Flow
GIVEN a request for data
WHEN the repository processes it
THEN it checks cache before network
AND updates cache on successful fetch
AND provides fallback on network failure

## REMOVED Requirements

### Requirement: Always-Fresh Data
**Previous**: App always shows fresh data from network
**Updated**: App may show cached data for better UX

#### Scenario: Eliminated Always-Fresh Requirement
GIVEN the old behavior
WHEN the app loads
THEN it no longer requires network for every display
AND can show cached data immediately
