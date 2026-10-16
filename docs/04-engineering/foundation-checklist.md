# Foundation Checklist

Everything Phase 1 must put in place before the first story. One row per item: what it is, where it lives, how to prove it works. The `code-reviewer` agent reviews the foundation against this list; a row is done only when its check passes.

Status: `[ ]` to do, `[~]` partly done, `[x]` done and verified.

## A. Build and run

| | Item | Where | How to verify |
|---|------|-------|---------------|
| [x] | Gradle multi-project, Kotlin DSL, version catalog | `backend/settings.gradle.kts`, `backend/gradle/libs.versions.toml` | `cd backend && ./gradlew build` passes |
| [x] | `contracts` module generates Java gRPC stubs from `people.proto` | `backend/contracts/` | Generated `PeopleServiceGrpc` class exists after build |
| [x] | `common` module: `AppException`, `ErrorCode`, `RequestId`, `RequestIdFilter`, gRPC client and server interceptors (15 tests) | `backend/common/` | Both services depend on it; no duplicated exception classes |
| [x] | `movie-service` starts on :8080 with H2 file and Liquibase master changelog | `backend/movie-service/` | `./gradlew :movie-service:bootRun`; `backend/data/movie.mv.db` appears |
| [x] | `people-service` starts gRPC on :9090 and HTTP on :8081 with its own H2 file | `backend/people-service/` | `./gradlew :people-service:bootRun`; `backend/data/people.mv.db` appears |
| [x] | Frontend starts on :5173 and renders the layout | `frontend/` | `npm run dev`; header with Movies, People, Search visible |
| [x] | `Makefile`: `make install`, `make up`, `make test`, `make verify`, `make down` | repo root | Each target runs; `make test` is green |
| [x] | `scripts/verify.sh` + `verify` skill: build, tests, live smoke check in one command | repo root, `.claude/skills/verify/` | `make verify` prints `VERIFY: ALL PASSED` | repo root | Each target runs; `make test` is green |
| [x] | `.gitignore` covers `backend/data/`, build outputs, `node_modules` | repo root | `git status` clean after a run (once git exists) |
| [x] | README skeleton: prerequisites, run, test, verify, API URLs | repo root | A newcomer can start the app from the README alone |

## B. Observability and API discovery

| | Item | Where | How to verify |
|---|------|-------|---------------|
| [x] | Actuator health on both services | `spring-boot-starter-actuator` | `GET :8080/actuator/health` and `GET :8081/actuator/health` return `UP` |
| [x] | GraphiQL enabled in local profile | `spring.graphql.graphiql.enabled=true` | `GET :8080/graphiql` opens; `{ __typename }` runs |
| [x] | Swagger UI via springdoc 3.1.1 (compatible with Boot 4.1.1) | `movie-service` dependency | `GET :8080/swagger-ui.html` returns 200 (in `make verify`); shows `POST /movies/{id}/poster` once E3-S1 adds it |
| [x] | Structured JSON logs on the console in the `local` profile; plain text in tests | `logging.structured.format.console=ecs` under `on-profile: local`; `spring.profiles.default: local` | One JSON line per request with `requestId` (verified with curl) |
| [x] | Request ID: created or read from `X-Request-Id`, returned in the response, forwarded over gRPC metadata, present in people-service logs | `common` filter and interceptors, registered in each service's `RequestIdConfig` | `make verify` sends a people query with `X-Request-Id: verify-e1s1` and finds it in both services' logs |

## C. Errors and validation

| | Item | Where | How to verify |
|---|------|-------|---------------|
| [x] | `AppException` sealed hierarchy with `ErrorCode` (NOT_FOUND, VALIDATION, CONFLICT, UNAVAILABLE, INTERNAL) | `common` | Unit test per subtype (4 tests) |
| [x] | GraphQL error mapper: `extensions.code` set, message user-safe, unexpected exceptions become INTERNAL with stack logged | `movie-service` `GraphQlExceptionResolver` | `@GraphQlTest` with a resolver that throws each type (5 tests) |
| [x] | REST error mapper: RFC 9457 `ProblemDetail` with `code`; 404, 400, 409, 503, 500, plus 404 for unknown URLs | `movie-service` `RestExceptionHandler` | MockMvc test per status (6 tests) |
| [x] | gRPC server mapper: `@GrpcAdvice` maps `AppException` to `Status` codes | `people-service` `GrpcExceptionAdvice` | Unit test per code (5 tests); end-to-end over gRPC covered by E1-S1 |
| [x] | gRPC client mapper: `PeopleClient.call` + `GrpcStatusMapper` map `StatusRuntimeException` back to `AppException`; stub bean wired by `@ImportGrpcClients` | `movie-service` `client/` | Unit tests per status (6); context test proves the stub bean exists |
| [ ] | Bean Validation wired: `@Valid` on GraphQL inputs produces a VALIDATION error with the field name | `movie-service` | Resolver already maps violations; wiring is proven by E2-S3 (first validated input), not by a foundation test |
| [x] | Frontend `ApiError { code, message }` thrown by `client.ts`; toast store; `Toast`, `EmptyState`, `ErrorState` with Retry, `ConfirmDialog` | `frontend/src/lib` | 10 client tests + 5 store tests + 10 component tests |
| [x] | Frontend shell and global styles: `src/app.css` with the colour tokens, `.btn`, `.field`, `.card`, `.grid` and the 640 px media query from `ui-design.md`; layout with header, nav, main, footer, toast outlet; components use the tokens | `frontend/src/app.css`, `routes/+layout.svelte` | 2 layout tests; `npm run check` clean; every component colour is a `var(--color-*)` |
| [x] | CORS allows the frontend dev origin, configured by property; `X-Request-Id` exposed to the browser | `movie-service` `CorsConfig` + `moviehub.cors.allowed-origins` | 3 MockMvc tests; preflight check in `make verify` |

## D. Standards and reference

| | Item | Where | How to verify |
|---|------|-------|---------------|
| [x] | `coding-standards.md` (one page), written before scaffolding so the scaffold follows it | `docs/04-engineering/` | `reviewer` pass done, 14 consistency fixes applied |
| [x] | `testing-strategy.md` | `docs/04-engineering/` | `reviewer` pass done |
| [x] | `definition-of-done.md` | `docs/04-engineering/` | `reviewer` pass done |
| [x] | Walking skeleton E1-S1 (list people) end to end | all layers | Built, verified, `code-reviewer` REVISE with 8 findings, all applied, verify green again |
| [x] | `implement-story` and `gen-tests` skills point at the standards and at E1-S1 as the reference | `.claude/skills/` | Skill files updated, draft headers removed, reference file lists added |

## Deliberately not in the foundation

Authentication, rate limiting, caching, metrics dashboards, distributed tracing beyond the request ID, CI beyond an optional test workflow, Docker. Each is out of scope for the brief; see BRD section 5.
