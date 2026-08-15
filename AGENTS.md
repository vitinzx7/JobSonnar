# AGENTS.md

JobSonnar = job search aggregator. Backend: Java 21 / Spring Boot (Maven wrapper) in repo root. Frontend: React 19 + Vite in `frontend/`.

## Run locally

Backend (root):
```bash
set -a; source .env; set +a
./mvnw spring-boot:run   # use ./mvnw, not mvn
```
`.env` (gitignored; see `.env.example`) sets `SERVER_PORT=8081` and the Jooble/Adzuna API keys. Do not start the backend without sourcing it: the app then defaults to port 8080, but the frontend calls `localhost:8081` and the Jooble/Adzuna providers return no results without keys.

Frontend:
```bash
cd frontend && npm install && npm run dev   # http://localhost:5173
```

CORS in `JobController.java:26` is hardcoded to `http://localhost:5173`. If the Vite dev port changes, update it.

## Verify

- Backend tests: `./mvnw test` (root). Tests live in `src/test/java/jobsonnar/`.
- Frontend: no test script — only `npm run lint` (ESLint) and `npm run build` (writes to gitignored `dist/`). Run lint before finishing frontend changes.

## Architecture / flow

`JobController.getJobs` (`GET /jobs?query=&location=&radiusKm=`) → `JobService` loops over all `JobProvider` beans (Gupy, Jooble, Adzuna), each returns `JobResponseDto`, then dedupes on `jobUrl+name+city` (`putIfAbsent`, first provider wins) and filters by location/radius.

- Each provider maps its own response shape to `JobResponseDto` (name, city, jobUrl, publishedDate). Gupy deserializes into `model/Job` + `JobResponse`; Jooble/Adzuna use dedicated DTO packages.
- A failing provider must not break the search: `JobService.safeSearch` catches per-provider exceptions and returns partial results (covered by `searchJobsKeepsPartialResultsWhenOneProviderFails`). Keep this behavior.

## Gotchas

- **Frontend link allowlist**: `isSafeJobUrl` in `App.jsx` only renders "Ver vaga" for domains it recognizes (`gupy.io`, `jooble.org`, `adzuna.com`, `adzuna.com.br`). Adding a new provider requires adding its domain here, or links show "Link indisponível". Adzuna links are `www.adzuna.com.br/...` — `adzuna.com` alone does not match.
- **Radius/location filter is a heuristic**: `JobService.matchesSearchArea` matches a hardcoded list of Brasília/DF terms on the normalized name+city+url text; it is not real geocoding. `city` is often blank in provider data, so don't rely on it for filtering.
- **JPA/PostgreSQL are commented out in `pom.xml`**. Do not re-enable them: Spring then fails to start without a running database. Only re-add when persistence work begins.
- No external-call timeouts or input validation/caching exist yet — this is a known limitation, not a regression.
