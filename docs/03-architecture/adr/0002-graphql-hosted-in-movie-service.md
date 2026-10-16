# ADR 0002 - GraphQL hosted in movie-service

| | |
|---|---|
| Status | Accepted |
| Date | 2026-10-09 |
| Relates to | Brief constraints (BRD section 7: GraphQL between frontend and backend); E1-E5 (every story uses the API) |

## Context

The brief requires GraphQL between the frontend and the backend and gRPC between services. The frontend needs one place to ask for a movie together with its cast and creators, which live in two services (ADR 0001). Something has to host the GraphQL API and compose the two.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A - `movie-service` hosts the GraphQL API and calls `people-service` over gRPC for all people data (chosen) | One entry point for the frontend. No third service. gRPC is used for every people operation, not just a token call. movie-service already owns credits, the link between the two domains | movie-service has two jobs: movie domain and API host. People mutations pass through it |
| B - A separate GraphQL gateway service in front of both | Cleanest separation; each domain service stays pure | A third Spring Boot service to build, test, run and explain. More code for the same user-visible behaviour |
| C - Each service hosts its own GraphQL endpoint; the frontend calls both | No composition code on the backend | The frontend must stitch movie and people data itself and would no longer need gRPC at all, which weakens the brief's requirement 2 |

## Decision

`movie-service` serves the single GraphQL schema. `people-service` exposes gRPC only and has no HTTP API apart from a health endpoint. We do not build a gateway now.

## Consequences

- The frontend talks to one URL; the GraphQL schema is the one contract the UI depends on.
- Every people operation from the UI is GraphQL -> movie-service -> gRPC -> people-service, so gRPC is exercised by E1, E2 (movie page loads its people), E4 and E5.
- Upgrade path: a gateway can be added later as `backend/api-gateway`; movie-service would drop its `api.graphql` package and nothing else moves.
