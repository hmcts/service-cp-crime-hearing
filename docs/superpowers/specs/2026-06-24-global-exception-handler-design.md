# GlobalExceptionHandler for service-cp-crime-hearing

## Problem

`service-cp-crime-hearing` has no `GlobalExceptionHandler`. `HearingClient` and
`CaseUrnMapperClient` call upstream services (`hearing-query-api`, `urnmapper`) via
`RestTemplate`, which throws `HttpClientErrorException`/`HttpServerErrorException` on
any non-2xx upstream response. With no handler, these propagate past the controller and
get rendered by Spring's default error handling instead of the published
`api-cp-crime-hearing` contract's `ErrorResponse` shape (`error`, `message`, `details`,
`timestamp`, `traceId`). A real 404 (caseURN not found in `urnmapper`) or a 5xx from
`hearing-query-api` currently doesn't look like a 404/5xx to the API consumer.

## Reference pattern

`service-cp-crime-prosecution-case-details` and `service-cp-crime-scheduleandlist-courtschedule`
have byte-identical `GlobalExceptionHandler`s — both stateless-proxy services like
`service-cp-crime-hearing`. `service-cp-refdata-courthearing-courthouses` extends that with
400-validation handlers; `service-cp-crime-hearing-results-document-subscription` (DB-backed)
has a materially different, richer shape and is not a fit reference here.

## Decision

Mirror the prosecution-case-details/scheduleandlist minimal-parity pattern exactly. No
validation handlers (`ConstraintViolationException` etc.) — `caseURN` has no bean-validation
constraints today, so those handlers would be dead code in this repo.

## Design

New file: `src/main/java/uk/gov/hmcts/cp/exceptions/GlobalExceptionHandler.java`
(package `exceptions`, matching the two reference repos).

`@RestControllerAdvice @Slf4j @AllArgsConstructor`, with `Tracer tracer` injected
(already on the classpath via `spring-boot-starter-opentelemetry` — no new dependency).

Handlers:

| Exception | Status | Source |
|---|---|---|
| `ResponseStatusException` | passthrough | future explicit business-error throws |
| `HttpServerErrorException` | passthrough | upstream 5xx via RestTemplate |
| `HttpClientErrorException` | passthrough | upstream 4xx (e.g. `urnmapper` 404 for unresolved caseURN) |
| `NoResourceFoundException` / `NoHandlerFoundException` | 404 | invalid route on this service |
| `Exception` (catch-all) | 500 | anything unhandled |

Each builds `ErrorResponse.builder().message(...).timestamp(Instant.now()).traceId(tracer.currentSpan().context().traceId()).build()`.
`error`/`details` fields stay unset, matching the existing convention in both reference repos.

## Test plan

Mirror `GlobalExceptionHandlerTest.java` from `service-cp-crime-prosecution-case-details`
1:1 under `src/test/java/uk/gov/hmcts/cp/exceptions/`: same 6 test methods, `@Spy Tracer
tracer = Tracer.NOOP`, `@InjectMocks GlobalExceptionHandler`. TDD: add the test file first
(it won't compile/will fail since the handler doesn't exist yet), then add the handler to
make it pass.

## Out of scope

- 400-validation handlers (no validation constraints exist on this controller yet).
- Populating `error`/`details` fields on `ErrorResponse` — pre-existing gap shared by all
  sibling repos, not introduced or fixed here.
- 401/403 — not produced by this service (no auth filter dependency wired in this repo).