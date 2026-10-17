# Repository Layout and Naming

This document records the layout we chose, the alternatives we weighed, and the naming rules. It is written so a beginner can find anything in the repo within a minute.

## Recommended layout

```
movie-hub/
├── README.md                     setup, run, test, AI usage (the submission README)
├── CLAUDE.md                     conventions Claude Code follows in this repo
├── Makefile                      make up / make test / make verify - one-word commands for beginners
├── compose.yaml                  optional Docker stack (make docker-up); Dockerfiles live in backend/ and frontend/
├── scripts/                      verify.sh (the 50-check verifier), verify-docker.sh, two headless-Chrome scripts
├── .claude/                      README.md points at the tooling map
│   ├── skills/                   write-stories, write-adr, implement-story, gen-tests, verify, ship-story
│   └── agents/                   reviewer (documents), code-reviewer (code)
├── docs/
│   ├── 00-start-here/
│   ├── 01-business/              brd.md
│   ├── 02-requirements/          epics-and-stories.md (one file, one table per epic)
│   ├── 03-architecture/          system-design.md, technical-architecture.md,
│   │   └── adr/                  application-architecture.md, 0001-domain-split.md ...
│   ├── 04-engineering/           coding-standards.md, testing-strategy.md, definition-of-done.md, backlog.md
│   └── 05-ai/                    ai-tooling-map.md
├── backend/                      Gradle multi-project root (Kotlin DSL)
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradlew, gradle/
│   ├── contracts/                .proto files + generated gRPC stubs (shared library)
│   ├── common/                   AppException, ErrorCode, request-ID filter and gRPC interceptors (shared library)
│   ├── movie-service/            movies, artwork, credits, GraphQL API, gRPC client
│   ├── people-service/           cast and creators, gRPC server
│   └── data/                     git-ignored: H2 files, posters/ and logs/ written at run time
├── frontend/                     SvelteKit + TypeScript
└── infra/
    └── seed/                     (unused: the sample catalogue lives in each service's 0099 changeset and movie-service/src/main/resources/seed-posters/)
```

## Why this layout

The previous draft had `services/`, `contracts/` and `infrastructure/` as top-level siblings. It read well but had one practical problem: Gradle needs a root project, and a `contracts` module that sits outside that root needs a manual path override in `settings.gradle.kts`. Putting all JVM code under `backend/` fixes that and gives three further benefits:

- **Clean repo root.** No `gradlew`, `build.gradle.kts` or `.gradle/` at the top level next to the docs.
- **One build tool per folder.** `backend/` is Gradle, `frontend/` is npm. A newcomer never has to ask which tool applies where.
- **Obvious ownership.** The gRPC contract is a backend concern, so it lives with the backend. The GraphQL schema is owned by `movie-service` and lives in its resources, where Spring for GraphQL loads it from anyway.

## Alternatives considered

| Layout | Shape | Verdict |
|--------|-------|---------|
| **A. Flat by layer** (first draft) | `frontend/`, `services/`, `contracts/`, `infrastructure/` | Readable, but Gradle root ends up either at repo root (clutter) or needs path overrides for `contracts/` |
| **B. Backend-rooted** (chosen) | `backend/` (Gradle root with four modules), `frontend/`, `infra/` | One tool per folder, clean root, contracts module is a normal Gradle subproject |
| **C. apps + packages** (JS monorepo style) | `apps/web`, `apps/movie-service`, `apps/people-service`, `packages/contracts` | Familiar to Node developers, but mixes Gradle and npm projects under one `apps/` folder and implies a Turborepo or Nx setup we do not need |
| **D. One repo per service** | three repos | Standard for large teams; for an assessment it hides the whole picture and makes review harder |

## Service names

| Option | Example | Verdict |
|--------|---------|---------|
| **Domain nouns + `-service`** (chosen) | `movie-service`, `people-service` | Uses the brief's own words ("movies", "cast and creators" = people). Reviewers map requirement to service instantly |
| Industry terms | `catalog-service`, `talent-service` | Sounds polished but introduces words that are not in the brief; "talent" needs explaining |
| Bare nouns | `movies`, `people` | Too terse once Docker images, Gradle modules and log lines all use the name |
| Separate GraphQL gateway | `api-gateway` + `movie-service` + `people-service` | Cleanest separation, but a third service to build, test and explain. Documented as the upgrade path in the ADR, not built now |

