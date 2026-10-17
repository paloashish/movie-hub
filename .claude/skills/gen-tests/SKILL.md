---
name: gen-tests
description: Write unit tests with edge cases for a Movie Hub Kotlin class (JUnit 6 + MockK) or Svelte component (Vitest + Testing Library). Use when asked to add, write or improve tests for a file or story.
---

# gen-tests

## Input
- The file or story to test.
- Acceptance criteria from `docs/02-requirements/epics-and-stories.md` when testing a story.
- `docs/04-engineering/testing-strategy.md` for which level each behaviour is tested at, and `docs/04-engineering/coding-standards.md` for the code rules the tests must respect.

## Reference tests (copy their shape)
| Kind | Example from E1-S1 |
|------|--------------------|
| Service unit test, MockK | `people-service/.../service/PersonServiceTest.kt` |
| Repository, `@DataJpaTest` with the Liquibase changeset | `people-service/.../repository/PersonRepositoryTest.kt` |
| gRPC method, mocked `StreamObserver` | `people-service/.../grpc/PeopleGrpcServiceTest.kt` |
| gRPC client, mocked stub incl. deadline | `movie-service/.../client/PeopleClientTest.kt` |
| Resolver, `@GraphQlTest` with a mocked client bean | `movie-service/.../api/graphql/PersonControllerTest.kt` |
| API module, mocked `gql` | `frontend/src/lib/api/people.test.ts` |
| Component | `frontend/src/lib/components/PersonCard.svelte.test.ts` |
| Page with four states | `frontend/src/routes/people/page.svelte.test.ts` |

## Where tests go
| Code | Test | Tools |
|------|------|-------|
| `backend/<service>/src/main/kotlin/.../X.kt` | `backend/<service>/src/test/kotlin/.../XTest.kt` | JUnit 6, MockK, Spring Boot Test only where wiring matters |
| `frontend/src/lib/components/X.svelte` | `frontend/src/lib/components/X.svelte.test.ts` (the `.svelte.test.ts` suffix selects the jsdom project) | Vitest, @testing-library/svelte, jest-dom |
| `frontend/src/lib/api/x.ts` | `frontend/src/lib/api/x.test.ts` | Vitest with a mocked fetch/XHR |

## Edge-case checklist (cover every line that applies)
- Empty input, blank strings, whitespace-only strings.
- Missing required fields; invalid values (year out of range, negative IDs, unknown role).
- Not found (update/delete of a missing ID).
- Duplicates where uniqueness is expected.
- Boundaries: max length, max file size (10 MB), file type not JPG/PNG.
- Search: no results, case-insensitive match, partial match, special characters.
- Upload: progress events 0, 50, 100; failure mid-way; retry.
- Cross-service: people-service unavailable or returns not found.
- UI states: loading, empty, error, success, confirm-before-delete cancelled and confirmed.

## Rules
1. One behaviour per test; name it `should <behaviour> when <condition>`.
2. Arrange-Act-Assert layout with a blank line between the three parts.
3. Mock only external boundaries (repositories, gRPC stubs, HTTP). Do not mock the class under test.
4. No sleeps, no real network, no real files outside a temp directory.
5. Run the tests and paste the summary line of the result. Red tests are reported, never hidden.
6. No `!!` in Kotlin tests; use `requireNotNull`. In page tests stub each expected call with `mockResolvedValueOnce` / `mockRejectedValueOnce` and clean up with the shared `afterEach(cleanup)` in `vitest-setup-client.ts`.
7. When a test file already exists, add to it. Never delete, weaken or rename a passing test to make room for a new one; if an old test is wrong, say which and why and let the author decide.
