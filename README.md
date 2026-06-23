# service-cp-crime-hearing

Implements the [CP Crime Hearing API](https://github.com/hmcts/api-cp-crime-hearing) —
exposes case timeline, defendant attendance, and defendant/offence lookups to
Remand and Sentencing (RaS) and HMPPS/Prison services.

- **API contract:** `api-cp-crime-hearing` (depends on published artefact `uk.gov.hmcts.cp:api-cp-crime-hearing:1.0.0` — see `build.gradle`).
- **Upstream backend:** the legacy `hearing-query-api` CQRS query service, called at
  `${CP_BACKEND_URL}/hearing-query-api/query/api/rest/hearing` with a `CJSCPPUID` header
  for authorization (see `application.yaml`).
- **Owning team:** `@hmcts/api-marketplace`.
- **Support model / escalation:** raise an issue against this repo or contact the
  `api-marketplace` team; on-call rota TBD.

## New team member setup

Anyone newly added to the `api-marketplace` team should verify push access once:

```bash
gh auth login                                          # if not already authenticated
git clone git@github.com:hmcts/service-cp-crime-hearing.git
cd service-cp-crime-hearing
git checkout -b smoke/access-check
git commit --allow-empty -m "chore: verify push access"
git push -u origin smoke/access-check
git push origin --delete smoke/access-check             # clean up the throwaway branch
```

If the push is rejected with a permissions error, check team membership in the
`api-marketplace` GitHub team before assuming a tooling problem.

---

## Documentation

This repo is built from the HMCTS Spring Boot service template — for generic setup,
build commands, static analysis, and implementation pattern guides, see the
[HMCTS Marketplace Springboot template README](https://github.com/hmcts/service-hmcts-marketplace-springboot-template/blob/main/README.md)
and its [supporting docs](https://github.com/hmcts/service-hmcts-marketplace-springboot-template/blob/main/docs).

Repo-specific documentation: [Logging](docs/Logging.md).

### Contribute to This Repository

Contributions are welcome! Please see the [CONTRIBUTING.md](.github/CONTRIBUTING.md) file for guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
