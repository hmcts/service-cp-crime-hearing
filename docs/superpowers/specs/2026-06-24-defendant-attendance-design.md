# API 2 — Defendant Attendance (`GET /hearings/{hearingId}/attendance`)

## Problem

`getDefendantAttendance` is currently unimplemented — falls through to `HearingsApi`'s
generated 501 default.

## Backend mapping (confirmed live against `steccm64` dev, and via `cpp-case-aggregator-poc`)

- Upstream call: `GET /hearings/{hearingId}` on `hearing-query-api`, `Accept:
  application/vnd.hearing.get.hearing+json`. No caseURN→caseId resolution needed —
  unlike `getCaseTimeline`, `hearingId` is used directly against this resource.
- Full gateway URL: `${CP_BACKEND_URL}/hearing-query-api/query/api/rest/hearing/hearings/{hearingId}`
  (same gateway prefix as the timeline call; RAML resource is `/hearings/{hearingId}`,
  not `/timeline/{id}`).
- **Confirmed live**: the response is wrapped in a `hearing` envelope —
  `{"hearing": {..., "defendantAttendance": [{"defendantId": "...", "attendanceDays":
  [{"day": "2026-06-23", "attendanceType": "IN_PERSON"}]}]}}`. `cpp-case-aggregator-poc`'s
  defensive `hearing.has("hearing") ? hearing.get("hearing") : hearing` check suggested
  this might be optional; live data confirms the envelope is present, so the domain DTO
  models it directly rather than defensively unwrapping both shapes.
- Published contract (`DefendantAttendanceView`): `{id, defendants: [{id, attendanceDays:
  [{day, type: IN_PERSON|VIDEO_LINK|DID_NOT_APPEAR}]}]}` — field renames required:
  `defendantId`→`id`, `attendanceType`→`type`.
- **404** (hearing doesn't exist): upstream 404 propagates as `HttpClientErrorException`
  via `RestTemplate`, already caught by the existing `GlobalExceptionHandler` — no new
  error-handling code needed for this AC.
- **Empty attendance**: `defendantAttendance` null/empty on the upstream response maps to
  `defendants: []`.

## Design

| Layer | File | Change |
|---|---|---|
| Domain | `domain/HearingResponse.java` (new) | `{hearing: HearingDetail}`, `HearingDetail {defendantAttendance: List<DefendantAttendanceEntry>, prosecutionCases: List<ProsecutionCase>}`, `DefendantAttendanceEntry {defendantId, attendanceDays}`, `AttendanceDayEntry {day, attendanceType}`, `ProsecutionCase {prosecutionCaseIdentifier: {caseURN}, defendants: List<DefendantEntry>}`, `DefendantEntry {id, masterDefendantId}` — `@JsonIgnoreProperties(ignoreUnknown = true)` on every level to tolerate the rest of the large hearing payload. One DTO, shared by both features below — same upstream call, two different read-paths over the same response |
| Client | `clients/HearingClient.java` | add `getHearing(UUID hearingId)` |
| Config | `config/AppPropertiesBackend.java`, `application.yaml` | add `hearing-client.get-path: /hearing-query-api/query/api/rest/hearing/hearings` (hardcoded, same convention as the existing `timeline` path — no new env var) |
| Service | `services/HearingService.java` | add `getDefendantAttendance(UUID hearingId)` and `resolveDefendantId(UUID hearingId, UUID masterDefendantId, String caseURN)` |
| Mapper | `mappers/DefendantAttendanceMapper.java` (new) | owns all `.builder()` calls for `DefendantAttendanceView`/`DefendantAttendance`/`AttendanceDay`. Separate from `HearingMapper` — different domain model, different upstream call, mirrors the existing `CaseUrnMapperClient`/`Service` split |
| Controller | `controllers/HearingController.java` | override `getDefendantAttendance(UUID hearingId)` |

### `resolveDefendantId` — reusable masterDefendantId → defendantId lookup

Corrects a previously-documented data gap: `CLAUDE.md` stated `hearing-query-api`'s
defendant object has no `masterDefendantId` equivalent. It does — nested under
`hearing.prosecutionCases[].defendants[].masterDefendantId` on the same `GET
/hearings/{hearingId}` response already being fetched for `getDefendantAttendance`.

No new HTTP call: `resolveDefendantId` calls the same `hearingClient.getHearing(hearingId)`,
then:
1. Filters `hearing.prosecutionCases[]` by `caseURN` if provided (belt-and-braces
   disambiguation — skippable since `masterDefendantId` + `hearingId` alone is normally
   sufficient to land a single defendant).
2. Flattens to `defendants[]` across the (filtered) prosecution cases.
3. Matches on `masterDefendantId`, returns the matching defendant's `id` as `Optional<UUID>`.

This is pure filtering — no object construction — so it lives directly on `HearingService`
per the layer rule (mappers only own `.builder()` construction; this method never builds one).

**No controller wired to this yet** — it's infrastructure for `getDefendants`
(`/hearings/{hearingId}/cases/{caseURN}/defendants`), which remains blocked by the other
documented data gap (`offences[].status` has no source field). Exercised directly by a unit
test so it isn't unreachable dead code.

## Test plan

Mirror the existing `HearingClientTest`/`HearingServiceTest`/`HearingControllerTest`/
`HearingMapperTest` patterns (JUnit 5 + Mockito, no WireMock for unit tests):

- `HearingClientTest`: new test for `getHearing` building the correct URL/headers.
- `DefendantAttendanceMapperTest`: maps multiple defendants/days correctly; null/empty
  `defendantAttendance` → empty `defendants` list; field renames (`defendantId`→`id`,
  `attendanceType`→`type`) correct; multiple days for the same defendant.
- `HearingServiceTest`: `getDefendantAttendance` delegates to client + mapper;
  `resolveDefendantId` returns the matching defendant across multiple prosecution
  cases/defendants, filters correctly by `caseURN` when given, returns `Optional.empty()`
  when no defendant matches.
- `HearingControllerTest`: returns 200 with mapped body.
- 404 path: no new test — already proven generically by `GlobalExceptionHandlerTest`.

WireMock fixtures for future `apiTest` authoring already added at
`src/apiTest/resources/mappings/hearing-get-defendant-attendance.json` (trimmed to the
fields the mapper reads — full live response contained case/personal data out of scope
for this fixture) and `hearing-timeline-get.json`.

## Out of scope

- `getDefendants` (`/hearings/{hearingId}/cases/{caseURN}/defendants`) — separate
  endpoint, separate data gaps (per repo `CLAUDE.md`), not addressed here.
- Wiring `docker-compose.yml`/`build.gradle` for `./gradlew dockerTest` — fixtures are
  in place but the apiTest Gradle source set isn't scaffolded yet (explicitly deferred).