---
trigger: always_on
---

# SuperSale Mini Booking API — Tech Stack (Pinned & Allowed)

## Purpose
Pin the stack to **stable, reproducible** versions that meet PRD SLOs and simplify local demos.

## When to Use
- Project setup and upgrades.
- Reviewing dependencies in PRs.

## Inputs
- PRD.md canonical ports, endpoints, SLOs.
- Allowed library list.

## Outputs
- Pinned dependencies, Docker images, and tool choices with clear “allow/avoid” guidance.

## Acceptance Criteria
- `docker compose up -d` brings up app + infra on fixed ports.
- Spring Boot app builds with pinned versions and exposes `/actuator/prometheus`.

---

## Languages & Build
- **Java:** 21  
- **Spring Boot:** 3.5.4  
- **Maven:** 3.9+ (wrapper encouraged)

## Data Stores
- **RDBMS:** Oracle (prod alignment) / **Dev:** H2 (Oracle mode)
- **Cache:** Redis **7.x**
- **Search:** Elasticsearch **8.x**

## Messaging
- **Kafka:** Apache Kafka; **Dev:** Redpanda **v24.1+**
- **Topic:** `reservations.created` (partitions=3 dev, retention=24h)

## Gateway
- **Nginx:** `1.27-alpine`
- Rate limiting and cache headers as in PRD.

## Observability
- **Prometheus:** `v2.x` (scrape every 5s at `/actuator/prometheus`)
- **Grafana:** `v10.x` (import dashboard JSON with p95/p99, error %, JVM)

## CI/CD & Quality
- **Jenkins** declarative pipeline (build → test → sonar → docker build)
- **SonarQube** (local stub acceptable); coverage gate **≥80% changed code**

## Allowed Libraries
- Spring Web, Validation, Data JPA, Data Redis, Data Elasticsearch
- Spring for Kafka, Micrometer Prometheus
- Resilience4j, MapStruct (optional)
- Tests: JUnit 5, Spring Test, spring-kafka-test

## Avoid
- Unpinned Docker tags (use explicit versions).
- DIY circuit breakers/retries (use Resilience4j).
- Exposing PII in logs.

## Port Map (local)
- App **8080**, Nginx **8081**, Redis **6379**, ES **9200**, Kafka **9092**, Prometheus **9090**, Grafana **3000**

## Canonical Snippets
### Nginx
limit_req_zone $binary_remote_addr zone=rez:10m rate=200r/s;
server {
  listen 8081;
  location /api/reservations { limit_req zone=rez burst=100 nodelay; proxy_pass http://app:8080; }
  location /api/search { add_header Cache-Control "public, max-age=60"; proxy_pass http://app:8080; }
}

### Prometheus
global: { scrape_interval: 5s }
scrape_configs:
  - job_name: 'spring-app'
    metrics_path: /actuator/prometheus
    static_configs: [{ targets: ['app:8080'] }]
	
### OracleAligned DDL
CREATE TABLE RESERVATIONS(
  ID VARCHAR2(36) PRIMARY KEY,
  USER_ID VARCHAR2(36) NOT NULL,
  HOTEL_ID VARCHAR2(36) NOT NULL,
  CHECK_IN DATE NOT NULL,
  CHECK_OUT DATE NOT NULL,
  TOTAL_PRICE NUMBER(12,2) NOT NULL,
  STATUS VARCHAR2(20) DEFAULT 'CREATED' NOT NULL,
  CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP
);
CREATE INDEX IDX_RES_HOTEL ON RESERVATIONS(HOTEL_ID, CHECK_IN, CHECK_OUT);

### Elasticsearch Mapping
{ "mappings": { "properties": {
  "name": {"type":"text","fields":{"kw":{"type":"keyword"}}},
  "city": {"type":"keyword"},
  "tags": {"type":"keyword"},
  "priceFrom": {"type":"scaled_float","scaling_factor":100}
}}, "settings":{"number_of_shards":1,"number_of_replicas":0} }

### k6 Thresholds (reference)
Search: p95 < 200ms @ 800 rps
Reservations: p95 < 350ms @ 150 rps

### Pitfalls & Anti patterns
Mixing dev/prod ports or TTLs.
Overscoping infra (e.g., full Kong) for a 3‑hour demo.
Missing metrics/alerts before perf tests.

###Cascade cues
“Reject dependency changes not on allowed list or without explicit version.”
“Ensure port map and Prometheus path match PRD before running codegen.”
“Insert Nginx and Prometheus snippets verbatim into stitched context.”