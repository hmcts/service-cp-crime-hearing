# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo: service-cp-crime-hearing

Implements the `api-cp-crime-hearing` contract — exposes case timeline, defendant
attendance, and defendant/offence lookups to HMPPS/Prison services.

**Pattern**: Stateless proxy
**Spring Boot version**: 4.1.0
**Implements**: `api-cp-crime-hearing`

## Infrastructure

| Component | Technology | Purpose |
|---|---|---|
| `hearing-query-api` | Legacy CQRS query service | Upstream source of case timeline, defendant attendance, and defendant/offence data |

## Source Structure

All three operations on `HearingsApi` are implemented: `getCaseTimeline`
(`GET /hearings/cases/{caseURN}/timeline`), `getDefendantAttendance`
(`GET /hearings/{hearingId}/attendance`), and `getDefendants`
(`GET /hearings/{hearingId}/cases/{caseURN}/defendants`).

- `controllers/HearingController` — implements `HearingsApi`, overrides all three operations.
- `services/HearingService` — `getCaseTimeline` resolves `caseURN` → `caseId`, fetches the
  timeline, delegates mapping. `getDefendantAttendance` fetches the hearing, delegates mapping.
  `getDefendants(hearingId, caseURN, masterDefendantId)` fetches the same hearing, filters
  `prosecutionCases[].defendants[]` by the required `caseURN` and then optionally by
  `masterDefendantId`, and delegates mapping to `DefendantMapper`.
- `services/CaseUrnMapperService` + `clients/CaseUrnMapperClient` — resolves `caseURN` to a `caseId`
  via the `urnmapper` backend (see Architecture Rules below) — the same pattern as
  `service-cp-crime-prosecution-case-details`.
- `clients/HearingClient` — `getTimeline` calls `hearing-query-api`'s `/timeline/{caseId}`;
  `getHearing` calls `/hearings/{hearingId}` (`Accept: application/vnd.hearing.get.hearing+json`) —
  shared by `getDefendantAttendance` and `getDefendants`.
- `mappers/HearingMapper` — owns all `HearingTimelineView`/`HearingSummaryView`/`NextAppearance`
  builder construction; sorts hearings chronologically and computes `nextAppearance` as the
  earliest hearing on or after "today" (via `ClockService`, per the shared time-access standard).
- `mappers/DefendantAttendanceMapper` — owns all `DefendantAttendanceView`/`DefendantAttendance`/
  `AttendanceDay` builder construction. Separate from `HearingMapper` — different domain model,
  different upstream call.
- `mappers/DefendantMapper` — owns all `DefendantView`/`OffenceView` builder construction for
  `getDefendants`. `name` comes from `defendant.personDefendant.personDetails.firstName/lastName`
  (null if absent, e.g. organisation/legal-entity defendants); `offences[].status` is derived from
  `offence.plea.pleaValue` (`"Active"` if no plea recorded yet), mirroring
  `cpp-case-aggregator-poc`'s `CaseAggregatorService.deriveOffenceStatus`. `dateOfBirth` is never
  set — it isn't present anywhere in the real `hearing-query-api` response.
- `domain/HearingResponse` — DTO for the `GET /hearings/{hearingId}` response, shared by
  `getDefendantAttendance` and `getDefendants` (one upstream call, two read-paths over the
  same response). The response is wrapped in a `hearing` envelope — confirmed live against
  `steccm64` dev, not just from RAML/example files (the checked-in RAML example is incomplete —
  see Architecture Rules).
- `exceptions/GlobalExceptionHandler` (`@RestControllerAdvice`) — maps
  `HttpClientErrorException`/`HttpServerErrorException`/`ResponseStatusException`/404s to the
  contract's `ErrorResponse`, stamping `timestamp` (via `ClockService`) and `traceId` (via
  Micrometer's `Tracer`). This is how upstream 404s become 404s to the API consumer with no
  per-endpoint error-handling code.
- `filters/tracing/TracingFilter` — ported from `service-cp-crime-prosecution-case-details`;
  this repo had none until now, despite `service-shared.md` mandating one for every `service-cp-*`.

## Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `CP_BACKEND_URL` | Base URL of the CP backend gateway fronting `hearing-query-api` | `http://localhost:8081` |
| `AMP_BACKEND_URL` | Base URL fronting the `urnmapper` service used to resolve `caseURN` → `caseId` | `http://localhost:8081` |
| `CJSCPPUID` | User UUID propagated to `hearing-query-api` for authorization | `00000000-0000-0000-0000-000000000000` |
| `SERVER_PORT` | HTTP port | `8082` |

