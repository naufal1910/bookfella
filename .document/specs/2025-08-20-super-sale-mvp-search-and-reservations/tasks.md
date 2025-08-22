# Tasks — Super-Sale MVP: Search & Reservations

Spec folder: `.document/specs/2025-08-20-super-sale-mvp-search-and-reservations/`

- [x] 1. Project scaffolding and base configs (Java 21, Spring Boot 3.5.4)
  - [x] 1.1 Test-first: Add a `@WebMvcTest` health check test for `/api/health` returns 200
  - [x] 1.2 Initialize Maven wrapper and Spring Boot project; add dependencies: Web, Validation, Data JPA, Data Redis, Data Elasticsearch, Spring for Kafka, Micrometer Prometheus, Resilience4j, Test libs
  - [x] 1.3 Create package layout per code-style (`config/`, `domain/`, `dto/`, `repo/`, `service/`, `web/`, `util/`)
  - [x] 1.4 Configure ports: app 8080; confirm `/actuator/prometheus` exposed
  - [x] 1.5 Verify: `mvn -q -DskipTests=false verify` passes and health test green

- [x] 2. Data model & persistence (Oracle-aligned; H2 Oracle mode for dev)
  - [x] 2.1 Test-first: `@DataJpaTest` saves and fetches a `Reservation` entity; validate column mappings
  - [x] 2.2 Create `RESERVATIONS` table via schema.sql; include index `IDX_RES_HOTEL (HOTEL_ID, CHECK_IN, CHECK_OUT)`
  - [x] 2.3 Implement JPA `Reservation` entity and `ReservationRepository`
  - [x] 2.4 Configure H2 in Oracle mode and datasource props
  - [x] 2.5 Verify: Data JPA tests pass; index present in schema script

- [x] 3. Elasticsearch hotels index (mapping + client + seed)
  - [x] 3.1 Test-first: Service unit test verifies ES query → returns mapped `Hotel` DTOs (mock ES client)
  - [x] 3.2 Define ES mapping: `name text+keyword`, `city keyword`, `tags keyword`, `priceFrom scaled_float`
  - [x] 3.3 Configure ES 8 client and index bootstrap; seed sample hotels
  - [x] 3.4 Verify: Smoke query returns seeded results

- [x] 4. Search API — GET `/api/search` with Redis cache-aside (TTL 60s)
  - [x] 4.1 Test-first: `@WebMvcTest` returns 400 if neither `q` nor `city` provided; 200 happy path
  - [x] 4.2 Slice/unit tests for cache: one test ensures cache miss → ES fetch → cache set; another ensures cache hit bypasses ES
  - [x] 4.3 Implement `SearchService` with keys `search:q:{normalized}` and `search:city:{city}`, TTL=60s; page size ≤20; normalization util
  - [x] 4.4 Add Resilience4j Retry(3) + CircuitBreaker(50/50) around ES calls
  - [x] 4.5 Verify: Tests pass; confirm TTL=60s explicitly in code; metrics timer for `/api/search`

- [x] 5. Reservations API — POST `/api/reservations` (idempotent) and GET by id
  - [x] 5.1 Test-first: `@WebMvcTest` enforces `Idempotency-Key` header and Bean Validation on DTO; returns 409 on duplicate key
  - [x] 5.2 Unit test `ReservationService` idempotency: Redis `SET idem:{key} <val> NX EX 600` called; duplicate path returns 409
  - [x] 5.3 Implement transactional create: idempotency check → DB save → after commit publish event → return 201 with DTO
  - [x] 5.4 Implement GET `/api/reservations/{id}` returning DTO or 404; add RFC7807 error handler
  - [x] 5.5 Verify: Tests pass; confirm TTL=600s explicitly in code

- [x] 6. Messaging — publish to Kafka topic `reservations.created`
  - [x] 6.1 Test-first: `spring-kafka-test` verifies event sent to `reservations.created` with expected payload after commit
  - [x] 6.2 Configure Kafka producer (dev: Redpanda); topic `reservations.created`
  - [x] 6.3 Implement event model and publisher triggered only after successful commit
  - [x] 6.4 Verify: Kafka test passes; topic name exact

- [x] 7. Gateway — Nginx 8081 (rate limit + cache header)
  - [x] 7.1 Test-first: Scripted smoke checks verifying `Cache-Control: public, max-age=60` on `/api/search` and rate limit behavior on POST
  - [x] 7.2 Add Nginx config:
        `limit_req_zone $binary_remote_addr zone=rez:10m rate=200r/s;`
        `server { listen 8081; location /api/reservations { limit_req zone=rez burst=100 nodelay; proxy_pass http://app:8080; } location /api/search { add_header Cache-Control "public, max-age=60"; proxy_pass http://app:8080; } }`
  - [x] 7.3 Wire via Docker Compose (gateway → app:8080)
  - [x] 7.4 Verify: Headers and rate limiting behave as expected
  - Status note (2025-08-22T02:01:40+07:00): Verified via gateway: `Cache-Control: public, max-age=60` on `/api/search`. k6 Docker run (`perf/rl-simple.js`) at ~300 rps observed both passes and limits: `created_201=4095` (~205 rps), `rate_limited=1906` (~95 rps), aligning with Nginx `rate=200r/s` and `burst=100 nodelay`. Evidence in `perf/rl-summary.json`.

