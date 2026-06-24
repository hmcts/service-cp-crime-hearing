# Architecture — service-cp-crime-hearing

## High-level boundary: API Marketplace ↔ Common Platform hearing domain

`service-cp-crime-hearing` is the API Marketplace (APIM) side of the boundary — a stateless
proxy. It holds no data of its own; every request fans out to Common Platform's legacy CQRS
`hearing-query-api` (and, for `getCaseTimeline`, the `urnmapper` service first). Exact endpoints
shown below.

```mermaid
flowchart LR
    subgraph Consumers["API Marketplace consumers"]
        HMPPS["HMPPS / Prison services"]
    end

    subgraph APIM["API Marketplace — service-cp-crime-hearing"]
        Hearing["HearingController\n(implements api-cp-crime-hearing)"]
    end

    subgraph CP["Common Platform"]
        UrnMapper["urnmapper service\nGET /urnmapper/{caseURN}"]
        HearingQueryApi["hearing-query-api\n(legacy CQRS)"]
    end

    HMPPS -->|"GET /hearings/cases/{caseURN}/timeline\nGET /hearings/{hearingId}/attendance"| Hearing

    Hearing -->|"GET /urnmapper/{caseURN}\n(caseURN → caseId, getCaseTimeline only)"| UrnMapper
    Hearing -->|"GET /hearing-query-api/query/api/rest/hearing/timeline/{caseId}\nAccept: application/vnd.hearing.case.timeline+json"| HearingQueryApi
    Hearing -->|"GET /hearing-query-api/query/api/rest/hearing/hearings/{hearingId}\nAccept: application/vnd.hearing.get.hearing+json"| HearingQueryApi
```

| Hop | From | To | Endpoint | Media type | Used by |
|---|---|---|---|---|---|
| 1a | `service-cp-crime-hearing` | `urnmapper` | `GET ${AMP_BACKEND_URL}/urnmapper/{caseURN}` | `application/json` | `getCaseTimeline` only |
| 1b | `service-cp-crime-hearing` | `hearing-query-api` | `GET ${CP_BACKEND_URL}/hearing-query-api/query/api/rest/hearing/timeline/{caseId}` | `application/vnd.hearing.case.timeline+json` | `getCaseTimeline` |
| 2 | `service-cp-crime-hearing` | `hearing-query-api` | `GET ${CP_BACKEND_URL}/hearing-query-api/query/api/rest/hearing/hearings/{hearingId}` | `application/vnd.hearing.get.hearing+json` | `getDefendantAttendance`, `resolveDefendantId` (no controller wired yet) |

All hops 1b/2 send a `CJSCPPUID` header for authorization; hop 1a sends none.

## Internal layers

```mermaid
flowchart TD
    Controller["HearingController\n(implements HearingsApi)"]
    ServiceL["HearingService"]
    UrnService["CaseUrnMapperService"]
    UrnClient["CaseUrnMapperClient"]
    HearingClient["HearingClient"]
    HearingMapper["HearingMapper"]
    AttendanceMapper["DefendantAttendanceMapper"]
    GlobalEx["GlobalExceptionHandler\n(@RestControllerAdvice)"]

    Controller --> ServiceL
    ServiceL --> UrnService --> UrnClient
    ServiceL --> HearingClient
    ServiceL --> HearingMapper
    ServiceL --> AttendanceMapper
    Controller -.->|"4xx/5xx from any client call"| GlobalEx
```

- **Controller** — thin; delegates to `HearingService`, returns `ResponseEntity`. No business logic, no object construction.
- **Service** — orchestrates the client(s) + mapper for each endpoint. Never builds response objects directly.
- **Mapper** — owns all `.builder()` construction. `HearingMapper` for the timeline; `DefendantAttendanceMapper` for attendance — kept separate since they map different domain models from different upstream calls.
- **Client** — `CaseUrnMapperClient` (urnmapper), `HearingClient` (`hearing-query-api`, two methods: `getTimeline`, `getHearing`). No business logic.
- **GlobalExceptionHandler** — catches `HttpClientErrorException`/`HttpServerErrorException` from any client call and maps to the contract's `ErrorResponse`; this is how a 404 from either upstream hop becomes a 404 to the API consumer, with no per-endpoint error-handling code.

## Sequence — `getCaseTimeline` (two-hop)

```mermaid
sequenceDiagram
    participant Client as HMPPS
    participant Ctrl as HearingController
    participant Svc as HearingService
    participant UrnSvc as CaseUrnMapperService
    participant HClient as HearingClient
    participant Urn as urnmapper
    participant HQA as hearing-query-api

    Client->>Ctrl: GET /hearings/cases/{caseURN}/timeline
    Ctrl->>Svc: getCaseTimeline(caseURN)
    Svc->>UrnSvc: getCaseId(caseURN)
    UrnSvc->>Urn: GET /urnmapper/{caseURN}
    Urn-->>UrnSvc: caseId
    Svc->>HClient: getTimeline(caseId)
    HClient->>HQA: GET .../timeline/{caseId}
    HQA-->>HClient: HearingTimelineResponse
    Svc->>Svc: HearingMapper.mapToHearingTimelineView(...)
    Svc-->>Ctrl: HearingTimelineView
    Ctrl-->>Client: 200 OK
```

## Sequence — `getDefendantAttendance` (single-hop)

```mermaid
sequenceDiagram
    participant Client as HMPPS
    participant Ctrl as HearingController
    participant Svc as HearingService
    participant HClient as HearingClient
    participant HQA as hearing-query-api

    Client->>Ctrl: GET /hearings/{hearingId}/attendance
    Ctrl->>Svc: getDefendantAttendance(hearingId)
    Svc->>HClient: getHearing(hearingId)
    HClient->>HQA: GET .../hearings/{hearingId}
    HQA-->>HClient: HearingResponse (hearing.defendantAttendance[])
    Svc->>Svc: DefendantAttendanceMapper.mapToDefendantAttendanceView(...)
    Svc-->>Ctrl: DefendantAttendanceView
    Ctrl-->>Client: 200 OK (404 if hearingId doesn't exist, via GlobalExceptionHandler)
```

`resolveDefendantId(hearingId, masterDefendantId, caseURN)` reuses the same `HearingClient.getHearing`
call and `HearingResponse` DTO, reading `hearing.prosecutionCases[].defendants[]` instead of
`hearing.defendantAttendance[]` — no second HTTP call. It has no controller wired to it yet; it's
infrastructure for the future `getDefendants` endpoint.
