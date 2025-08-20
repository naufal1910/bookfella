# Super-Sale MVP (Lite Spec)

- Purpose: Minimal production-ready booking API for Super-Sale.
- Endpoints:
  - GET /api/search (Redis cache TTL 60s → ES fallback)
  - POST /api/reservations (Idempotency-Key; Redis SET NX EX 600; 201/409)
  - GET /api/reservations/{id}
- SLOs: search p95 < 200 ms @ 800 rps; reservations p95 < 350 ms @ 150 rps
- Events: Publish to Kafka topic reservations.created after DB commit
- Gateway: Nginx limit 200 r/s (burst=100, nodelay); add Cache-Control: public, max-age=60 for search
- Observability: /api/health, /actuator/prometheus; dashboards for p95/p99, error %, JVM
- Data: Oracle-aligned schema; ES hotels index; Redis 7 keys search:* and idem:*
- Acceptance: TTLs exact (60s/600s), topic exact, /actuator/prometheus exposed, k6 thresholds pass, ≥80% coverage on changed code
- Demo Plan: Run via Nginx 8081; warm cache; run k6 scripts; show Grafana panels and Kafka events
