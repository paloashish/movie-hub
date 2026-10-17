# Decisions and Delivery Plan

> **Status (2026-10-16):** Phase 0 done; git initialised on 2026-10-16 (one commit, main). Phase 1 foundations and Phase 2's 21 stories are complete, code-reviewed and verified (`make verify`: 50 checks, 225 backend + 191 frontend tests). Phase 3 has the final README, a README in every code folder, the demo script and the AI tooling map; optional local Docker is implemented and `make docker-verify` passes; both services write a JSON log file under `backend/data/logs/`. Deferred items live in `docs/04-engineering/backlog.md`; the Docker browser walkthrough is parked there. Checkboxes below are the detailed status.

## Guiding principle

Keep every piece small enough to explain in a walkthrough. Prefer plain, well-named code over clever abstractions. Add nothing (Kubernetes, message brokers, search engines, CQRS, event sourcing) that the brief does not need.

This is an assessment, not a product. Documents are one page by default and exist to show the process, not to be exhaustive. Stories are one line plus two or three acceptance criteria each.

## Key technical decisions

Decisions 1 to 5, 9 and 10 are architecture decisions and get an ADR (0001 to 0007) in `docs/03-architecture/adr/`. Decisions 6 to 8 are working conventions and are recorded here only.

| #   | Decision                                                                                                                                                                                                                       | Reason                                                                                                                                                                                                                      |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **Two services split by domain:** `movie-service` owns movies, artwork and movie-to-person links; `people-service` owns cast and creators                                                                                      | Matches the two business areas in the brief; no read-model synchronisation; easy to explain                                                                                                                                 |
| 2   | **GraphQL is served by `movie-service`**, which calls `people-service` over gRPC for all people data                                                                                                                           | Single entry point for the frontend without a third service; makes gRPC central to every people operation rather than a token call                                                                                          |
| 3   | **Artwork upload uses one plain multipart HTTP endpoint**, not GraphQL. Files are stored in a local folder and served as static URLs                                                                                           | GraphQL multipart uploads are awkward; a plain XHR upload gives real percentage progress for free                                                                                                                           |
| 4   | **H2 file database, one file per service, Liquibase migrations in XML.** H2 in-memory for unit tests                                                                                                                           | Zero infrastructure to run: no Docker needed. One file per service makes the ownership boundary physical. Liquibase XML change types are database-agnostic, so the upgrade path to PostgreSQL is a driver and config change |
| 5   | **Search is a database query** (case-insensitive `LIKE` on title/name)                                                                                                                                                         | Good enough for the data size; no search engine to run or explain                                                                                                                                                           |
| 6   | **Diagrams are Mermaid blocks inside Markdown**                                                                                                                                                                                | Render on GitHub, no tooling, easy to keep up to date                                                                                                                                                                       |
| 7   | **Tooling:** Gradle multi-project (Kotlin DSL) for backend with a shared `contracts` module that generates gRPC stubs; npm + SvelteKit for frontend; JUnit 6 + MockK on backend; Vitest + Testing Library on frontend          | Standard, well-documented choices with minimal setup                                                                                                                                                                        |
| 8   | **AI usage is structured:** root `CLAUDE.md`, small skills (write-stories, write-adr, implement-story, gen-tests, verify, ship-story), two agents (reviewer, code-reviewer), a tooling map, commits reference story IDs | Shows AI as a disciplined productivity aid and gives the walkthrough a trail to follow                                                                                                                                      |
| 9   | **Java blocking gRPC stubs called from Kotlin;** grpc-kotlin coroutine stubs are not used (ADR 0006)                                                                                                                           | Fewer moving parts; the brief asks for Kotlin services, not Kotlin stubs                                                                                                                                                    |
| 10  | **One error hierarchy with one error code per boundary;** RFC 9457 problem details for the upload endpoint (ADR 0007)                                                                                                          | Every story reuses the same mapping; the frontend handles one error shape                                                                                                                                                   |

## Phased plan

### Phase 0 - Discover and document

Output: a reviewable set of documents before any code exists.

- [x] Start-here docs: summary, decisions, layout, research notes
- [x] AI tooling: `CLAUDE.md`, tooling map
- [x] Business Requirements Document (one page): goals, scope, out of scope, personas, success criteria
- [x] Six epics with short user stories and acceptance criteria, in one file (reviewed)
- [x] System design (context and container diagrams), technical architecture (stack, versions, wiring), application architecture (data model, contracts, layers, cross-cutting patterns) - reviewed
- [x] ADRs 0001-0007 via `/write-adr` (reviewed)
- [x] UI design (one page): how screens derive from the brief, app shell, screen map, text wireframes, responsive rules, feedback states per screen (added after E1-S1 when the gap was noticed; reviewer pending)
- [x] Skills and agents: write-stories, write-adr, implement-story, gen-tests, reviewer, code-reviewer
- [x] `git init` and first commit (2026-10-16, branch `main`, 334 files, nothing from `backend/data/`, builds or `node_modules`)

