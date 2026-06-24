## Repo: service-cp-crime-hearing

Implements the `api-cp-crime-hearing` contract — exposes case timeline, defendant
attendance, and defendant/offence lookups to Remand and Sentencing (RaS) and
HMPPS/Prison services.

**Pattern**: Stateless proxy
**Spring Boot version**: 4.1.0
**Implements**: `api-cp-crime-hearing`

## Infrastructure

| Component | Technology | Purpose |
|---|---|---|
| `hearing-query-api` | Legacy CQRS query service | Upstream source of case timeline, defendant attendance, and defendant/offence data |

## Source Structure

`getCaseTimeline` (`GET /hearings/cases/{caseURN}/timeline`) and `getDefendantAttendance`
(`GET /hearings/{hearingId}/attendance`) are implemented. `getDefendants` still falls through
to `HearingsApi`'s default (501) implementation — one of its two data gaps is now resolved
(see Architecture Rules), the other (`offences[].status`) is not.

- `controllers/HearingController` — implements `HearingsApi`, overrides `getCaseTimeline` and
  `getDefendantAttendance`.
- `services/HearingService` — `getCaseTimeline` resolves `caseURN` → `caseId`, fetches the
  timeline, delegates mapping. `getDefendantAttendance` fetches the hearing, delegates mapping.
  `resolveDefendantId(hearingId, masterDefendantId, caseURN)` fetches the same hearing and
  filters `prosecutionCases[].defendants[]` by `masterDefendantId` (optionally narrowed by
  `caseURN`) — infrastructure for `getDefendants`, no controller wired to it yet.
- `services/CaseUrnMapperService` + `clients/CaseUrnMapperClient` — resolves `caseURN` to a `caseId`
  via the `urnmapper` backend (see Architecture Rules below) — the same pattern as
  `service-cp-crime-prosecution-case-details`.
- `clients/HearingClient` — `getTimeline` calls `hearing-query-api`'s `/timeline/{caseId}`;
  `getHearing` calls `/hearings/{hearingId}` (`Accept: application/vnd.hearing.get.hearing+json`) —
  shared by `getDefendantAttendance` and `resolveDefendantId`.
- `mappers/HearingMapper` — owns all `HearingTimelineView`/`HearingSummaryView`/`NextAppearance`
  builder construction; sorts hearings chronologically and computes `nextAppearance` as the
  earliest hearing on or after "today" (via `ClockService`, per the shared time-access standard).
- `mappers/DefendantAttendanceMapper` — owns all `DefendantAttendanceView`/`DefendantAttendance`/
  `AttendanceDay` builder construction. Separate from `HearingMapper` — different domain model,
  different upstream call.
- `domain/HearingResponse` — DTO for the `GET /hearings/{hearingId}` response, shared by
  `getDefendantAttendance` and `resolveDefendantId` (one upstream call, two read-paths over the
  same response). The response is wrapped in a `hearing` envelope — confirmed live against
  `steccm64` dev, not just from RAML/example files (the checked-in RAML example is incomplete —
  see Architecture Rules).
- `filters/tracing/TracingFilter` — ported from `service-cp-crime-prosecution-case-details`;
  this repo had none until now, despite `service-shared.md` mandating one for every `service-cp-*`.

## Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `CP_BACKEND_URL` | Base URL of the CP backend gateway fronting `hearing-query-api` | `http://localhost:8081` |
| `AMP_BACKEND_URL` | Base URL fronting the `urnmapper` service used to resolve `caseURN` → `caseId` | `http://localhost:8081` |
| `CJSCPPUID` | User UUID propagated to `hearing-query-api` for authorization | empty |
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
  POJO both have the field). `HearingService.resolveDefendantId` implements this resolution but
  has no caller yet.
- `getDefendants` remains blocked on one data gap only: `offences[].status` has no source field
  on `hearing-query-api`'s raw offence object (would need a second call to
  `resulting-query-api`/`results-query-api`, or a derivation rule from plea fields).
- The `apiSpec` dependency is wired as a plain `implementation` coordinate (`uk.gov.hmcts.cp:api-cp-crime-hearing:1.0.0`) rather than a dedicated `apiSpec` Gradle configuration — this template generation does not define one; see `build.gradle`.

## Debugging

| Symptom | Cause / Fix |
|---|---|
| Build fails to resolve `api-cp-crime-hearing` | Azure Artifacts feed unreachable, or the dependency version was bumped without a corresponding release on `api-cp-crime-hearing` — check its latest release tag |
| `getCaseTimeline` 404s against a real backend | `hearing-query-api`'s `/timeline/{id}` expects a `caseId` UUID — check `CaseUrnMapperClient` actually resolved the `caseURN` rather than passing it straight through |

## Repo-Specific Notes

`getDefendants` (`/hearings/{hearingId}/cases/{caseURN}/defendants`) controller/service/client are
not yet implemented — see the `offences[].status` data gap above before starting it.