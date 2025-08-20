---
trigger: always_on
---

# Super Sale Mini Booking API — Code Style (Java 21 / Spring Boot 3.5)

## Purpose
Ensure consistent, reviewable Java code optimized for **highload APIs** and easy troubleshooting.

## When to Use
- During implementation and PR review.
- When scaffolding new endpoints, services, or repos.

## Inputs
- PRD endpoints & SLOs
- ADRs for TTLs, gateway, outbox
- Team build tools (Maven, Jenkins, Sonar)

## Outputs
- Consistent package layout, DTO/entity boundaries, error handling, and tests that pass CI.

## Acceptance Criteria
- Builds with `mvn -q -DskipTests=false verify`.
- Test coverage **≥80% on changed code**.
- Controllers never expose entities; all inputs validated; errors RFC7807.

---

## Package & Layout
com.bookfella.booking
├─ config/ # Redis, Kafka, Security, Observability, Resilience4j
├─ domain/ # JPA entities (Reservation), ES docs (Hotel)
├─ dto/ # Request/Response models
├─ repo/ # Spring Data JPA/ES repositories
├─ service/ # Business logic (SearchService, ReservationService)
├─ web/ # Controllers + @ControllerAdvice
└─ util/ # Mappers, key utils, normalization

## Naming & Nullability
- Classes: PascalCase; members: camelCase.
- Validate at boundaries (`@Valid`); forbid `null` in DTOs; use `Optional` in service boundaries.

## DTO, Validation & Errors
- **Never** return entities from controllers.
- Example DTO & validation:
record CreateReservationRequest(
  @NotBlank String userId,
  @NotBlank String hotelId,
  @Pattern(regexp="\\d{4}-\\d{2}-\\d{2}") String checkIn,
  @Pattern(regexp="\\d{4}-\\d{2}-\\d{2}") String checkOut,
  @Positive BigDecimal totalPrice){}
  
## Central errors:
@RestControllerAdvice
class ApiErrors {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> badReq(...) { /* return RFC7807 payload */ }
}

## Controller Style
Thin controllers; delegate to services; validate query invariants:

@GetMapping("/api/search")
ResponseEntity<?> search(@RequestParam(required=false) String q,
                         @RequestParam(required=false) String city) {
  if ((q==null || q.isBlank()) && (city==null || city.isBlank())) throw new BadRequest("Provide q or city");
  return ResponseEntity.ok(service.search(q, city));
}

## Services & Transactions
@Transactional at service layer for writes.

Idempotency check first; write; publish outbox after commit.

## Caching & Keys
Redis keys:

search:city:{city}

search:q:{normalized}

idem:{key}

TTLs: 60s (search), 600s (idempotency).

## Logging (Structured, No PII)
SLF4J with JSON layout fields: traceId, method, uri, status, elapsedMs.

Use INFO sparingly; rely on metrics for high level health.

## Resilience
Resilience4j beans: Retry(3), CircuitBreaker(50%/window 50), TimeLimiter.

Apply to ES/Kafka clients.

## Tests
Unit (*ServiceTest) using JUnit 5 and Mockito.

Web slice (@WebMvcTest) with MockMvc for controllers.

JPA slice (@DataJpaTest) with H2 (Oracle mode).

Integration optional: spring-kafka-test for outbox publish.

## PR Checklist
 DTOs validated; entities not exposed.

 Redis keys & TTLs match PRD (60s/600s).

 Nginx headers/limits present in config.

 Metrics at /actuator/prometheus accessible.

 k6 scripts committed; thresholds pass locally.

 ADR updated if defaults changed.

## Formatting
UTF-8, LF; Java 21; one class per file; < 400 LOC per class preferred.

Static imports only for assertions.

## Pitfalls & Anti-patterns
Business logic in controllers.

Silent catch and ignore.

Hardcoded timeouts without metrics.

## Cascade cues
“Reject PR if entity leaked to API or TTLs mismatch.”

“Require tests showing 409 on duplicate Idempotency Key.”

“Block merge if /actuator/prometheus not scraped.”