# ADR 0006 - Java blocking gRPC stubs called from Kotlin

| | |
|---|---|
| Status | Accepted |
| Date | 2026-10-09 |
| Relates to | Brief constraints (BRD section 7: Kotlin services, gRPC between them) |

## Context

The brief requires Kotlin services and gRPC between them. The protobuf toolchain can generate Java stubs, Kotlin coroutine stubs (grpc-kotlin), or both. Spring Boot 4.1 ships gRPC auto-configuration built around grpc-java (see technical architecture section 1). We want the fewest moving parts that still satisfy the brief and are easy to explain.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A - Generate Java stubs only; call the blocking stub from Kotlin inside a thin `PeopleClient` wrapper (chosen) | One code generator. Works directly with Spring Boot 4.1's gRPC starters. Blocking calls are easy to read, test and debug. The Kotlin code we write and explain is our own, not generated | Blocking calls hold a thread per request; irrelevant at assessment load |
| B - grpc-kotlin coroutine stubs | Idiomatic suspend functions | A second generator plugin and `kotlinx-coroutines` on both sides, plus coroutine wiring inside Spring MVC and the GraphQL controllers. More to configure and more to explain for no user-visible benefit |
| C - Reactive stubs (RxJava or Reactor) | Non-blocking | Even more concepts; the services are plain Spring MVC, not WebFlux |

## Decision

`backend/contracts` generates Java message classes and gRPC stubs only. `PeopleClient` in movie-service wraps the blocking stub and is the only class that sees gRPC types. We do not add grpc-kotlin.

## Consequences

- The spike in Phase 1 only has to prove Spring Boot 4.1 + protobuf plugin 0.10 + Java stubs, the most common combination.
- Error mapping is in one place: `PeopleClient` turns `StatusRuntimeException` into `AppException` (ADR 0007).
- Upgrade path: add the grpc-kotlin generator later if coroutines are ever wanted; the proto file does not change.