- [ ] 8. Observability — Prometheus, Grafana, structured logs
  - [x] 8.1 Test-first: Integration or unit test confirms `/actuator/prometheus` exposes HTTP server metrics; JSON log format outputs traceId/uri/status
  - [x] 8.2 Configure Micrometer Prometheus endpoint; Prom scrape config (every 5s); Grafana dashboard JSON with p95/p99, RPS, error %, JVM
  - [x] 8.3 Add request metrics (timers) for search/reservations; structured JSON logging (no PII)
  - [x] 8.4 Verify: Dashboards show traffic; metrics present
  - Status note (2025-08-22T03:29:57+07:00): Micrometer histograms enabled in `src/main/resources/application.yml`; Prometheus scrape configured in `ops/prometheus/prometheus.yml`. Grafana provisioning added:
    - Datasource: `ops/grafana/provisioning/datasources/datasource.yml` → `http://prometheus:9090`
    - Dashboard: `ops/grafana/dashboards/supersale.json` via provider `ops/grafana/provisioning/dashboards/dashboards.yml`
    To verify: (1) `docker compose up -d --build app prometheus grafana`; (2) hit `http://localhost:8081/api/search?city=Tokyo` a few times; (3) Prom query `sum by (uri) (rate(http_server_requests_seconds_count[1m]))` shows data; (4) Grafana at `http://localhost:3000` (admin/admin) → dashboard "SuperSale API - Overview" shows p95/RPS/JVM panels with live traffic.

  - Status note (2025-08-22T11:41:20+07:00): Verified. Grafana dashboard "SuperSale API - Overview" shows live panels: HTTP p95 latency by URI (includes `/api/search`), RPS by URI non-zero after sample traffic, and JVM heap. Prometheus target `spring-app` is UP and queries return results for RPS and p95. Evidence: screenshot attached; Prometheus queries executed locally.

- [ ] 9. Performance validation — k6 scripts and runs
  - [x] 9.1 Test-first: Commit k6 scripts for `/api/search` and `/api/reservations` with thresholds (p95<200ms @800rps; p95<350ms @150rps)
  - [x] 9.2 Run k6 with warm-cache for search; record results and screenshots
  - [x] 9.3 Tune (ES query, caching, threadpools) until thresholds pass
  - [x] 9.4 Verify: Attach k6 results to PR; thresholds pass
  - Status note (2025-08-21T21:46:59+07:00): Search smoke p95 ~32ms (pass). Full 800 rps run exceeded p95; re-run with warm cache planned.
  - Status note (2025-08-22T11:45:30+07:00): Applied tuning in `src/main/resources/application.yml`:
    - `server.tomcat.threads.max=400`, `min-spare=50`, `accept-count=1000`, `connection-timeout=5s`
    - Redis Lettuce pool: `max-active=64`, `max-idle=16`, `min-idle=8`
    - Elasticsearch client timeouts: `connection-timeout=200ms`, `socket-timeout=500ms`
    - Hikari pool: `maximum-pool-size=20`
    Redis/ES TTL anchors remain unchanged per PRD: search=60s, idempotency=600s.
  - Status note (2025-08-22T12:54:36+07:00): k6 thresholds PASS under gateway `http://gateway` on Docker network `bookfella_booking-net`.
    - `/api/search` @ 800 rps (warm cache): http_req_duration p95 ≈ 38.37 ms (<200). Error rate: 0. Evidence: `perf/verify-latest/search-summary.json`.
    - `/api/reservations` @ 150 rps: http_req_duration p95 ≈ 4.21 ms overall; for successful responses (`expected_response:true`) p95 ≈ 4.43 ms (<350). Evidence: `perf/verify-latest/reservations-summary-latest.json`.
    Grafana “SuperSale API - Overview” shows non-zero RPS and p95 by URI; Prometheus target `spring-app` is UP.

- [ ] 10. CI/CD & local infra
  - [ ] 10.1 Test-first: Minimal Jenkins pipeline step that fails on coverage <80% for changed code
  - [ ] 10.2 Create Jenkinsfile: build → test → sonar (stub) → docker build
  - [x] 10.3 Docker Compose: app(8080), nginx(8081), redis(6379), es(9200), kafka(9092), prometheus(9090), grafana(3000)
  - [ ] 10.4 Verify: `docker compose up -d` brings up stack; pipeline runs green

- [ ] 11. API contracts, ADRs, and docs
  - [ ] 11.1 Test-first: Contract tests using `@WebMvcTest` validate OpenAPI fields (status codes, required headers)
  - [ ] 11.2 Align controllers with `api-spec.md`; ensure RFC7807 error shapes
  - [ ] 11.3 ADR: Document TTL anchors (60s search, 600s idempotency), topic name, gateway limits
  - [ ] 11.4 Verify: Docs updated; spec acceptance criteria checklist complete
