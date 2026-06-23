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

Only `getCaseTimeline` (`GET /hearings/cases/{caseURN}/timeline`) is implemented so far.
`getDefendantAttendance` and `getDefendants` still fall through to `HearingsApi`'s
default (501) implementation — not yet started.

- `controllers/HearingController` — implements `HearingsApi`, overrides `getCaseTimeline` only.
- `services/HearingService` — resolves `caseURN` → `caseId`, fetches the timeline, delegates mapping.
- `services/CaseUrnMapperService` + `clients/CaseUrnMapperClient` — resolves `caseURN` to a `caseId`
  via the `urnmapper` backend (see Architecture Rules below) — the same pattern as
  `service-cp-crime-prosecution-case-details`.
- `clients/HearingClient` — calls `hearing-query-api`'s `/timeline/{caseId}`.
- `mappers/HearingMapper` — owns all `HearingTimelineView`/`HearingSummaryView`/`NextAppearance`
  builder construction; sorts hearings chronologically and computes `nextAppearance` as the
  earliest hearing on or after "today" (via `ClockService`, per the shared time-access standard).

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
- `defendantAttendance`/`defendants` endpoints are unimplemented. Building them needs two unresolved data
  gaps: `offences[].status` has no source field on `hearing-query-api`'s raw offence object (would need a
  second call to `resulting-query-api`/`results-query-api`, or a derivation rule from plea fields), and the
  optional `masterDefendantId` filter has no equivalent on `hearing-query-api`'s defendant object (only on
  `progression`'s `/prosecutioncases/{caseId}`, per `cpp-case-aggregator-poc`'s own design notes).
- The `apiSpec` dependency is wired as a plain `implementation` coordinate (`uk.gov.hmcts.cp:api-cp-crime-hearing:1.0.0`) rather than a dedicated `apiSpec` Gradle configuration — this template generation does not define one; see `build.gradle`.

## Debugging

| Symptom | Cause / Fix |
|---|---|
| Build fails to resolve `api-cp-crime-hearing` | Azure Artifacts feed unreachable, or the dependency version was bumped without a corresponding release on `api-cp-crime-hearing` — check its latest release tag |
| `getCaseTimeline` 404s against a real backend | `hearing-query-api`'s `/timeline/{id}` expects a `caseId` UUID — check `CaseUrnMapperClient` actually resolved the `caseURN` rather than passing it straight through |

## Repo-Specific Notes

`getDefendantAttendance` and `getDefendants` controllers/services/clients are not yet implemented —
see the data gaps noted above before starting either.