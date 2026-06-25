# API 1 — Case Timeline (`GET /hearings/cases/{caseURN}/timeline`)

## Problem

Retroactive design doc — `getCaseTimeline` is already implemented (AMP-686, merged in PR #3).
Written now alongside the API 2/3 docs so all three endpoints have a matching record of their
backend mapping.

## Backend mapping

Two upstream calls, chained — `caseURN` is not usable directly against `hearing-query-api`, so it
must be resolved to a `caseId` first.

### 1. `GET /urnmapper/{caseURN}` — caseURN → caseId

- Client: `CaseUrnMapperClient.getCaseId(String caseUrn)`.
- Full gateway URL: `${AMP_BACKEND_URL}/urnmapper/{caseURN}` (`case-mapper-client.url` +
  `case-mapper-client.path`, no `CJSCPPUID` header — this lookup doesn't require it).
- `Accept: application/json`.
- Response (`CaseMapperResponse`, `@JsonIgnoreProperties(ignoreUnknown = true)`): `{caseId,
  caseUrn}`. Only `caseId` (`UUID`) is read.
- Service: `CaseUrnMapperService.getCaseId(String caseUrn)` — thin pass-through, no mapping.

### 2. `GET /hearing-query-api/query/api/rest/hearing/timeline/{caseId}` — the timeline itself

- Client: `HearingClient.getTimeline(UUID caseId)`.
- Full gateway URL: `${CP_BACKEND_URL}/hearing-query-api/query/api/rest/hearing/timeline/{caseId}`
  (`hearing-client.url` + `hearing-client.path`).
- `Accept: application/vnd.hearing.case.timeline+json`, `CJSCPPUID` header set from
  `hearing-client.cjscppuid`.
- Response (`HearingTimelineResponse`): `{hearingSummaries: [{hearingId, hearingDate, hearingType,
  courtHouse, courtRoom, hearingTime, startTime, outcome, estimatedDuration, defendants,
  youthDefendantIds}]}` — `@JsonIgnoreProperties(ignoreUnknown = true)` tolerates the fields not
  read (`estimatedDuration`, `defendants`, `youthDefendantIds`).
- **Known gap**: the WireMock fixture (`hearing-timeline-get.json`) models `hearingDate` as
  `"23 Jun 2026"` (`dd MMM yyyy`), but `HearingMapper.parseDate()` calls `LocalDate.parse(...)`
  with no formatter — which expects ISO-8601 (`yyyy-MM-dd`). Against that fixture's actual shape,
  parsing would throw, be caught, logged as a warning, and silently fall back to a `null` date
  (sorted last, excluded from `nextAppearance` resolution). Not fixed here — flagging only, since
  it's unclear whether the fixture or the real upstream format is the stale one.

## Design (as implemented)

| Layer | File | Role |
|---|---|---|
| Client | `clients/CaseUrnMapperClient.java` | `getCaseId(caseUrn)` — urnmapper call |
| Service | `services/CaseUrnMapperService.java` | pass-through to the client |
| Client | `clients/HearingClient.java` | `getTimeline(caseId)` — timeline call |
| Domain | `domain/CaseMapperResponse.java`, `domain/HearingTimelineResponse.java` | upstream response shapes |
| Mapper | `mappers/HearingMapper.java` | `mapToHearingTimelineView` — sorts summaries chronologically (skipping unparseable dates), maps fields 1:1 except date parsing, derives `nextAppearance` as the earliest non-past hearing |
| Service | `services/HearingService.java` | `getCaseTimeline(caseUrn)` — resolves `caseId` via `CaseUrnMapperService`, then calls `HearingClient.getTimeline`, then `HearingMapper` |
| Controller | `controllers/HearingController.java` | `getCaseTimeline(caseURN)` — sanitizes `caseURN` via `Encode.forJava` before logging |

## Test plan (already in place)

- `CaseUrnMapperClientTest`, `CaseUrnMapperServiceTest` — urnmapper call/pass-through.
- `HearingMapperTest` — chronological sort, next-appearance resolution (future vs. all-past),
  unparseable date skipped, null/empty response handling.
- `HearingServiceTest.getCaseTimeline_should_resolveCaseIdThenFetchAndMapTimeline` — full chain via
  mocks.
- `HearingControllerTest` — 200 with mapped body, `caseURN` XSS-sanitization smoke test.

## Out of scope

- Fixing the `hearingDate` format mismatch noted above — needs confirmation of the real upstream
  format first.
- No `apiTest`/WireMock-driven integration test wired up (`hearing-timeline-get.json` exists as a
  fixture but the `apiTest` Gradle source set isn't scaffolded yet — same gap noted in the API 2
  doc).