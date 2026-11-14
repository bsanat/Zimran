# Sxodim USA — backend context for code agents

Product: AI personal leisure planner (MVP). A guest (no login) picks city, interests,
date range, budget and time of day; backend fetches REAL events from Ticketmaster
Discovery API, filters them, asks OpenAI to rank up to 5 and explain why, validates
the AI answer, returns JSON. Pilot city: New York only.

Stack: Java 21, Spring Boot 4.1.1 (Spring Framework 7, Jackson 3), spring-webmvc,
Data JPA, Validation, Flyway, PostgreSQL 18 (docker-compose.yml), Lombok, MapStruct 1.6,
springdoc-openapi. Build: ./mvnw. Base package: kz.astyq.zimran.

Package layout (feature-first):
core/        config, exception, i18n, model (BaseEntity, AuditableEntity), util, web
event/       Event domain model, EventId codec, events API (controller/service/dto)
ticketmaster/ Discovery API client, raw DTOs, normalizer, category mapping
planner/     plan request/response, filtering pipeline, fallback ranking, POST /api/plan
ai/          OpenAI client, prompt builder, response schema + validator
account/     (P2) auth, profiles, user preferences

Non-negotiable rules:
- Never invent events, URLs, prices, dates, addresses or images. Every field returned
  to the client comes from Ticketmaster data, never from LLM text.
- Unknown price => priceMin/priceMax = null, priceKnown = false. Never treat as free/$0.
- AI may only choose eventIds from the candidate set we sent; anything else is dropped.
- User notes and external event descriptions are untrusted text (prompt injection).
- Secrets (TICKETMASTER_API_KEY, OPENAI_API_KEY, SUPABASE_*) only via env vars;
  never logged, never returned to the client.
- Public endpoints (GET /api/events, GET /api/events/{id}, POST /api/plan) require
  no auth. Guest flow must never break because of auth work.
- Error body is always: {"error":{"code":"...","message":"...","details":{...},"requestId":"..."}}
- Dates: event times in the venue's timezone (ISO-8601 with offset + IANA timezone).
  Request dates are YYYY-MM-DD interpreted in the city's timezone.
- JSON: camelCase, nulls included for optional Event fields (frontend expects them).

Coding conventions:
- Constructor injection (@RequiredArgsConstructor).
- NO Java records anywhere. DTOs (API request/response, Ticketmaster/OpenAI payloads)
  are Lombok classes in the style of the existing ExceptionResponse:
  @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor.
  Immutable internal value objects (EventId, Event, Candidate...) use @Value @Builder
  (add @Jacksonized if Jackson must deserialize them).
  Wherever a prompt writes Name(field, field), it means a Lombok class with those fields.
- MapStruct for, MapStruct for
  entity<->DTO, @ConfigurationProperties Lombok classes for config (@Getter @Setter, like AppSettings) (validated with @Validated).
- Use RestClient for HTTP calls (no WebFlux). Timeouts on every outbound call.
- Every new table => Flyway migration in src/main/resources/db/migration (V<n>__name.sql).
- Tests: JUnit 5 + AssertJ; WireMock for Ticketmaster/OpenAI; Testcontainers Postgres
  for repository/integration tests. No real network calls in tests.
- Keep controllers thin; business logic in services; pure functions where possible.
- Run ./mvnw verify before finishing. Do not change unrelated files.