### Phase 1 - Foundation

Output: an empty but fully wired system that starts with one command, plus the standards every story follows.

Full item list with verification steps: `docs/04-engineering/foundation-checklist.md`. The `code-reviewer` agent reviews the foundation against it.

Scaffolding (built in small steps, the build is run after each one; this replaces a separate spike):

- [x] Gradle multi-project with the `contracts` module (generated gRPC stubs) and a `common` module (exceptions, error codes; request ID comes with cross-cutting)
- [x] Two Spring Boot skeletons with health endpoints, Liquibase master changelog, GraphQL ping, gRPC server listening
- [x] SvelteKit skeleton with GraphQL client, ApiError and shared layout
- [x] `Makefile` with install, up, test, verify, down
- [x] Passing tests on each side (backend 6, frontend 9) and `make verify` green

Cross-cutting concerns (decided once, used by every story):

- [x] Error handling: shared exception hierarchy, GraphQL error mapper, REST problem details, gRPC advice and client mapper (4a); frontend ApiError, toast, EmptyState, ErrorState, ConfirmDialog (4c)
- [x] Frontend shell: global stylesheet with design tokens, buttons, forms, card, responsive grid; header, nav, footer; layout tests (added after the UI design doc, 2026-10-11)
- [x] Logging: structured ECS logs in the local profile, request ID filter and gRPC interceptors (4b)
- [x] API discovery: Actuator health, GraphiQL, Swagger UI (springdoc 3.1.1)
- [x] CORS for the frontend dev origin
- [x] Validation approach: resolver maps Bean Validation to VALIDATION with field names; frontend shows them under the form (wiring proven in E2-S3)
- [x] Configuration profiles: local (default, H2 file, ECS logs), test (H2 in-memory)
- [x] Test setup: H2 + MockK on the backend, Vitest + Testing Library on the frontend

Standards (in `docs/04-engineering/`):

- [x] `coding-standards.md` (one page) - moved before scaffolding so code follows it from the first file
- [x] `testing-strategy.md`, `definition-of-done.md` (reviewed with coding-standards)
- [x] Walking skeleton: E1-S1 (list people) built end to end via `/implement-story`, code-reviewed (8 findings applied) and verified
- [x] Update `implement-story` and `gen-tests` skills to point at the standards, `ui-design.md` and E1-S1 as the reference story
- [ ] Optional: a small GitHub Actions workflow that runs all tests
- [x] Optional (Phase 3): local Docker Compose files implemented and `make docker-verify` passes

### Phase 2 - Deliver by epic, in vertical slices

Each story is delivered with `/ship-story <ID>`: built end to end with the `implement-story` steps (copying reference story E1-S1: Liquibase changeset, domain and repository, service, gRPC if cross-service, GraphQL, UI, tests), reviewed by the `code-reviewer` agent, fixed, verified again, then checked by the author in the browser against a walkthrough card before it is ticked.

| Epic                                             | Scope                                                                                                                                                                                                                                |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| E1 People                                        | Cast and creators CRUD and search                                                                                                                                                                                                    |
| E2 Movies                                        | Movie CRUD and search                                                                                                                                                                                                                |
| E3 Artwork                                       | Upload with real percentage progress, display on cards and detail page                                                                                                                                                               |
| E4 Credits                                       | Link cast and creators to movies with roles; show on movie page                                                                                                                                                                      |
| E5 Search, responsive layout and consistent look | Search movies and people; responsive grid and forms; one visual pass over every screen against `ui-design.md` and the reference image (E5-S3, last). Loading, empty, error and confirm states are acceptance criteria on every story |
| E6 Extras                                        | Sorting and seed data (pagination moved to E2). Done                                                                                                                                                                                 |

### Phase 3 - Harden and hand over

- [x] Edge-case tests on every service and component (covered story by story through the gen-tests checklist and the code reviews; ticked 2026-10-16)
- [x] Final README: setup, run, test, verify, URLs, where to look, how AI was used (2026-10-12)
- [x] AI usage: summarised in the README and the tooling map
- [x] Demo script and walkthrough outline for the interview: `docs/00-start-here/05-demo-script.md` (2026-10-16)

## Next task

Continue the study series with the next concept the author asks about, or pick up the parked demo script or git init on request.
