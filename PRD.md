# Super-Sale Mini Booking API — PRD

## 0. Document Control

* **Owner:** Backend Architect — Big Travel Company TDD Portfolio Project
* **Version:** 1.0
* **Last Updated:** August 19, 2025 (Asia/Jakarta, UTC+7)
* **Links:**

  * Repo: [https://github.com/example/mini-booking-api](https://github.com/example/mini-booking-api)
  * CI Job: `mini-booking-api-ci`
  * Dashboards: Grafana at `http://localhost:3000` (import `infra/grafana-dashboard.json`)

---

## 1. Executive Summary

Big Travel Company faces quarterly “Super Sale” bursts that demand extremely fast search and reliable, high-throughput reservations. This project delivers a compact but production-credible backend proving capability at those loads: **READ-heavy search** via **Elasticsearch + Redis** and **WRITE-heavy reservations** via **Oracle-style RDBMS + Kafka outbox**, fronted by **Nginx** for backpressure and observed via **Prometheus/Grafana**. It maps directly to the JD: Java 17, Spring Boot 3.3, Oracle, Redis, Elasticsearch, Kafka, Nginx/Kong, Prometheus/Grafana, Jenkins/Sonar; and the scale profile **≥800 reads/sec** and **≥150 writes/sec**.

---

## 2. Goals & Non-Goals

### Goals

* Achieve demo SLOs:

  * `/api/search` **p95 < 200 ms** at 800 rps (after cache warmup).
  * `/api/reservations` **p95 < 350 ms** at 150 rps.
* Demonstrate operational patterns: cache-aside (TTL 60s), idempotency (Redis `SET NX`, TTL 600s), circuit breaking/retries, rate limiting (Nginx `limit_req`), and structured logging.
* Provide ready-to-run observability (Actuator→Prometheus→Grafana) with p95/p99 and error-rate panels.
* Provide CI stubs (Jenkins, Sonar) and short load scripts (k6).

### Non-Goals

* Payments, cancellations, post-booking workflows.
* Full production authZ/SSO and compliance scope.
* Globalization analyzers and multi-currency pricing (future work).

---

## 3. Users & Use Cases

**Users:**

* Recruiters/engineers evaluating system design/operability.
* Demo operators running load tests and reading dashboards.

**Top Use Cases:**

1. **Search hotels** by free text or city with low tail latency (cache → ES).
2. **Create reservation** reliably under bursty load (transaction + outbox + idempotency).
3. **Fetch reservation** by ID for confirmation/view.

---

## 4. Requirements

### 4.1 Functional Requirements (FR)

**FR-1 — Search Hotels**

* Users can search by `q` (name) or `city` (one required).
* Cache-aside Redis with TTL 60s; on miss, query ES `hotels` index, then set cache.
  **Acceptance (G/W/T):**
* **Given** Redis is warm for `city=Tokyo`, **when** GET `/api/search?city=Tokyo`, **then** response is 200 with JSON list and latency < 50 ms on laptop (cache hit).
* **Given** cache miss for `city=Sapporo`, **when** GET `/api/search?city=Sapporo`, **then** first call queries ES and second call hits cache.

**FR-2 — Create Reservation**

* POST with `Idempotency-Key` header; transactional write; publish `reservations.created` to Kafka after commit.
  **Acceptance:**
* **Given** a new `Idempotency-Key`, **when** POST `/api/reservations`, **then** 201 with persisted record and event published.
* **Given** the same `Idempotency-Key` sent again within 10 minutes, **when** POST, **then** 409 Conflict (duplicate).

**FR-3 — Get Reservation by ID**

* Retrieve persisted reservation.
  **Acceptance:**
* **Given** an existing reservation ID, **when** GET `/api/reservations/{id}`, **then** 200 with DTO; non-existent returns 404.

**FR-4 — Health & Readiness**

* `/api/health` returns `{status: "ok"}`; Actuator `/actuator/health`, `/actuator/prometheus`.
  **Acceptance:**
* **When** hitting `/api/health`, **then** 200; Prometheus can scrape metrics.

### 4.2 Non-Functional Requirements (NFR)

* **NFR-1 Performance:** Search p95 < 200 ms @ 800 rps; Reservation p95 < 350 ms @ 150 rps (dev laptop, warmed cache).
* **NFR-2 Reliability:** Idempotency via Redis `SET NX` 600s; Nginx `limit_req` 200 r/s for POST; retries (3) + circuit breaker on ES.
* **NFR-3 Security:** Input validation (Bean Validation), minimal auth for POST (Basic/JWT stub), no PII in logs.
* **NFR-4 Observability:** Micrometer Prometheus metrics; Grafana panels (p95/p99, error rate, JVM); optional Kafka lag.
* **NFR-5 Scalability:** ES pagination (page size ≤20), single-shard dev; cache hit rate ≥70% after warmup.
* **NFR-6 Maintainability:** New/changed code coverage ≥80%; clear ADRs; pinned Docker tags.

---

## 5. Scope (In / Out)

**In:** `/api/search`, `/api/reservations` (create/get), cache, rate limits, metrics, k6 load, Jenkins/Sonar stubs, ADRs.
**Out:** Payments, cancellation flows, user accounts/points, i18n analyzers, multi-currency.

---

## 6. Architecture Overview

### 6.1 ASCII Diagram

```
[Client]
   |
[NGINX 8081] --(rate-limit POST, cache headers GET)---------------------.
   |                                                                    |
   v                                                                    |
[Spring Boot App 8080]  <Actuator/Prom>                                 |
   | \__________\__________\__________\                                 |
   |            |          |          |                                  |
 [Redis 6379]  [ES 9200] [H2(Oracle mode) RDBMS] [Kafka/Redpanda 9092]   |
                                 |         ^                              |
                                 '-----(commit)-> publish outbox --------'
   |
[Prometheus 9090] -> [Grafana 3000]
```

### 6.2 Key Flows

* **READ:** `/api/search` → Redis key `search:city:{city}` or `search:q:{norm}` (TTL 60s). Miss → ES query `hotels`, set cache, return.
* **WRITE:** `/api/reservations` → validate + `Idempotency-Key` check (Redis `SET NX`, TTL 600s) → DB tx save → publish `reservations.created` → 201.

---

## 7. API Design (with concrete examples)

### GET `/api/search?q|city`

* **Params:** `q` (string, optional), `city` (string, optional) — **one required**.
* **Headers (response):** `Cache-Control: public, max-age=60`
* **200 Example:**

```json
[
  {"id":"h1","name":"Shinjuku Comfort","city":"Tokyo","tags":["business","metro"],"priceFrom":85.0},
  {"id":"h2","name":"Osaka Riverside","city":"Osaka","tags":["family","view"],"priceFrom":72.0}
]
```

* **400 Example:**

```json
{ "type":"https://httpstatuses.com/400","title":"Bad Request","detail":"Provide q or city","status":400 }
```

* **curl:**

```bash
curl "http://localhost:8081/api/search?city=Tokyo"
curl "http://localhost:8081/api/search?q=comfort"
```

### POST `/api/reservations`

* **Headers (request):** `Idempotency-Key: demo-123`
* **Body (JSON):**

```json
{
  "userId":"u1",
  "hotelId":"h1",
  "checkIn":"2025-09-01",
  "checkOut":"2025-09-03",
  "totalPrice":199.99
}
```

* **201 Example:**

```json
{
  "id":"1f0c2d88-6d1b-4b36-9136-2b7f5a3322f1",
  "userId":"u1",
  "hotelId":"h1",
  "checkIn":"2025-09-01",
  "checkOut":"2025-09-03",
  "totalPrice":199.99,
  "status":"CREATED",
  "createdAt":"2025-08-19T04:31:00Z"
}
```

* **409 Duplicate Example:**

```json
{ "type":"https://httpstatuses.com/409","title":"Conflict","detail":"Duplicate request","status":409 }
```

* **Validation Rules:** `userId`,`hotelId` non-blank; `checkIn` < `checkOut`; `totalPrice` > 0; ISO date `yyyy-MM-dd`.
* **curl:**

```bash
curl -X POST http://localhost:8081/api/reservations \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-123" \
  -d '{"userId":"u1","hotelId":"h1","checkIn":"2025-09-01","checkOut":"2025-09-03","totalPrice":199.99}'
```

### GET `/api/reservations/{id}`

* **200 Example:**

```json
{
  "id":"1f0c2d88-6d1b-4b36-9136-2b7f5a3322f1",
  "userId":"u1",
  "hotelId":"h1",
  "checkIn":"2025-09-01",
  "checkOut":"2025-09-03",
  "totalPrice":199.99,
  "status":"CREATED",
  "createdAt":"2025-08-19T04:31:00Z"
}
```

* **404 Example:**

```json
{ "type":"https://httpstatuses.com/404","title":"Not Found","detail":"Reservation not found","status":404 }
```

* **curl:**

```bash
curl http://localhost:8081/api/reservations/1f0c2d88-6d1b-4b36-9136-2b7f5a3322f1
```

---

## 8. Data Model & Persistence

### 8.1 RDBMS (Oracle-aligned; dev H2 Oracle mode)

```sql
CREATE TABLE RESERVATIONS (
  ID          VARCHAR2(36)      PRIMARY KEY,
  USER_ID     VARCHAR2(36)      NOT NULL,
  HOTEL_ID    VARCHAR2(36)      NOT NULL,
  CHECK_IN    DATE              NOT NULL,
  CHECK_OUT   DATE              NOT NULL,
  TOTAL_PRICE NUMBER(12,2)      NOT NULL,
  STATUS      VARCHAR2(20)      DEFAULT 'CREATED' NOT NULL,
  CREATED_AT  TIMESTAMP         DEFAULT SYSTIMESTAMP
);

CREATE INDEX IDX_RES_HOTEL ON RESERVATIONS (HOTEL_ID, CHECK_IN, CHECK_OUT);
```

**Indexing rationale:** Reservation lookups by `(HOTEL_ID, CHECK_IN, CHECK_OUT)` avoid full scans during spikes. Always use bind variables:

```sql
EXPLAIN PLAN FOR
SELECT * FROM RESERVATIONS
 WHERE HOTEL_ID = :hotelId
   AND CHECK_IN  >= :fromDate
   AND CHECK_OUT <= :toDate;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY());
```

### 8.2 Elasticsearch (search index)

**Mapping:**

```json
{
  "mappings": {
    "properties": {
      "name":      { "type":"text", "fields": { "kw": { "type":"keyword" } } },
      "city":      { "type":"keyword" },
      "tags":      { "type":"keyword" },
      "priceFrom": { "type":"scaled_float", "scaling_factor": 100 }
    }
  },
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 }
}
```

**Seed NDJSON:**

```ndjson
{ "index": { "_index": "hotels", "_id": "h1" } }
{ "name":"Shinjuku Comfort","city":"Tokyo","tags":["business","metro"],"priceFrom":85 }
{ "index": { "_index": "hotels", "_id": "h2" } }
{ "name":"Osaka Riverside","city":"Osaka","tags":["family","view"],"priceFrom":72 }
{ "index": { "_index": "hotels", "_id": "h3" } }
{ "name":"Sapporo Snow Inn","city":"Sapporo","tags":["ski","cozy"],"priceFrom":90 }
```

**Load commands:**

```bash
curl -X PUT "http://localhost:9200/hotels" -H "Content-Type: application/json" --data-binary @mapping.json
curl -X POST "http://localhost:9200/_bulk" -H "Content-Type: application/x-ndjson" --data-binary @search/es-seed-hotels.json
```

### 8.3 Redis Keys

* Search cache: `search:city:{city}`, `search:q:{normalized}` (TTL **60s**)
* Idempotency: `idem:{key}` (TTL **600s**)

---

## 9. Messaging

* **Topic:** `reservations.created`
* **Partitions (dev):** 3; **Retention:** 24h; **Acks:** `all` (default recommended).
* **Event Schema (JSON):**

```json
{
  "id":"1f0c2d88-6d1b-4b36-9136-2b7f5a3322f1",
  "userId":"u1",
  "hotelId":"h1",
  "totalPrice":199.99,
  "createdAt":"2025-08-19T04:31:00Z"
}
```

* **Delivery semantics:** At-least-once. Consumers must be idempotent (use event key `id` or dedupe store).

---

## 10. Gateway & Backpressure (Nginx)

```nginx
worker_processes auto;
events { worker_connections 1024; }
http {
  limit_req_zone $binary_remote_addr zone=rez:10m rate=200r/s;

  upstream app { server app:8080; }

  server {
    listen 8081;

    location /api/reservations {
      limit_req zone=rez burst=100 nodelay;
      proxy_pass http://app;
    }

    location /api/search {
      add_header Cache-Control "public, max-age=60";
      proxy_pass http://app;
    }

    location / { proxy_pass http://app; }
  }
}
```

---

## 11. Observability

**Prometheus scrape config (`infra/prometheus.yml`):**

```yaml
global:
  scrape_interval: 5s
scrape_configs:
  - job_name: 'spring-app'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['app:8080']
```

**Grafana panels (dashboard JSON provided):**

* Request rate by URI (`http_server_requests_seconds_count`).
* Latency p95/p99 by URI (`histogram_quantile`).
* Error percentage by status class.
* JVM memory/threads.
* (Optional) Kafka consumer lag.

**Logging:** JSON-structured with fields `timestamp`, `traceId`, `uri`, `status`, `method`, `elapsedMs`; no PII.

---

## 12. Performance Plan

**k6 — Search (800 rps):**

```js
import http from 'k6/http';
import { check, sleep } from 'k6';
export let options = {
  stages: [{duration:'30s',target:800},{duration:'60s',target:800},{duration:'20s',target:0}],
  thresholds: { http_req_duration: ['p(95)<200'] }
};
export default function() {
  const res = http.get('http://localhost:8081/api/search?city=Tokyo');
  check(res, { 'status is 200': r => r.status === 200 });
  sleep(0.1);
}
```

**k6 — Reservations (150 rps):**

```js
import http from 'k6/http';
import { check, sleep } from 'k6';
export let options = {
  stages: [{duration:'30s',target:150},{duration:'60s',target:150},{duration:'20s',target:0}],
  thresholds: { http_req_duration: ['p(95)<350'] }
};
export default function() {
  const headers = {'Content-Type':'application/json','Idempotency-Key':`${__VU}-${__ITER}`};
  const body = JSON.stringify({"userId":"u1","hotelId":"h1","checkIn":"2025-09-01","checkOut":"2025-09-03","totalPrice":199.99});
  const res = http.post('http://localhost:8081/api/reservations', body, { headers });
  check(res, { 'ok/dupe': r => [200,201,409].includes(r.status) });
  sleep(0.2);
}
```

**Warm-up:** Run a 10–20s ramp to fill Redis before measuring.
**Pass/Fail:** Thresholds above; error rate < 1% after warmup.

---

## 13. Security & Compliance

* Validate all inputs (Bean Validation).
* Minimal auth: protect POST (Basic or JWT stub); GETs public for demo.
* Rate limit POST; enforce idempotency.
* No sensitive data logged; redact headers and tokens if present.

---

## 14. Risks & Mitigations

| Risk                                       | Likelihood | Impact | Mitigation                                                         |
| ------------------------------------------ | ---------- | ------ | ------------------------------------------------------------------ |
| ES heap pressure under load                | Medium     | Medium | Single shard, small page size (≤20), simple mapping, warm cache    |
| Kafka single-node durability (dev)         | Medium     | Low    | Short retention (24h), document prod settings, idempotent consumer |
| Oracle index gaps (prod)                   | Low        | High   | Composite indexes + EXPLAIN PLAN checks; bind variables            |
| Gateway misconfig throttling legit traffic | Medium     | Medium | Tested `nginx.conf`, staged bursts, monitor 429 rate               |
| Over-scope beyond MVP                      | High       | Medium | Strict feature cut; defer payments/i18n                            |

---

## 15. Release Plan & Milestones

* **M0 (Week 1):** Endpoints working; Compose stack up; ES seeded.
* **M1 (Weeks 2–3):** Meet k6 thresholds; add Grafana panels; tune cache/indexes.
* **M2 (Weeks 4–6):** CI pipeline stable, coverage ≥80%, ADRs updated; optional Mongo audit consumer.

---

## 16. Success Metrics & KPIs

* Search p95/p99 latency; Reservation p95 latency.
* Error rate < 1% during sustained load.
* Cache hit % (search) ≥70% after warmup.
* CI pass rate ≥95%; code coverage ≥80% on changed lines.

---

## 17. Open Questions

* None for MVP. Production variants (Oracle XE locally, Kong instead of Nginx, multi-shard ES, proper JWT issuer) are documented as future enhancements.

---

## 18. Appendices

### A. Demo Script

```bash
# 1) Start stack
docker compose up -d

# 2) Seed Elasticsearch
curl -H "Content-Type: application/json" -X PUT http://localhost:9200/hotels -d @infra/es-mapping.json || true
curl -H "Content-Type: application/x-ndjson" --data-binary @search/es-seed-hotels.json http://localhost:9200/_bulk

# 3) Smoke
curl http://localhost:8081/api/health
curl "http://localhost:8081/api/search?city=Tokyo"
curl -X POST http://localhost:8081/api/reservations \
  -H "Content-Type: application/json" -H "Idempotency-Key: demo-1" \
  -d '{"userId":"u1","hotelId":"h1","checkIn":"2025-09-01","checkOut":"2025-09-03","totalPrice":199.99}'

# 4) Metrics
open http://localhost:3000
```

### B. JD Mapping Table

| JD Requirement                    | Evidence in Project                                      |
| --------------------------------- | -------------------------------------------------------- |
| Java/Spring API on Linux          | Spring Boot 3.3 app, Dockerized, Compose                 |
| High-traffic search (≥800 qps)    | Redis cache + ES, k6 search script, cache headers        |
| High-throughput writes (≥150 rps) | Idempotent POST, RDBMS tx, Kafka outbox, k6 reservations |
| Oracle & indexing                 | Oracle-aligned DDL, composite index, EXPLAIN plan        |
| Redis/Mongo/ES                    | Redis cache, ES index (Mongo optional)                   |
| Kafka/MQ                          | `reservations.created` topic, producer code              |
| Gateway                           | Nginx rate limit + cache headers                         |
| Observability                     | Actuator→Prometheus→Grafana dashboard                    |
| CI/CD                             | Jenkinsfile + Sonar stub                                 |
| Team practices                    | ADRs, tests, coverage target, structured logging         |

### C. ADR Summary (initial set)

* **Cache TTL:** Redis 60s for search; cache-aside, headers `max-age=60`.
* **Idempotency:** `Idempotency-Key` with Redis `SET NX`, TTL 600s.
* **Gateway:** Nginx `limit_req` 200 r/s for POST, `burst=100`.
* **Outbox:** Publish `reservations.created` post-commit to Kafka (Redpanda in dev).
* **Dev DB:** H2 (Oracle mode) locally; Oracle DDL tracked.
* **SLOs:** Search p95 < 200 ms @ 800 rps; Reservations p95 < 350 ms @ 150 rps.

---
