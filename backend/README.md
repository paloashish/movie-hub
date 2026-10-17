# Movie Hub backend

One Gradle multi-project (Kotlin DSL, JDK 21) with two Spring Boot services and two shared libraries.

| Module | What it is | Talks |
|--------|-----------|-------|
| `movie-service/` | movies, posters, credits; hosts the GraphQL API the frontend uses and the one REST endpoint (poster upload) | HTTP :8080; gRPC client of people-service |
| `people-service/` | cast and creators (people) | gRPC server :9090; actuator on :8081 |
| `contracts/` | `people.proto` and the Java stubs generated from it | used by both services |
| `common/` | the error hierarchy (`AppException`, five codes) and the request id filter and interceptors | used by both services |

The split and why GraphQL lives in movie-service are ADRs 0001 and 0002 in `docs/03-architecture/adr/`.

## Run and test

Normally from the repo root with `make up`, `make test`, `make verify`. On its own:

```
./gradlew :people-service:bootRun     # start first, movie-service calls it
./gradlew :movie-service:bootRun
./gradlew test                        # JUnit 6 + MockK, 225 tests
```

No profile means `local`: each service opens an H2 file database and, on an empty database, loads the sample catalogue (the `seed` Liquibase context). Tests use the `test` profile: H2 in memory, no seed.

## Runtime files

Everything a running service writes goes to `backend/data/`, git-ignored and deleted by `make reset`:

- `movie.mv.db`, `people.mv.db`: the two H2 databases
- `posters/`: uploaded poster files
- `logs/movie-service.log`, `logs/people-service.log`: the same JSON lines as the console

In Docker the same files live in two named volumes (`compose.yaml`).

## Where to read next

Layers, contracts, error handling and the request id: `docs/03-architecture/application-architecture.md`. Kotlin, Spring, GraphQL, gRPC and Liquibase rules: `docs/04-engineering/coding-standards.md`. Each module has a short README of its own.
