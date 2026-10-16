# Architecture

| Document | Answers |
|----------|---------|
| [system-design.md](system-design.md) | What are the big pieces and how do they talk? Context, containers, key flows |
| [technical-architecture.md](technical-architecture.md) | Which technologies and versions, how they are wired, how it runs locally |
| [version-sources.md](version-sources.md) | Where each version number came from, with the lookup date |
| [application-architecture.md](application-architecture.md) | Data model, contracts, code layers, cross-cutting patterns, frontend patterns |
| [ui-design.md](ui-design.md) | Which screens exist, how they link, text wireframes, responsive rules, feedback states per screen |

## Architecture Decision Records

| ADR | Title | Status |
|-----|-------|--------|
| [0001](adr/0001-two-services-split-by-domain.md) | Two services split by domain | Accepted |
| [0002](adr/0002-graphql-hosted-in-movie-service.md) | GraphQL hosted in movie-service | Accepted |
| [0003](adr/0003-poster-upload-over-http-multipart-to-local-disk.md) | Poster upload over HTTP multipart, files on local disk | Accepted |
| [0004](adr/0004-h2-file-database-per-service-with-liquibase.md) | H2 file database per service with Liquibase XML migrations | Accepted |
| [0005](adr/0005-search-is-a-database-query.md) | Search is a database query | Accepted |
| [0006](adr/0006-java-blocking-grpc-stubs-from-kotlin.md) | Java blocking gRPC stubs called from Kotlin | Accepted |
| [0007](adr/0007-one-error-hierarchy-one-code-per-boundary.md) | One error hierarchy with one error code per boundary | Accepted |