## Repo-Specific Architecture Rules

- `getCaseTimeline` is a **two-hop** call, not the single hop the original scaffold docs assumed:
  1. `CaseUrnMapperClient` resolves `caseURN` → `caseId` via `${AMP_BACKEND_URL}/urnmapper/{caseURN}`
     (mirrors `service-cp-crime-prosecution-case-details`'s `case-mapper-client`). `hearing-query-api`'s
     `/timeline/{id}` RAML resource takes a case **ID** (UUID), not a URN — confirmed against
     `cpp-context-hearing`'s RAML; passing the raw URN through would 404.
  2. `HearingClient` then calls `${CP_BACKEND_URL}/hearing-query-api/query/api/rest/hearing/timeline/{caseId}`
     with a `CJSCPPUID` header — no other auth on that call.
- `nextAppearance` has no `hearingId` input on the published `getCaseTimeline` contract (only `caseURN`),
  unlike the reference `cpp-case-aggregator-poc` implementation which takes an explicit `hearingId`. It is
  computed automatically as the earliest *future* hearing relative to `ClockService.nowOffsetUTC()` — this is a
  deliberate interpretation of the contract, not a literal upstream field; revisit if product intent differs.
- `getDefendantAttendance` is a **single-hop** call, unlike `getCaseTimeline` — `hearingId` is
  used directly against `GET /hearings/{hearingId}`, no `caseURN`→`caseId` resolution needed.
- **Corrected data gap**: this file previously stated `hearing-query-api`'s defendant object has
  no `masterDefendantId` equivalent. It does — nested under
  `hearing.prosecutionCases[].defendants[].masterDefendantId` on the same `GET /hearings/{hearingId}`
  response, confirmed live against `steccm64` dev (the checked-in RAML example
  `json/hearing.get.hearing.json` is incomplete and omits it — the binding schema and generated
  POJO both have the field). `HearingService.resolveDefendantIds` implements this resolution
  (returning a list — `masterDefendantId`:`defendantId` is 1:many, not 1:1) but has no caller yet.
- `offences[].status` has no literal source field on `hearing-query-api`'s raw offence object —
  resolved via a derivation rule (`plea.pleaValue`, default `"Active"`) rather than a second
  backend call, matching `cpp-case-aggregator-poc`'s existing precedent.
- The `apiSpec` dependency is wired as a plain `implementation` coordinate (`uk.gov.hmcts.cp:api-cp-crime-hearing:1.0.1`) rather than a dedicated `apiSpec` Gradle configuration — this template generation does not define one; see `build.gradle`. Must stay in lockstep with the `api-cp-crime-hearing` release that introduces whatever endpoint/field this service needs next.

## Debugging

| Symptom | Cause / Fix |
|---|---|
| Build fails to resolve `api-cp-crime-hearing` | Azure Artifacts feed unreachable, or the dependency version was bumped without a corresponding release on `api-cp-crime-hearing` — check its latest release tag |
| `getCaseTimeline` 404s against a real backend | `hearing-query-api`'s `/timeline/{id}` expects a `caseId` UUID — check `CaseUrnMapperClient` actually resolved the `caseURN` rather than passing it straight through |

## Repo-Specific Notes

- `application.yaml` is intentionally aligned with sibling `service-cp-*` repos: graceful shutdown,
  `management.endpoint.health.show-details: always`, `management.tracing` enabled at full sampling
  (this service depends on `spring-boot-starter-opentelemetry` but didn't configure tracing until
  now), `spring.web.resources.add-mappings: false`, the `spring.config.import:
  optional:configtree:/mnt/secrets/rpe/` secrets mount (needed for `rpe.AppInsightsInstrumentationKey`
  to resolve to anything other than its all-zeros default), and
  `spring.jackson.default-property-inclusion: non_null` (so fields with no backend data, e.g.
  `DefendantView.dateOfBirth`, are omitted from responses rather than serialized as `null`).
- `docs/architecture.md` and `docs/superpowers/specs/*.md` hold per-endpoint design notes (backend
  call mapping, sequence diagrams) for all three operations — check there before re-deriving the
  upstream contract from scratch.