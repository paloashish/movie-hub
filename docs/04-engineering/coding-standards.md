# Coding Standards

One page. Every prompt, skill and reviewer in this repo follows it. Where an official guide exists we follow it and only list what we add or restrict. Naming rules are in `docs/00-start-here/03-repo-layout.md`.

## Kotlin

Base: [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

- `data class` for values and DTOs; `enum class` for fixed sets; `sealed class` for closed hierarchies (as in `AppException`).
- No `!!`. Nullability is explicit and handled where the value is produced.
- No wildcard imports. Four-space indent. Trailing commas on multi-line declarations.
- Functions do one thing and fit on a screen. Expression bodies for one-liners.
- Comments say why, not what. A reviewer must be able to explain every line.

## Spring Boot

Base: [Spring Boot reference](https://docs.spring.io/spring-boot/reference/).

- Constructor injection only. No `@Autowired` on fields, no `lateinit` for dependencies.
- `@ConfigurationProperties` classes for settings; no `@Value` scattered through code.
- Controllers map transport to a service call and return. No business logic, no repository access.
- One service class per aggregate (`MovieService`, `CreditService`, `PosterService`, `PersonService`). Services throw `AppException` subtypes; nothing else throws to the edge.
- Repositories are Spring Data interfaces with derived or `@Query` methods. No custom SQL strings in services.
- Profiles: `local` (default) and `test`. No other profiles, no secrets.
- Transactions: `@Transactional` on service methods that write; read-only methods are plain.

## GraphQL

Base: [Spring for GraphQL reference](https://docs.spring.io/spring-graphql/reference/) and [GraphQL best practices](https://graphql.org/learn/best-practices/).

- Schema first. `schema.graphqls` is the contract; resolvers follow it, never the other way round.
- Nullability is deliberate: required fields are `!`; a lookup by id returns a nullable type.
- Input types end in `Input`; enums are `UPPER_SNAKE`; pagination returns a `*Page` type with `items`, `page`, `size`, `total`.
- Errors only through `GraphQlExceptionResolver` with `extensions.code`. Resolvers never catch and rewrap.
- One `@Controller` per aggregate, plus `SearchController` for the cross-aggregate search; `@QueryMapping`, `@MutationMapping`, `@SchemaMapping` for nested fields that need another service (for example `Movie.credits.person`).

## gRPC and Liquibase

- Proto package `moviehub.people.v1`; messages are nouns, RPCs are verbs; every RPC has its own request message.
- Only `PeopleClient` sees gRPC types in movie-service; only `PeopleGrpcService` sees them in people-service.
- Liquibase: XML, one changeset file per story named `NNNN-<story>-<desc>.xml`, included from the master in order. An applied changeset is never edited; add a new one. The seed changeset `0099-E6-S2-seed.xml` follows the same rule.

## TypeScript and Svelte

Base: [Svelte docs](https://svelte.dev/docs/svelte) (runes mode), [SvelteKit docs](https://svelte.dev/docs/kit), TypeScript `strict`.

- `strict: true`; no `any`; types for every API response live in `lib/types/graphql.ts`.
- All server calls go through `lib/api/*`; components and pages never call `fetch` directly.
- Imports from `lib` use the SvelteKit 3 alias with the file extension: `#lib/types/graphql.ts`, `#lib/components/Spinner.svelte`. It is a Node subpath import, so the exact filename is required. Inside `lib` itself, plain relative imports are fine.
- Environment variables are declared once in `src/env.ts` with `defineEnvVars` and read from `$app/env/public`; nothing else reads `process.env` or `import.meta.env`.
- Every list and detail page uses the four-state pattern: `loading | ready | empty | error` with `Spinner`, content, `EmptyState`, `ErrorState` + Retry.
- Every mutation shows a saving state and ends with a toast; every delete goes through `ConfirmDialog`.
- Every `<input>` has a `<label>`; every image has `alt`; buttons are `<button>`.
- Components are small and named after what they show. Shared state only in `lib/stores`.

## Tests, comments, commits

- Tests follow `.claude/skills/gen-tests/SKILL.md`: one behaviour per test, Arrange-Act-Assert, edge cases from the checklist.
- A file is done when its author can explain every line in a walkthrough. If a generated line cannot be explained, it is rewritten or removed.
- Commits (once git is used): Conventional Commits with the story ID, for example `feat(people): E1-S1 list people`.
