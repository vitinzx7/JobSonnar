# AGENTS.md

JobSonnar = job search aggregator. Backend: Java 21 / Spring Boot (Maven wrapper) in repo root. Frontend: React 19 + Vite in `frontend/`.

## Run locally

Backend (root):
```bash
set -a; source .env; set +a
./mvnw spring-boot:run   # use ./mvnw, not mvn
```
`.env` (gitignored; see `.env.example`) sets `SERVER_PORT=8081` and the Jooble, Adzuna and Serper API keys. Do not start the backend without sourcing it: the app then defaults to port 8080, but the frontend calls `localhost:8081` and the Jooble/Adzuna/Serper providers return no results without keys.

Frontend:
```bash
cd frontend && npm install && npm run dev   # http://localhost:5173
```

CORS in `JobController.java:26` is hardcoded to `http://localhost:5173`. If the Vite dev port changes, update it.

## Verify

- Backend tests: `./mvnw test` (root). Tests live in `src/test/java/jobsonnar/`.
- Frontend: no test script — only `npm run lint` (ESLint) and `npm run build` (writes to gitignored `dist/`). Run lint before finishing frontend changes.

## Architecture / flow

`JobController.getJobs` (`GET /jobs?query=&location=&radiusKm=&provider=`) → `JobService.searchJobs` normalizes/validates the input, then checks the in-memory `SearchCache` (short-lived, TTL `search.cache.ttl-seconds`, default 120s); on a miss it runs a `safeSearch` for each matching `JobProvider` bean (Gupy, Jooble, Adzuna, Serper) concurrently on a fixed `ExecutorService` sized to the provider count (`JobService` field, daemon threads, `@PreDestroy` shutdown), then joins the futures strictly in provider order, dedupes/filters by location/radius, and stores the final sorted result in the cache. All provider `RestClient`s get a 5s connect / 10s read timeout via the `RestClientCustomizer` bean in `RestClientConfig` (auto-applied to the injected `RestClient.Builder`, Spring Boot >= 3.2).

- **Input validation** happens in the `JobSearchRequest` record constructor plus an explicit guard in `JobService.searchJobs`: (1) `query` is trimmed; a null/blank query returns an empty list immediately WITHOUT calling any provider (explicit service guard; provider guards still apply); a query longer than 100 chars is truncated to the first 100 chars (after trim); (2) `location` is trimmed, blank is tolerated; (3) `radiusKm` <= 0 (or null) is treated as no radius, values > 50 are clamped to 50 (matches the frontend max), 1..50 pass through unchanged. `radiusKm` only meaningfully filters when a location/radius search is active.
- **Short-lived in-memory cache**: `SearchCache` (`src/main/java/jobsonnar/service/SearchCache.java`) is a thread-safe `@Component` (ConcurrentHashMap) keyed by a normalized key built in `JobService.buildCacheKey` from `(trimmed query, location, radiusKm-or-null, provider-or-"all")` — provider is normalized (accents stripped, whitespace removed) so `provider=jooble` and `provider=Jooble` share a key. Each entry stores the final deduped/sorted `List<JobResponseDto>` plus an insertion timestamp; TTL is injected via `@Value("${search.cache.ttl-seconds:120}")` (constructor takes seconds; a second constructor takes a `Duration` for tests, e.g. 50ms expiry). `get()` returns a defensive copy (`new ArrayList<>(entry.jobs)`) and removes the entry once expired (lazy eviction — no scheduler). The cache wraps the FINAL sorted/deduped result only. `JobService` keeps a one-arg constructor that defaults to a 120s cache so plain tests need no cache wiring.

- Dedup is cross-provider fuzzy: two jobs are the same when their URLs are identical OR their normalized titles are ≥ 60% token-similar (Jaccard) with compatible cities (≥ 60% similar, or at least one blank — Serper has no city) AND compatible companies (≥ 60% similar, or one name is a substring of the other, or at least one blank — Serper has no company). First provider wins (`putIfAbsent` semantics via `isSameJob` in `JobService`). Exact `jobUrl+name+city` alone is NOT enough because every API returns the same job with a different URL. Distinct companies with the same title+city are kept (they are different postings).

