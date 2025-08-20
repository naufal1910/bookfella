# Super-Sale MVP: Search & Reservations

Date: 2025-08-20
Owner: Booking API Team

## Problem & Context
Deliver a minimal, production-ready booking API that withstands Super-Sale load while keeping the system fast, safe, and observable. Scope includes high-RPS cached hotel search and idempotent reservation writes with event publication and gateway rate limiting.

## Goals (SLOs)
- /api/search: p95 < 200 ms @ 800 rps (after warm cache)
- /api/reservations: p95 < 350 ms @ 150 rps
- Enforce idempotency for writes (Redis SET NX EX 600s); duplicates → 409
- Publish Kafka event to topic "reservations.created" after DB commit
- Expose health and Prometheus metrics

## In Scope
- GET /api/search with Redis cache-aside (TTL 60s); fallback to Elasticsearch 8 "hotels" index
- POST /api/reservations with Idempotency-Key (Redis SET NX EX 600) → 201 or 409 on duplicate
- GET /api/reservations/{id}
- Kafka publish to topic reservations.created after successful DB transaction
- Oracle-aligned schema (H2 Oracle mode for dev)
- Nginx gateway: rate limit 200 r/s (burst=100, nodelay); add Cache-Control: public, max-age=60 for search
- Observability: /api/health, /actuator/prometheus; structured logs
- k6 perf scripts with thresholds matching SLOs

## Out of Scope
- Payments/refunds; inventory sync; authz/authn; user profiles; multi-region DR

## APIs (Overview)
- GET /api/search?q=...|city=... [&page,&size≤20]
  - Cache key: search:q:{normalized} or search:city:{city}; TTL 60s
  - Response: list of hotels (name, city, tags, priceFrom)
- POST /api/reservations (Idempotency-Key required)
  - On first occurrence: reserve in a single DB tx; after commit publish reservations.created; return 201
  - On duplicate key: return 409
- GET /api/reservations/{id}: retrieve reservation by id

## Data & Integrations
- RDBMS: Oracle-aligned (H2 in dev). Table RESERVATIONS; index IDX_RES_HOTEL (HOTEL_ID, CHECK_IN, CHECK_OUT)
- Cache: Redis 7
  - Keys: search:city:{city}, search:q:{normalized} (TTL 60s); idem:{key} (TTL 600s)
- Search: Elasticsearch 8 hotels index (mapping: name text+keyword, city keyword, tags keyword, priceFrom scaled_float)
- Messaging: Kafka/Redpanda topic reservations.created
- Gateway: Nginx 1.27-alpine on 8081 → app 8080
- Observability: Prometheus scrape /actuator/prometheus every 5s; Grafana dashboards

## Performance & Resilience
- Cache-aside for search; page size ≤ 20
- Resilience4j on ES/Kafka: Retry(3, backoff), CircuitBreaker(50% failure, window=50)
- Nginx: limit 200 r/s (burst=100 nodelay) for POST /api/reservations; add Cache-Control for /api/search

## Observability & Logging
- Endpoints: /api/health, /actuator/prometheus
- Metrics: request rate; p95/p99 by URI; error rate; JVM health
- Logs: JSON fields include traceId, uri, status, elapsedMs; no PII

## Acceptance Criteria (merge blockers)
- Redis TTLs exact: 60s (search), 600s (idempotency)
- Kafka topic exactly: reservations.created
- Nginx gateway: limit_req 200 r/s burst=100 nodelay; Cache-Control: public, max-age=60 for /api/search
- Prometheus at /actuator/prometheus; scrape every 5s
- SLOs proven with k6 scripts; attach results and dashboard screenshots
- DTO validation and RFC7807 errors; no entity exposure
- Tests: ≥80% coverage changed code; slice tests for cache miss/hit and 409 duplicate idempotency

## Milestones
- M1: Spec + API contracts + DDL + mappings
- M2: Controllers/services/repos + caching/idempotency + outbox publish
- M3: Nginx + Prometheus + Grafana + k6 perf
- M4: Tests and docs; acceptance sign-off
