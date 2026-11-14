# Sxodim USA — backend context for code agents

Product: AI personal leisure planner (MVP). A guest (no login) picks city, interests,
date range, budget and time of day; backend fetches REAL events from Ticketmaster
Discovery API, filters them, asks OpenAI to rank up to 5 and explain why, validates
the AI answer, returns JSON. Pilot city: New York only.

Stack: Java 21, Spring Boot 4.1.1 (Spring Framework 7, Jackson 3), spring-webmvc,
Data JPA, Validation, Flyway, PostgreSQL 18 (docker-compose.yml), Lombok, MapStruct 1.6,
springdoc-openapi. Build: ./mvnw. Base package: kz.astyq.zimran.

## Requirements

Do not never broke SOLID principles.

No testing needed: do not write new tests and do not run tests (including ./mvnw verify) unless
explicitly asked. Only keep existing test sources compiling when a change breaks them. This overrides
the "Tests mirror..." and "Run ./mvnw verify" rules below.

## Project structure (same layering as our restaurant-service)

Feature-first. Inside each feature package:
view/controller/<Feature>ViewController       public REST endpoints (guest/user)
view/service/<Feature>ViewService             interface the controller depends on
view/service/impl/<Feature>ViewServiceImpl    @Service implementation
admin/controller|service|service/impl          same shape for admin endpoints (none in MVP)
service/<Name>Service + service/impl/<Name>ServiceImpl
internal services used by other features
client/<Name>Client + client/impl/<Name>ClientImpl
outbound HTTP clients (ticketmaster, ai only)
mapper/<Feature>Mapper                        MapStruct
model/dto/                                    request/response DTOs
model/entity/                                 JPA entities (only features that persist)
model/enums/
model/domain/                                 non-persisted internal objects (e.g. Event)
repository/<Feature>Repository                extends JpaRepository<Entity, Long>
util/MessageCode                              i18n message keys; pure static helpers

Features:
core/          configuration, exception (ServiceValidationException, dto, handler), i18n,
model (BaseEntity, AuditableEntity, AppSettings), util (ErrorCode), web
(filters), security (P2)
city/          supported cities (service, model/domain, model/dto)
meta/          GET /api/meta (view/...)
event/         catalog: view (GET /api/events, /api/events/{id}), service (EventQueryService),
mapper, model/{domain,dto,enums}, util
ticketmaster/  client, model/dto (raw API DTOs), service (normalizer, interest mapping), util
planner/       view (POST /api/plan), service (validation, candidates, recommendations),
mapper, model/{domain,dto,enums}, util
ai/            client (OpenAI), service (ranking, usage guard), model/dto, util (prompt,
schema, sanitizer)
profile/, user_preference/  (P2) view, mapper, model/{dto,entity}, repository, util

## Layer rules (copy restaurant-service)

- Controller: @RestController @RequiredArgsConstructor @RequestMapping("/api/...");
  depends only on its view service interface; returns DTOs directly; ResponseEntity only
  for 204 or custom headers; @Valid @RequestBody; query objects with @ParameterObject.
- Service: interface in service/ (or view/service/), implementation in .../impl with
  @Service @RequiredArgsConstructor; @Transactional on write methods only.
- Errors: throw new ServiceValidationException(ErrorCode.X, MessageCode.Y, args...).
  Each feature has util/MessageCode: @NoArgsConstructor(access = AccessLevel.PRIVATE),
  public static final String keys in snake_case (EVENT_NOT_FOUND = "event_not_found").
  Add every key to messages.properties, messages_en, messages_ru and messages_kk
  (ru/kk may repeat the English text for now). No user-facing text hardcoded in Java.
- Mapper: @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE),
  methods toEntity / updateEntity(@MappingTarget) / toViewResponse.
- DTOs: Lombok @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor, bean
  validation on requests. Names: <X>CreateRequest, <X>UpdateRequest, <X>ViewResponse.
  NO Java records anywhere. Immutable domain objects: @Value @Builder (+ @Jacksonized
  if Jackson reads them). Config: @ConfigurationProperties Lombok classes like
  AppSettings, registered in ZimranApplication via @EnableConfigurationProperties.
- Entity: extends AuditableEntity; @Entity @Table(name = "...") @Getter @Setter
  @SuperBuilder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(callSuper = false)
  @SequenceGenerator(name = "seq", sequenceName = "s_<table>", allocationSize = 1)
  @SQLDelete(sql = "update <table> set is_deleted = true, deleted_at = now() where id = ?")
  @SQLRestriction("is_deleted = false").
- Flyway: src/main/resources/db/migration/V<n>__create_<table>_table.sql; first
  CREATE SEQUENCE s_<table> INCREMENT BY 1 MINVALUE 1 START 1; then the table with
  id BIGINT PK, created_at TIMESTAMPTZ NOT NULL, updated_at, deleted_at, is_deleted
  BOOLEAN DEFAULT FALSE; unique indexes as partial indexes WHERE is_deleted = false.
- Tests mirror the main package path:
  service impl -> @ExtendWith(MockitoExtension.class), @Mock deps, @InjectMocks impl,
  one @Nested class + @DisplayName per method, AssertJ assertThat/assertThatThrownBy;
  controller -> @WebMvcTest(XController.class), @MockitoBean services, MockMvc, jsonPath;
  mapper -> plain unit test with Mappers.getMapper(XMapper.class).
  WireMock for Ticketmaster/OpenAI, Testcontainers Postgres for repositories.
  No real network calls in tests.

## Non-negotiable product rules

- Never invent events, URLs, prices, dates, addresses or images. Every field returned
  to the client comes from Ticketmaster data, never from LLM text.
- Unknown price => priceMin/priceMax = null, priceKnown = false. Never treat as free/$0.
- AI may only choose events from the candidate set we sent; anything else is dropped.
- User notes and external event descriptions are untrusted text (prompt injection).
- Secrets (TICKETMASTER_API_KEY, OPENAI_API_KEY, SUPABASE_*) only via env vars;
  never logged, never returned to the client.
- Public endpoints (GET /api/events, GET /api/events/{id}, POST /api/plan, GET /api/meta)
  require no auth. Guest flow must never break because of auth work.
- Error body is always: {"error":{"code":"...","message":"...","details":{...},"requestId":"..."}}
- Dates: event times in the venue's timezone (ISO-8601 with offset + IANA timezone).
  Request dates are YYYY-MM-DD interpreted in the city's timezone.
- JSON: camelCase, nulls included for optional event fields (frontend expects them).
- Use RestClient for HTTP calls (no WebFlux). Timeouts on every outbound call.
- Run ./mvnw verify before finishing. Do not change unrelated files.