One naming note: `movie-service` also hosts the GraphQL API. That is a deliberate choice (ADR 2) and is stated in its README so nobody expects a separate gateway.

## Naming rules

| Thing | Rule | Example |
|-------|------|---------|
| Folders and files | kebab-case | `people-service`, `testing-strategy.md` |
| Gradle project names | same as folder | `:movie-service`, `:contracts` |
| Kotlin root package | `com.moviehub.<service>` | `com.moviehub.movie`, `com.moviehub.people` |
| Docker images | `moviehub/<service>` | `moviehub/movie-service` |
| Database files | one H2 file per service, git-ignored | `backend/data/movie.mv.db`, `backend/data/people.mv.db` |
| Proto packages | `moviehub.<domain>.v1` | `moviehub.people.v1` |
| GraphQL types | PascalCase, inputs end in `Input` | `Movie`, `CreateMovieInput` |
| Svelte components | PascalCase | `MovieCard.svelte` |
| Code term for artwork | `poster` everywhere in code; "artwork" only when quoting the brief | `PosterService`, `posterUrl` |
| Epics and stories | `E<n>` and `E<n>-S<n>` | `E3-S2 Upload shows percentage progress` |
| Branches | `<type>/E<n>-S<n>-short-slug` | `feat/E3-S2-upload-progress` |
| Commits | Conventional Commits with story ID | `feat(movie): E3-S2 upload progress endpoint` |

## Inside each service

Both services use the same simple layered layout so a reader learns it once.

```
backend/movie-service/src/main/kotlin/com/moviehub/movie/
├── MovieServiceApplication.kt
├── api/
│   ├── graphql/        MovieController.kt, PersonController.kt, SearchController.kt  (Spring for GraphQL controllers)
│   │                   MovieInput.kt, PersonInput.kt, MoviePage.kt, MaxYearsAhead.kt    (the edge's own types, kept beside their controllers)
│   └── http/           PosterController.kt                  (multipart upload)
├── domain/             Movie.kt, Credit.kt, Role.kt
├── repository/         MovieRepository.kt, CreditRepository.kt
├── service/            MovieService.kt, CreditService.kt, PosterService.kt
├── client/             PeopleClient.kt                      (wraps the generated gRPC stub; maps gRPC errors)
├── common/             GraphQlExceptionResolver.kt, RestExceptionHandler.kt   (AppException and ErrorCode come from backend/common)
└── config/             PostersProperties.kt, GrpcClientConfig.kt, CorsConfig.kt
src/main/resources/
├── graphql/schema.graphqls
├── db/changelog/db.changelog-master.xml
├── db/changelog/changes/0001-E1-S1-create-person.xml ... 0099-E6-S2-seed.xml
└── application.yml

backend/people-service/src/main/kotlin/com/moviehub/people/
├── PeopleServiceApplication.kt
├── grpc/               PeopleGrpcService.kt                 (gRPC server implementation)
├── domain/             Person.kt
├── repository/         PersonRepository.kt
├── service/            PersonService.kt
├── common/             GrpcExceptionAdvice.kt                               (AppException and ErrorCode come from backend/common)
└── config/

backend/contracts/src/main/proto/moviehub/people/v1/people.proto
```

## Inside the frontend

```
frontend/src/
├── routes/
│   ├── +layout.svelte              header, nav, toast outlet
│   ├── +page.svelte                movie grid (home)
│   ├── movies/new/+page.svelte
│   ├── movies/[id]/+page.svelte    detail with poster, cast and creators
│   ├── movies/[id]/edit/+page.svelte
│   ├── people/+page.svelte
│   ├── people/[id]/+page.svelte
│   └── search/+page.svelte
└── lib/
    ├── api/            client.ts (GraphQL fetch + ApiError), movies.ts, people.ts, credits.ts, search.ts, upload.ts (XHR with progress)
    ├── components/     MovieCard, PersonCard, SearchBox, UploadProgress, Toast, ConfirmDialog, EmptyState, ErrorState, Spinner, Pagination
    ├── stores/         toast.svelte.ts (a .svelte.ts module so $state works outside components)
    └── types/          graphql.ts (hand-written types matching the schema)
```

## Open points

None blocking. If a separate GraphQL gateway is ever wanted, it becomes `backend/api-gateway` and `movie-service` drops its `api/graphql` package; nothing else moves.
