# ADR 0007 - One error hierarchy with one error code per boundary

| | |
|---|---|
| Status | Accepted |
| Date | 2026-10-09 |
| Relates to | BRD N2 (feedback: validation errors, failure and retry); every story with an error or validation criterion |

## Context

Errors cross three boundaries: service to GraphQL, service to the REST upload endpoint, and people-service to movie-service over gRPC. The stories require the UI to show the right feedback for not found, validation, duplicates and failures. Without one agreed shape, each story would invent its own and the frontend would need special cases.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A - A sealed `AppException` hierarchy with four business codes (NOT_FOUND, VALIDATION, CONFLICT, UNAVAILABLE) plus INTERNAL for unexpected errors; one mapper per boundary: GraphQL `extensions.code`, RFC 9457 problem details for REST, gRPC `Status` codes; the frontend handles one `ApiError { code, message }` (chosen) | Every story reuses the same four codes. One mapper class per boundary, each a few lines. The frontend has one error path. Easy to test | A small amount of foundation code before the first story |
| B - Let each layer throw and map what it likes | Nothing to build up front | Inconsistent messages, duplicated mapping, special cases in the UI; the kind of code that is hard to explain in a walkthrough |
| C - Return errors as data in the GraphQL schema (union result types) | Typed errors in the schema | Doubles the schema size and every resolver; heavier than this product needs |

## Decision

Services throw `AppException` subtypes only. `GraphQlExceptionResolver`, a `@RestControllerAdvice` and a `@GrpcAdvice` map them at the edges; `PeopleClient` maps gRPC status back. Unexpected exceptions become a generic INTERNAL error with the stack logged and no internals in the message. We do not use union result types or per-layer ad hoc errors.

## Consequences

- Stories implement behaviour, not error plumbing; the acceptance criteria about error messages are met by the shared mapping.
- The `code` is the contract with the frontend; messages can change without breaking the UI.
- Request IDs (application architecture 4.3) ride along, so a user-visible error can be found in both services' logs.
