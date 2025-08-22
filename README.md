# bookfella
Big Travel Company faces quarterly “Super Sale” bursts that demand extremely fast search and reliable, high-throughput reservations. This project delivers a compact but production-credible backend proving capability at those loads.

## TL;DR
- __Endpoints__: `/api/search` and `/api/reservations`
- __SLOs (validated)__:
  - Search p95 < 200ms @ 800 rps → Achieved p95 ≈ 38 ms (warm cache)
  - Reservations p95 < 350ms @ 150 rps → Achieved p95 ≈ 4–5 ms (201 responses)
- __Exports__: k6 JSON summaries in `perf/verify-latest/`; Grafana dashboard provisioned.

## What this demonstrates
- __Cache-aside search__ with Redis (TTL 60s) and normalized cache keys.
- __Idempotent writes__ for reservations via Redis NX (TTL 600s).
- __Nginx gateway__ with rate limiting for writes and cache headers for reads.
- __Observability__: Micrometer → Prometheus `/actuator/prometheus` → Grafana p95 & RPS panels.
- __Elasticsearch 8__ for hotel search (single-node dev).

Key anchors (see `src/main/java/com/bookfella/booking/util/KeyUtils.java` and `ops/nginx/nginx.conf`):
- Redis TTLs: `SEARCH_TTL_SECONDS=60`, `IDEMPOTENCY_TTL_SECONDS=600`
- Nginx: `limit_req 200 r/s burst=100 nodelay` on `/api/reservations`; `Cache-Control: public, max-age=60` on `/api/search`
- Ports: App 8080, Gateway 8081, Redis 6379, ES 9200, Kafka 9092, Prometheus 9090, Grafana 3000

## Architecture at a glance
```mermaid
flowchart LR
  CLIENT[[Client]] -->|HTTP| GW[Gateway (Nginx) :8081]
  GW -->|GET /api/search (Cache-Control: 60s)| APP[Spring Boot App :8080]
  GW -->|POST /api/reservations (limit_req 200 r/s; burst=100 nodelay)| APP

  %% Search path (cache-aside)
  APP -->|Cache-aside get/set TTL 60s| REDIS[(Redis :6379)]
  APP -->|Query hotels| ES[(Elasticsearch :9200)]

  %% Reservation path (idempotency + DB write)
  APP -->|SET idem:{key} NX EX 600| REDIS
  REDIS -->|exists → 409 Conflict| APP
  APP -->|JPA write/read| DB[(DB: H2 Oracle mode)]

  %% Observability
  PROM[Prometheus :9090] -->|scrape /actuator/prometheus (5s)| APP
  GRAF[Grafana :3000] -->|PromQL| PROM
```

Legend and references:
- __Nginx__: `ops/nginx/nginx.conf` (rate limit and cache headers)
- __Prometheus__: `ops/prometheus/prometheus.yml` (scrapes app:8080 every 5s)
- __Redis keys__: `search:city:{city}`, `search:q:{normalized}`, `idem:{key}`; TTLs in `KeyUtils`

## Quickstart
Prereqs: Docker Desktop, Java 21, Maven 3.9+

1) Build and run the stack
```bash
docker compose up -d --build
# Gateway: http://localhost:8081, Grafana: http://localhost:3000 (admin/admin)
```

2) Sanity checks
```bash
curl -s "http://localhost:8081/api/search?city=Tokyo" | jq .
curl -s -X POST "http://localhost:8081/api/reservations" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: demo-$(date +%s)" \
  -d '{"userId":"u1","hotelId":"h1","checkIn":"2025-09-01","checkOut":"2025-09-03","totalPrice":199.99}' | jq .
```

## k6 performance (Windows-friendly)
We run k6 inside Docker on the compose network. Disable MSYS path conversion in Git Bash.

Search (800 rps, warm cache) → exports `perf/verify-latest/search-summary.json`:
```bash
MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" docker run --rm \
  --network bookfella_booking-net \
  -e GW_URL=http://gateway \
  -v d:/ai/bookfella/perf:/scripts \
  grafana/k6 run --summary-export=/scripts/verify-latest/search-summary.json /scripts/search.js
```