- The optional `provider` param (e.g. `provider=jooble`) limits the search to a single provider; each `JobProvider` exposes a stable `providerName()` key (`gupy`, `jooble`, `adzuna`, `serper`). The frontend has a provider dropdown to test each source individually.

- Each provider maps its own response shape to `JobResponseDto` (name, company, city, jobUrl, publishedDate). Company comes from Gupy's `careerPageName`, Jooble's `company`, Adzuna's `company.display_name`, TheirStack's `company.name`; Serper has no company. Gupy deserializes into `model/Job` + `JobResponse`; Jooble/Adzuna/Serper use dedicated DTO packages. Both Open Web Ninja (`dto/openwebninja`) and TheirStack (`dto/theirstack`) providers are **disabled** — the `@Component` was removed from `OpenWebNinjaJobProvider` (its API is rate-limited: the 200 free requests were exhausted) and from `TheirStackJobProvider` (its API key was rejected with 401) — re-enable only when the limit resets / a valid key exists. TheirStack uses `POST /v1/jobs/search` with `Authorization: Bearer <key>`, requires `posted_at_max_age_days` (or a company filter), and returns jobs under `data[]` with `job_title`/`url`/`short_location`/`date_posted`. Serper uses web search (`POST /search` on `google.serper.dev`) with an `X-API-KEY` header and a body of `q`/`gl`/`hl`/`num` (hardcoded `br`/`pt-br`, 10 results), returning `organic[]` results with `title`/`link`/`snippet`/`date`. Note: Serper's dedicated Google Jobs endpoint (`/jobs`) no longer exists — it returns 404 with a valid key.
- A failing provider must not break the search: `JobService.safeSearch` catches per-provider exceptions and returns partial results (covered by `searchJobsKeepsPartialResultsWhenOneProviderFails`). Keep this behavior.
- Each job's `source` field is set to the supplying provider's `providerName()` (`gupy`, `jooble`, `adzuna`, `serper`) in `JobService` before dedup, so a deduped job keeps the source of the first provider that supplied it. The frontend renders it as a badge in the card via `PROVIDER_LABELS[job.source] || job.source` (see `job-source` in `App.css`).
- Final results are sorted newest-first by `publishedDate` in `JobService.searchJobs` (`sortByPublishedDate`). The date is parsed leniently: try `Instant`, then `LocalDateTime`, then `LocalDate` (all `java.time`); any null, blank, or unparseable value sorts to the end. Ties keep the existing (provider-order) sequence because `List.sort` on objects is a stable sort.

## Gotchas

- **Frontend link check**: `isSafeJobUrl` in `App.jsx` renders "Ver vaga" for any valid `https:` URL. It was previously a strict domain allowlist (`gupy.io`, `jooble.org`, `adzuna.com(.br)`), but Open Web Ninja's apply links (disabled provider) point to arbitrary employer domains (LinkedIn, Indeed, ZipRecruiter...), so the allowlist was relaxed. Links still open with `noopener noreferrer`. Don't re-introduce a strict allowlist without breaking Open Web Ninja-style apply links if that provider is re-enabled.
- **Radius/location filter is a heuristic**: `JobService.matchesSearchArea` matches a hardcoded list of Brasília/DF terms on the normalized name+city+url text; it is not real geocoding. `city` is often blank in provider data, so don't rely on it for filtering.
- **JPA/PostgreSQL are commented out in `pom.xml`**. Do not re-enable them: Spring then fails to start without a running database. Only re-add when persistence work begins.
- HTTP timeouts (5s connect / 10s read) are handled centrally by the `RestClientCustomizer` in `RestClientConfig`.
