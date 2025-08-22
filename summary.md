Here’s a short executive summary version of your PRD — a 1–2 page recruiter-facing doc, in Markdown format. It keeps the essence (mission, key features, roadmap, tech stack, success metrics) without the long technical detail.

# Super-Sale Mini Booking API — Executive Summary

## Mission
Rakuten Travel must handle massive traffic spikes during quarterly **Super Sale events** (≥800 reads/sec for search, ≥150 writes/sec for reservations).  
This project demonstrates a **production-credible backend** that proves capability at that scale, using modern Java/Spring practices with resilient architecture and observability.

**Core Idea:**  
A compact booking API that supports **fast hotel search** and **reliable reservations** under burst load.

**Key Features:**
- **Search API** — low-latency hotel search (Redis cache + Elasticsearch index).
- **Reservation API** — transactional reservation creation with idempotency & Kafka outbox.
- **Observability** — Prometheus metrics + Grafana dashboards for p95/p99 latency & error rates.
- **Gateway Control** — Nginx with rate limiting (200 r/s for POST) and cache headers.
- **CI/CD Ready** — Jenkins pipeline stub, SonarQube checks, k6 performance scripts.

**Target Audience:**  
- **Recruiters/Engineering reviewers** — to evaluate technical depth, architecture decisions, and production readiness.  
- **Demo operators** — quick “spin up and test” showcase of system capability.

**Top Use Cases:**
1. Search hotels by city or keyword (`/api/search`).
2. Create a reservation reliably, even under retry storms (`/api/reservations`).
3. Fetch a reservation by ID (`/api/reservations/{id}`).

---

## Architecture at a Glance


Client → Nginx (8081, rate-limit/cache) → Spring Boot App (8080)
├─ Redis (6379) — search cache & idempotency keys
├─ Elasticsearch (9200) — hotel search index
├─ H2 (Oracle mode) — reservations DB (prod: Oracle)
└─ Kafka/Redpanda (9092) — reservation outbox events
→ Prometheus (9090) + Grafana (3000) for observability


**Patterns used:**  
- Cache-aside Redis (TTL 60s) for hot search results.  
- Reservation idempotency via `Idempotency-Key` in Redis (TTL 600s).  
- Outbox pattern with Kafka topic `reservations.created`.  
- Backpressure via Nginx `limit_req`.  
- Observability-first (Prometheus scrape, Grafana panels).

---

## Roadmap (90 Days)
- **M0 (Week 1):** Endpoints + Docker Compose stack + seeded ES index.  
- **M1 (Weeks 2–3):** k6 load tests passing thresholds; Grafana dashboards online.  
- **M2 (Weeks 4–6):** CI/CD polish; coverage ≥80%; ADRs & docs complete.  
- **Stretch:** MongoDB audit sink, Kong gateway PoC, i18n ES analyzers.

---

## Tech Stack
- **Backend:** Java 17, Spring Boot 3.3, Maven  
- **Database:** Oracle (prod), H2 Oracle mode (dev)  
- **Cache/Search:** Redis 7, Elasticsearch 8  
- **Messaging:** Kafka (dev: Redpanda)  
- **Gateway:** Nginx 1.27 (Kong optional future)  
- **Observability:** Prometheus 2.x, Grafana 10.x  
- **CI/CD:** Jenkins (Declarative pipeline), SonarQube  

---

## Success Metrics
- **Search API:** p95 < 200 ms @ 800 rps (warm cache).  
- **Reservation API:** p95 < 350 ms @ 150 rps.  
- Error rate < 1% under sustained load.  
- Cache hit rate ≥70% after warmup.  
- CI pass rate ≥95%, coverage ≥80% on changed code.

---

## Why It Matters
This project directly addresses **Rakuten Travel TDD’s high-scale requirements** by showing mastery of:  
- Java/Spring at scale  
- Oracle + Redis + Elasticsearch indexing  
- Messaging via Kafka  
- Backpressure and caching strategies  
- Observability with p95/p99 dashboards  

It proves ability to design and implement backend systems that stay **reliable, observable, and performant** under Super Sale conditions.