Reservations (150 rps, unique idempotency keys) → exports `perf/verify-latest/reservations-summary.json`:
```bash
MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" docker run --rm \
  --network bookfella_booking-net \
  -e GW_URL=http://gateway \
  -v d:/ai/bookfella/perf:/scripts \
  grafana/k6 run --summary-export=/scripts/verify-latest/reservations-summary.json /scripts/reservations-unique.js
```

Optional status breakdown (counts 201/409/429/503) → `perf/verify-latest/reservations-status-summary.json`:
```bash
MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" docker run --rm \
  --network bookfella_booking-net \
  -e GW_URL=http://gateway \
  -v d:/ai/bookfella/perf:/scripts \
  grafana/k6 run --summary-export=/scripts/verify-latest/reservations-status-summary.json /scripts/reservations-status.js
```

### k6 results (exported JSON)
- Search: `perf/verify-latest/search-summary.json` → p95 ≈ 38.37 ms
- Reservations: `perf/verify-latest/reservations-summary.json` → p95(201) ≈ 4.43 ms
- Status breakdown: `perf/verify-latest/reservations-status-summary.json` (201/409/429/503 counts)

## Grafana dashboard
- URL: `http://localhost:3000` (admin/admin) → dashboard “SuperSale API - Overview”
- Shows p95 latency by URI, RPS by URI, JVM panels; Prometheus target `spring-app` must be UP.

Screenshots (drop files in repo if you capture them):
- `docs/images/k6-search-p95.png`
- `docs/images/k6-reservations-p95.png`
- `docs/images/grafana-overview.png`

## Curl examples
Search (cache-aside):
```bash
curl -s "http://localhost:8081/api/search?city=Tokyo" | jq .
```

Reservation create (idempotent):
```bash
curl -s -X POST "http://localhost:8081/api/reservations" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: demo-$(date +%s)" \
  -d '{"userId":"u1","hotelId":"h1","checkIn":"2025-09-01","checkOut":"2025-09-03","totalPrice":199.99}' | jq .
```

Duplicate (expect 409 with same key):
```bash
KEY=dup-$(date +%s)
curl -s -X POST "http://localhost:8081/api/reservations" -H 'Content-Type: application/json' -H "Idempotency-Key: $KEY" -d '{"userId":"u1","hotelId":"h1","checkIn":"2025-09-01","checkOut":"2025-09-03","totalPrice":199.99}' > /dev/null
curl -i -s -X POST "http://localhost:8081/api/reservations" -H 'Content-Type: application/json' -H "Idempotency-Key: $KEY" -d '{"userId":"u1","hotelId":"h1","checkIn":"2025-09-01","checkOut":"2025-09-03","totalPrice":199.99}' | head -n 1
```

## 60–90 sec demo script
1) `docker compose up -d --build` and confirm health: app 8080, gateway 8081, Prometheus 9090, Grafana 3000.
2) Open Grafana → “SuperSale API - Overview”; keep it visible.
3) Warm search cache: hit `/api/search?city=Tokyo` once.
4) Run k6 search at 800 rps (command above). Show p95 panel in Grafana and mention JSON in `perf/verify-latest/search-summary.json`.
5) Run k6 reservations at 150 rps with unique keys. Show p95 panel; mention `perf/verify-latest/reservations-summary*.json`.
6) (Optional) Run status breakdown. Mention counts for 201/409/429/503.
7) Call out anchors: Redis TTL 60s (search), 600s (idempotency); Nginx limits and cache headers; ports per PRD.

## Project structure (high level)
- App: `src/main/java/com/bookfella/booking/...`
- Config: `ops/nginx/nginx.conf`, `ops/prometheus/prometheus.yml`, `ops/grafana/...`
- Perf: `perf/*.js`, exports in `perf/verify-latest/`
- Compose: `docker-compose.yml`

