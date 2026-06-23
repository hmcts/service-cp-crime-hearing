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

Bare template scaffold only — `Application.java` is the sole class. Controllers,
services, and the `hearing-query-api` client implementing the three GET endpoints
have not been written yet; that is a follow-up implementation task, not yet started.

## Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `CP_BACKEND_URL` | Base URL of the CP backend gateway fronting `hearing-query-api` | none — required |
| `CJSCPPUID` | User UUID propagated to `hearing-query-api` for authorization | empty |
| `SERVER_PORT` | HTTP port | `8082` |

## Repo-Specific Architecture Rules

- Calls `hearing-query-api` at `${CP_BACKEND_URL}/hearing-query-api/query/api/rest/hearing` with a `CJSCPPUID` header — no other auth on that call.
- The `apiSpec` dependency is wired as a plain `implementation` coordinate (`uk.gov.hmcts.cp:api-cp-crime-hearing:1.0.0`) rather than a dedicated `apiSpec` Gradle configuration — this template generation does not define one; see `build.gradle`.

## Debugging

| Symptom | Cause / Fix |
|---|---|
| Build fails to resolve `api-cp-crime-hearing` | Azure Artifacts feed unreachable, or the dependency version was bumped without a corresponding release on `api-cp-crime-hearing` — check its latest release tag |

## Repo-Specific Notes

Controllers/services/clients for the three GET endpoints are not yet implemented —
this repo is currently the bootstrapped template plus identity/dependency wiring only.