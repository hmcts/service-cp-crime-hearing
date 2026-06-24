# Implement GET /hearings/{hearingId}/cases/{caseURN}/defendants

## Context

`api-cp-crime-hearing` (the OpenAPI spec library this service implements) already defines
`GET /hearings/{hearingId}/cases/{caseURN}/defendants`, including an optional `masterDefendantId`
query parameter and a `masterDefendantId` field on the `DefendantView` response (added in
`api-cp-crime-hearing` v1.0.1). `service-cp-crime-hearing` has no controller implementation for
this operation at all, and depends on `api-cp-crime-hearing:1.0.0`, which predates the endpoint.

A partial building block already exists but is unused: `HearingService.resolveDefendantIds()`
filters defendants by `masterDefendantId` (required) and `caseUrn` (optional), returning only IDs.
It's exercised only by its own unit tests, never wired to a controller.

The backend call this service already makes — `HearingClient.getHearing(hearingId)`, hitting
`hearing-query-api`'s `GET .../hearings/{id}` — was confirmed (via a real response payload) to
carry everything needed except `dateOfBirth` and a literal offence `status` field.

## Scope

Implement the endpoint fully, returning `id`, `masterDefendantId`, `name`, and `offences[]`
(`id`/`code`/`title`/`status`) per defendant. `dateOfBirth` is omitted (not present anywhere in the
real backend response) rather than guessed.

## Backend data confirmed (real payload)

```
hearing.prosecutionCases[].prosecutionCaseIdentifier.caseURN
hearing.prosecutionCases[].defendants[].id
hearing.prosecutionCases[].defendants[].masterDefendantId
hearing.prosecutionCases[].defendants[].personDefendant.personDetails.firstName
hearing.prosecutionCases[].defendants[].personDefendant.personDetails.lastName
hearing.prosecutionCases[].defendants[].offences[].id
hearing.prosecutionCases[].defendants[].offences[].offenceCode
hearing.prosecutionCases[].defendants[].offences[].offenceTitle
hearing.prosecutionCases[].defendants[].offences[].plea.pleaValue
```

`personDefendant` is absent for organisation (legal entity) defendants — `name` is left null in
that case rather than guessing at the `legalEntityDefendant` shape (not present in the confirmed
payload).

## Mapping rules

- `name` = `(firstName + " " + lastName).strip()` when `personDefendant` is present, else `null`.
  Mirrors `cpp-case-aggregator-poc`'s `CaseAggregatorService.fullName()`.
- `offences[].status` = `plea.pleaValue` if present and non-blank, else `"Active"`. Mirrors
  `cpp-case-aggregator-poc`'s `CaseAggregatorService.deriveOffenceStatus()`.
- `dateOfBirth` is never set. `spring.jackson.default-property-inclusion: non_null` is added to
  `application.yaml` so it (and any other null field) is omitted from the JSON response instead of
  serialized as `null` — there's no equivalent `@JsonInclude(NON_NULL)` on the generated DTOs
  themselves (a known, separate gap in `api-cp-crime-hearing`'s codegen config).

## Changes

1. `build.gradle` — bump `api-cp-crime-hearing` `1.0.0` → `1.0.1`.
2. `application.yaml` — add `spring.jackson.default-property-inclusion: non_null`.
3. `HearingResponse.java` (domain) — add to `DefendantEntry`: `personDefendant`, `offences`. New
   nested classes: `PersonDefendant { personDetails }`, `PersonDetails { firstName, lastName }`,
   `OffenceEntry { id, offenceCode, offenceTitle, plea }`, `Plea { pleaValue }`. All follow the
   existing `@JsonIgnoreProperties(ignoreUnknown = true)` pattern — only fields actually used are
   declared.
4. `HearingService.java` — replace `resolveDefendantIds` (dead code, superseded) with
   `getDefendants(hearingId, caseURN, masterDefendantId)`: fetch via the existing
   `hearingClient.getHearing(hearingId)`, find the `ProsecutionCase` matching `caseURN` (reusing
   the existing `matchesCaseUrn` helper, now always applied since `caseURN` is a required path
   param), get its defendants, filter further by `masterDefendantId` if supplied, delegate mapping
   to `DefendantMapper`.
5. New `DefendantMapper.java` — plain `@Component` (matches `DefendantAttendanceMapper`'s existing
   style, not MapStruct), maps `DefendantEntry` → `DefendantView` and `OffenceEntry` → `OffenceView`.
6. `HearingController.java` — add `getDefendants(hearingId, caseURN, masterDefendantId)` following
   the existing pattern of the two other endpoint methods.
7. Tests: `HearingControllerTest` (new case), `HearingServiceTest` (replace the 5
   `resolveDefendantIds_*` tests with `getDefendants_*` equivalents), new `DefendantMapperTest`.

## Out of scope

- No new backend integration — reuses the existing `hearingClient.getHearing` call.
- No `apiTest`/WireMock harness — this repo has no `apiTest` Java tests wired up yet (only orphaned
  mapping files), so adding that infrastructure is a separate concern.
- `legalEntityDefendant` (organisation defendant) name resolution — not present in the confirmed
  payload, left as `null` name rather than guessed.