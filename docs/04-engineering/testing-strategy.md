# Testing Strategy

One page. What we test, where, with what, and what "enough" means. The `gen-tests` skill holds the edge-case checklist; this page holds the rules.

## Levels

| Level | Backend | Frontend | Runs in |
|-------|---------|----------|---------|
| Unit | Service classes, mappers, validators: JUnit 6 + MockK, no Spring context | `lib/api`, `lib/stores`, pure helpers: Vitest in node | `make test` |
| Component | Resolvers with `@GraphQlTest`, REST with `@WebMvcTest`, repositories with `@DataJpaTest` (in-memory H2), gRPC service methods as plain unit tests with a mocked `StreamObserver` (the wiring is proven by the smoke check); `@SpringBootTest` only for context and config wiring | Svelte components and pages with Testing Library in jsdom (`X.svelte.test.ts`) | `make test` |
| Smoke | Both services started, health, GraphQL ping, gRPC port, request ID, CORS, Swagger, GraphiQL | Dev server serves the layout | `make verify`, about a minute |
| End to end | Manual demo script in Phase 3; no automated browser tests. `node scripts/check-responsive.mjs <movieId> <personId>` (needs the app up and Google Chrome) opens every page at 375, 768 and 1280 px and reports sideways scroll, unlabelled fields, images without alt and nameless buttons | | By hand before submission |

No automated end-to-end suite on purpose: the brief asks for unit tests with edge cases, and a Playwright layer would cost more than it proves at this size.

## What every story must cover

For each story, tests exist at the level where the behaviour lives:

1. **Service rules** as unit tests: every validation rule, every `AppException` the service can throw, the happy path.
2. **The edge** as a component test: the resolver, REST endpoint or gRPC method returns the right data and the right `code` for each error.
3. **The UI** as component tests: loading, empty, error-with-Retry, success, and confirm-before-delete where the story has them.
4. **The edge-case checklist** in `.claude/skills/gen-tests/SKILL.md`, every line that applies.
5. **A smoke line** in `scripts/verify.sh` for any new endpoint or page.

## Rules

- One behaviour per test, named `should <behaviour>`, adding `when <condition>` where there is one; Arrange-Act-Assert with blank lines between the three.
- Mock only at boundaries: repositories, the gRPC stub, `fetch` or `XMLHttpRequest`. Never mock the class under test.
- Tests run against in-memory H2; no file database, no network, no sleeps. Temp directories for poster files.
- A test proves intent, not current behaviour: if a test asserts something the checklist or story contradicts, the test is wrong.
- A failing test is reported, never deleted or skipped to get green.
- Coverage is not a target number. The gate is "every rule and every error path has a test", checked by the `code-reviewer` agent.

## Where the tests live

| Code | Test |
|------|------|
| `backend/<module>/src/main/kotlin/.../X.kt` | `backend/<module>/src/test/kotlin/.../XTest.kt` |
| Test-only GraphQL fields | `backend/movie-service/src/test/resources/graphql/*.graphqls` (merged via `classpath*:`) |
| `frontend/src/lib/**/x.ts` | `frontend/src/lib/**/x.test.ts` (node) |
| `frontend/src/lib/components/X.svelte` | `frontend/src/lib/components/X.svelte.test.ts` (jsdom); small related components may share one file, as `Feedback.svelte.test.ts` does |

## Current state (end of foundation)

43 backend tests across three modules (`contracts` is generated code, no tests), 24 frontend tests, 13 verify checks of which 9 are smoke. All green on `make verify`.
