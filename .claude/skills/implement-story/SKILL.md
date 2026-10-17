---
name: implement-story
description: Implement one Movie Hub user story end to end as a vertical slice (database, service, gRPC, GraphQL, UI, tests) following the repo conventions. Use when asked to implement, build or code a story by ID such as E2-S1.
---

# implement-story

## Input
- The story ID. Read its row in `docs/02-requirements/epics-and-stories.md`.
- `docs/03-architecture/application-architecture.md` for the data model, contracts, module layout and cross-cutting patterns (errors, request id, validation).
- `docs/03-architecture/ui-design.md` for the screen the story lives on, its wireframe and its feedback states.
- `docs/04-engineering/coding-standards.md` (Kotlin, Spring, GraphQL, gRPC, Liquibase, TypeScript, Svelte rules) and `docs/04-engineering/definition-of-done.md`.

## Reference story
E1-S1 (list people) is the shape to copy. Before writing code, read these files and mirror their structure, naming and comments:
- people-service: `changes/0001-E1-S1-create-person.xml`, `domain/Person.kt`, `repository/PersonRepository.kt`, `service/PersonService.kt`, `grpc/PeopleGrpcService.kt` and their tests.
- movie-service: `client/PeopleClient.kt` (one `call { }` per RPC, deadline and error mapping inside), `api/graphql/PersonController.kt`, `graphql/schema.graphqls` and their tests.
- frontend: `lib/api/people.ts`, `lib/components/PersonCard.svelte`, `routes/people/+page.svelte` (four-state page) and their tests.
- smoke: the two `E1-S1` lines in `scripts/verify.sh`.

## Steps (do them in this order, stop and report if a step is not needed)
1. **Plan** - list in one short message which layers the story touches and which files will change. Wait for the author's go if anything is unclear.
2. **Database** - Liquibase XML changeset `NNNN-<story>-<desc>.xml` under `db/changelog/changes/` in the owning service, included from the master. One changeset file per story; never edit an applied one.
3. **Domain and repository** - entity and Spring Data repository in the owning service.
4. **Service** - business logic with validation. Throw clear exceptions; no logic in controllers.
5. **gRPC** (only if movie-service needs people data or vice versa) - update `backend/contracts` proto, regenerate, implement server method in people-service, client call in movie-service.
6. **GraphQL** - schema change in `movie-service/src/main/resources/graphql/schema.graphqls`, then the resolver.
7. **UI** - SvelteKit route or component in `frontend/`, built from the wireframe in `ui-design.md` section 4 and the feedback-state row in section 6. Use the shared components (`Spinner`, `EmptyState`, `ErrorState`, `Toast`, `ConfirmDialog`, `Pagination`, `UploadProgress`) and the global styles; no new colours or one-off CSS. Every acceptance criterion about feedback (loading, saving, progress, success, error, confirm) must be visible on screen.
8. **Tests** - run the `gen-tests` skill for each new service, resolver and component. Then run the `verify` skill (`make verify`); add a smoke line to `scripts/verify.sh` for any new endpoint or page. Nothing is done while anything is red.
9. **Log** - row in the author's private AI usage log (`docs/05-ai/ai-usage.md`, git-ignored) with the story ID and what the author changed by hand.
10. **Review** - ask the author to review, then run the `code-reviewer` agent.

## Rules
- Smallest change that satisfies the acceptance criteria. No extra fields, endpoints or abstractions.
- Errors: throw `AppException` subclasses from services; never catch and map at the resolver, the edge mappers do that (application-architecture 4.1).
- Tests: no `!!`; `requireNotNull` in tests and mappers. Frontend page tests stub `gql` calls with `...Once` variants.
- Names follow `docs/00-start-here/03-repo-layout.md`.
- Earlier stories stay as they are: extend an existing file rather than rewriting it, and keep its tests passing. If a story needs to change behaviour another story delivered, say so in the plan step first.
- Comment the "why", never the "what".
