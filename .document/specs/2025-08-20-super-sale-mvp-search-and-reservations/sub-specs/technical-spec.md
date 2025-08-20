# Technical Spec — Super-Sale MVP: Search & Reservations

## API Contracts (summary)
- GET /api/search
  - Query: q (optional), city (optional). Require at least one. page (default 0), size (default 10, max 20)
  - Cache: Redis cache-aside, TTL 60s, keys search:q:{normalized} or search:city:{city}
  - Response: 200 → [{ id, name, city, tags[], priceFrom }]
- POST /api/reservations
  - Headers: Idempotency-Key (required)
  - Body: { userId, hotelId, checkIn(YYYY-MM-DD), checkOut(YYYY-MM-DD), totalPrice }
  - Flow: Redis SET idem:{key} <uuid> NX EX 600 → if set, proceed; else 409
  - Tx: Persist reservation; after commit publish Kafka event → 201
  - Errors: 400 validation, 409 duplicate, 500
- GET /api/reservations/{id}
  - Response: 200 { reservation } or 404

All errors use RFC7807 shape via @RestControllerAdvice.

## DTO Validation
- @NotBlank userId, hotelId
- @Pattern YYYY-MM-DD for dates
- @Positive totalPrice
- Controller enforces q or city present for search

## Data Model & DDL
- Table: RESERVATIONS
  - ID VARCHAR2(36) PK
  - USER_ID, HOTEL_ID (NOT NULL)
  - CHECK_IN DATE, CHECK_OUT DATE (NOT NULL)
  - TOTAL_PRICE NUMBER(12,2) NOT NULL
  - STATUS VARCHAR2(20) DEFAULT 'CREATED' NOT NULL
  - CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP
- Index: IDX_RES_HOTEL (HOTEL_ID, CHECK_IN, CHECK_OUT)

See: ../database-schema.md for full DDL.

## Elasticsearch (hotels index)
```json
{ "mappings": { "properties": {
  "name": {"type":"text","fields":{"kw":{"type":"keyword"}}},
  "city": {"type":"keyword"},
  "tags": {"type":"keyword"},
  "priceFrom": {"type":"scaled_float","scaling_factor":100}
}}, "settings": {"number_of_shards":1, "number_of_replicas":0} }
```

## Redis Keys & TTLs
- search:city:{city} → TTL 60s
- search:q:{normalized} → TTL 60s
- idem:{Idempotency-Key} → TTL 600s

## Messaging
- Kafka/Redpanda topic: reservations.created
- Event (example):
```json
{
  "eventType": "reservations.created",
  "reservationId": "<uuid>",
  "userId": "u1",
  "hotelId": "h1",
  "checkIn": "2025-09-01",
  "checkOut": "2025-09-03",
  "totalPrice": 199.99,
  "createdAt": "2025-08-20T08:00:00Z"
}
```

## Nginx Gateway (8081)
```nginx
limit_req_zone $binary_remote_addr zone=rez:10m rate=200r/s;
server {
  listen 8081;
  location /api/reservations { limit_req zone=rez burst=100 nodelay; proxy_pass http://app:8080; }
  location /api/search { add_header Cache-Control "public, max-age=60"; proxy_pass http://app:8080; }
}
```

## Prometheus Scrape
```yaml
global: { scrape_interval: 5s }
scrape_configs:
  - job_name: 'spring-app'
    metrics_path: /actuator/prometheus
    static_configs: [{ targets: ['app:8080'] }]
```

## Resilience
- Apply Resilience4j to ES/Kafka clients
  - Retry: 3 attempts, exponential backoff
  - CircuitBreaker: failureRateThreshold=50, slidingWindowSize=50

## k6 Performance Tests
- Search (`/api/search`):
```js
import http from 'k6/http';
export let options={stages:[{duration:'30s',target:800},{duration:'60s',target:800},{duration:'20s',target:0}],
thresholds:{http_req_duration:['p(95)<200']}};
export default()=>http.get('http://localhost:8081/api/search?city=Tokyo');
```
- Reservations (`/api/reservations`):
```js
import http from 'k6/http';
export let options={stages:[{duration:'30s',target:150},{duration:'60s',target:150},{duration:'20s',target:0}],
thresholds:{http_req_duration:['p(95)<350']}};
export default()=>http.post('http://localhost:8081/api/reservations',
JSON.stringify({userId:'u1',hotelId:'h1',checkIn:'2025-09-01',checkOut:'2025-09-03',totalPrice:199.99}),
{headers:{'Content-Type':'application/json','Idempotency-Key':`${__VU}-${__ITER}`}} );
```

## Observability & Logging
- Endpoints: /api/health, /actuator/prometheus
- Dashboards: p95/p99 by URI, request rate, error %, JVM memory/threads
- Logging: JSON structured; include traceId, uri, status, elapsedMs; no PII

## Acceptance & Tests
- Exact TTLs (60s/600s), topic name, Nginx and Prometheus configs present
- Slice tests: cache miss/hit; 409 on duplicate idempotency
- Coverage ≥80% on changed code
