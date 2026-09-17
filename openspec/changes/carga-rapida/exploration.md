## Exploration: Carga rápida de datos y USDT lazy-loading

### Current State

**Data loading flow today:**
1. `DolarViewModel.init` → calls `load()` → `repository.getQuotes()`
2. `CachedDolarRepository.getQuotes()` → tries API first (BCV + USDT sequentially), on failure falls back to SharedPreferences cache (`QuotesCache`, 12h expiry)
3. BCV fetch: GET `rates.dolarvzla.com/bcv/current.json` → returns USD + EUR quotes
4. USDT fetch: POST `p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search` → returns average of BUY offers
5. Both are blocking network calls in `Dispatchers.IO` — user sees a spinner until BOTH complete

**Background scheduling:**
- `FetchQuotesWorker` — daily at 8 AM via WorkManager (fetches BCV + USDT)
- `FetchUsdtWorker` — hourly (fetches USDT only)
- These update the cache but the ViewModel still fetches on every launch

**Cache现状 (QuotesCache):**
- SharedPreferences-based, 12-hour expiry
- Only used as FALLBACK when API fails — never shown first for fast startup
- Stores all 3 quotes (BCV USD, BCV EUR, USDT) as serialized JSON

**Calculator screen:**
- The "calculator" is `CalculatorCard` composable inside `DolarScreen`
- It shows a currency conversion card (VES ↔ selected currency)
- It uses the same `DolarQuote` that was already fetched — no separate loading
- The user scrolls down to see it (it's below the featured card, chips, and section header)

### Affected Areas

- `app/src/main/java/com/example/erp/data/CachedDolarRepository.kt` — needs "cache-first" strategy instead of "API-first"
- `app/src/main/java/com/example/erp/ui/DolarViewModel.kt` — needs background refresh, lazy USDT loading
- `app/src/main/java/com/example/erp/data/ApiDolarRepository.kt` — may need separate `getQuotesBcv()` and `getQuotesUsdt()` methods
- `app/src/main/java/com/example/erp/data/DolarData.kt` — `DolarRepository` interface may need additional methods
- `app/src/main/java/com/example/erp/ui/DolarScreen.kt` — needs to trigger USDT load when calculator becomes visible
- `app/src/main/java/com/example/erp/data/QuotesCache.kt` — already good, just needs to be read first instead of last

### Approaches

1. **Cache-first + background refresh (Recommended)**
   - On launch: show cached data IMMEDIATELY (no spinner), then fetch fresh data in background
   - `CachedDolarRepository.getQuotes()` reads cache first, returns it, then launches API call to update cache
   - ViewModel gets cached data instantly, updates UI, then replaces with fresh data when available
   - USDT is fetched separately only when user scrolls to calculator or selects USDT chip
   
   - Pros: Instant startup, minimal UI changes, leverages existing QuotesCache, smooth UX
   - Cons: Slightly stale data shown briefly (typically <1s), need to handle "refreshing" state
   - Effort: Low-Medium

2. **Lazy USDT loading on calculator visibility**
   - BCV (USD/EUR) fetched on launch as today
   - USDT NOT fetched on launch
   - When user scrolls to CalculatorCard or selects USDT chip → trigger USDT fetch
   - Show loading indicator only in the USDT chip/card, not full-screen
   
   - Pros: Faster initial load (only 1 API call instead of 2), USDT only when needed
   - Cons: USDT chip might show stale/empty data initially, need visibility detection
   - Effort: Medium

3. **Combined: Cache-first BCV + lazy USDT**
   - Show cached BCV immediately on launch
   - Fetch fresh BCV in background (no spinner)
   - USDT: lazy-load only when calculator is visible or USDT chip selected
   - If USDT was cached (<12h), show it immediately; refresh in background when visible
   
   - Pros: Best UX — instant BCV, lazy USDT, always fresh data
   - Cons: Most complex, two loading strategies to manage
   - Effort: Medium

### Recommendation

**Approach 3 (Combined)** is the best fit:

1. **Cache-first BCV**: Modify `CachedDolarRepository.getQuotes()` to read cache first, return immediately, then update from API in background. The ViewModel shows cached data instantly and replaces when fresh data arrives.

2. **Lazy USDT**: Don't fetch USDT on launch. When user scrolls to calculator area or taps USDT chip, trigger `fetchUsdt()` separately. Show a loading indicator in the USDT chip only.

3. **Implementation details**:
   - `DolarRepository` interface: add `suspend fun getQuotesBcv(): List<DolarQuote>` and `suspend fun getQuotesUsdt(): DolarQuote?`
   - `CachedDolarRepository`: implement cache-first for BCV, lazy for USDT
   - `DolarViewModel`: separate `loadBcv()` and `loadUsdt()` functions
   - `DolarScreen`: use `LaunchedEffect` or scroll detection to trigger USDT load when calculator is near

### Risks

- Stale cache data shown briefly (12h max, typically <1s) — acceptable tradeoff for instant startup
- Need to handle partial data state (BCV loaded, USDT still loading) in UI
- USDT lazy loading adds complexity to the ViewModel state management
- `QuoteScheduler` already fetches USDT hourly — background cache stays warm

### Ready for Proposal

Yes — the approach is clear, low-risk, and leverages existing infrastructure (QuotesCache, WorkManager). The user should proceed to proposal phase.

## Key Learnings

1. The app already has a 12-hour SharedPreferences cache (QuotesCache) but only uses it as API failure fallback, not for fast startup.
2. USDT and BCV are fetched sequentially in a single getQuotes() call, blocking the UI for both even when only BCV is needed initially.
3. The CalculatorCard is a simple composable that uses the already-fetched DolarQuote — no separate data loading exists today.
4. WorkManager already keeps the cache warm with hourly USDT and daily BCV fetches — the cache-first strategy would make this background work immediately useful on app launch.
5. The DolarRepository interface is minimal (single getQuotes method) — splitting into BCV and USDT methods would enable lazy loading cleanly.
