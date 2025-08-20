---
trigger: always_on
---

# SuperSale Mini Booking API — Development Best Practices

## Purpose
Prescribe pragmatic patterns that keep code **fast, safe, and observable** under Big Travel Company Super Sale profile (≥800 reads/sec, ≥150 writes/sec).

## When to Use
- For all specs, implementation, reviews, and demos.
- When choosing between alternatives (caching/indexing/resilience).
- Before merging PRs that affect hot paths.

## Inputs
- PRD.md (canonical endpoints, SLOs, TTLs, ports, rate limits)
- ADRs/decisions (cache TTLs, idempotency, outbox, gateway)
- Tech stack pins (Java 17, Spring Boot 3.3, Redis 7, ES 8, Kafka/Redpanda, Nginx, Prometheus/Grafana, Jenkins/Sonar)

## Outputs
- Code and configs that meet SLOs and pass CI.
- Dashboards showing p95/p99, error rate, and JVM health.
- Updated ADRs when behavior or defaults change.

## Acceptance Criteria
- `/api/search`: **p95 < 200ms @ 800 rps** (after warm cache).
- `/api/reservations`: **p95 < 350ms @ 150 rps**.
- Required anchors implemented exactly:  
  - **Redis TTLs:** search **60s**, idempotency **600s**  
  - **Kafka topic:** `reservations.created`  
  - **Nginx:** `limit_req 200 r/s burst=100 nodelay`; `Cache-Control: public, max-age=60` for `/api/search`  
  - **Prometheus:** `/actuator/prometheus`  
  - **Ports:** 8080(app), 8081(gateway), 6379(redis), 9200(es), 9092(kafka), 9090(prom), 3000(grafana)

---

## Core Practices

### 1) TDD & Test Pyramid
- Unit > Slice (`@WebMvcTest`, `@DataJpaTest`) > Integration (optional Testcontainers).
- Coverage target: **≥80% on changed code**. Fail CI if below.

### 2) API Contracts & Validation
- DTOs with Bean Validation: `@NotBlank @Positive @Pattern` etc.
- RFC7807 error shape via `@ControllerAdvice` (400/404/409/500).

### 3) Idempotent Writes
- Require `Idempotency-Key` for **POST /api/reservations**.  
- Store key in Redis with `SET key value NX EX 600`. On duplicate → **409**.
- Persist reservation within a single transactional boundary; publish outbox event **after commit**.

### 4) Caching & Search
- **Cache-aside** for `/api/search`:  
  - Keys: `search:city:{city}` or `search:q:{normalized}`  
  - TTL: **60s**; limit ES payload (page size ≤20).
- ES mapping (hotels): `name text+keyword`, `city keyword`, `tags keyword`, `priceFrom scaled_float`.

### 5) Data & Indexing
- Oracle aligned schema (dev: H2 Oracle mode).  
- Index: `IDX_RES_HOTEL (HOTEL_ID, CHECK_IN, CHECK_OUT)`.  
- Always use bind variables; verify plans with `DBMS_XPLAN`.

### 6) Resilience & Backpressure
- Resilience4j: Retry(3, backoff), CircuitBreaker(failureRateThreshold=50, slidingWindow=50) on ES/Kafka calls.
- Nginx: 
limit_req_zone $binary_remote_addr zone=rez:10m rate=200r/s;
server {
  listen 8081;
  location /api/reservations { limit_req zone=rez burst=100 nodelay; proxy_pass http://app:8080; }
  location /api/search { add_header Cache-Control "public, max-age=60"; proxy_pass http://app:8080; }
}

### 7) Observability & Logging
- Micrometer Prometheus exporter at /actuator/prometheus; scrape every 5s.
- Grafana panels: request rate, p95/p99 by URI, error %, JVM memory/threads; (optional) Kafka lag.
- Structured JSON logs: include traceId, uri, status, elapsedMs; no PII.

### 8) Performance Verification
- k6 search:
export let options={stages:[{duration:'30s',target:800},{duration:'60s',target:800},{duration:'20s',target:0}],
thresholds:{http_req_duration:['p(95)<200']}}; export default()=>http.get('http://localhost:8081/api/search?city=Tokyo');

- k6 reservations:
export let options={stages:[{duration:'30s',target:150},{duration:'60s',target:150},{duration:'20s',target:0}],
thresholds:{http_req_duration:['p(95)<350']}}; export default()=>http.post('http://localhost:8081/api/reservations',
JSON.stringify({userId:'u1',hotelId:'h1',checkIn:'2025-09-01',checkOut:'2025-09-03',totalPrice:199.99}),
{headers:{'Content-Type':'application/json','Idempotency-Key':`${__VU}-${__ITER}`}} );

### 9) CI/CD Discipline
- Jenkins stages: build → test → sonar (stub) → docker build.
- Block merge if SLO thresholds fail or anchors drift.
- Pitfalls & Anti patterns
- Blind retries without idempotency key.
- Changing TTLs or Nginx limits without ADR update.
- Exposing JPA entities in controllers.
- Large ES payloads (no pagination).

##Cascade cues
“Refuse merge if TTL=60s (search) or TTL=600s (idempotency) is missing.”
“Add slice tests for cache miss/hit and 409 duplicate idempotency.”
“Attach p95/p99 panels and k6 results to the PR description.”