# ADR 0004 - H2 file database per service with Liquibase XML migrations

|            |                                                                                                                                    |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Status     | Accepted                                                                                                                           |

| Relates to | BRD N4 (runs locally with one documented command); the brief's submission section (a README that lets reviewers set up and run it) |

## Context

Reviewers will clone the repository and run it. Each service must own its data (ADR 0001). We want the fewest possible prerequisites and a schema that is versioned in the repository. Earlier drafts planned PostgreSQL in docker-compose with Flyway; the author re-evaluated this for assessment scope.

## Options considered

| Option                                                                                                         | Pros                                                                                                                                                                                                                                                                     | Cons                                                                                                                                                                                             |
| -------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| A - H2 in file mode, one file per service, PostgreSQL compatibility mode; Liquibase migrations in XML (chosen) | No Docker, no server to install. One file per service makes the data boundary physical. Same engine in tests and at runtime. Liquibase XML change types are database-agnostic, so a later move to PostgreSQL is a driver and URL change. The author knows Liquibase well | Not a production database; not suitable for concurrent multi-instance use. H2's PostgreSQL mode is close but not identical                                                                       |
| B - PostgreSQL in docker-compose, one schema per service; Flyway SQL migrations                                | Production-like; Flyway has fewer concepts                                                                                                                                                                                                                               | Docker becomes a prerequisite for reviewers. Two schemas in one server is a softer boundary than two files. Flyway SQL would be written against PostgreSQL, so switching engines means rewriting |
| C - H2 in-memory only                                                                                          | Simplest possible                                                                                                                                                                                                                                                        | Data vanishes on restart; a demo needs persistence                                                                                                                                               |
| D - Liquibase with YAML or formatted SQL changelogs                                                            | YAML is less verbose; SQL is readable                                                                                                                                                                                                                                    | YAML adds nothing the author needs; formatted SQL is database-specific and cannot include other files, so it cannot be the master that lists one file per story                                  |

## Decision

Each service opens its own H2 file under `backend/data/` with `MODE=PostgreSQL`. Schema changes are Liquibase XML changesets, one file per story, included from an XML master changelog, pointed at by one Spring property. Tests use in-memory H2. We do not require Docker or a database server, and we do not mix changelog formats.

## Consequences

- Setup is a JDK and Node. Reviewers run three processes and nothing else.
- Seed data (E6-S2) is a Liquibase changeset with explicit IDs, applied once on first start.
- PostgreSQL remains the documented upgrade path. The change is the JDBC driver and the datasource URL per service; the changelogs apply unchanged.
- The optional local Compose workflow packages the same H2 design in three containers. Named volumes preserve each service's files and uploaded posters; it remains a convenience, not a prerequisite.
