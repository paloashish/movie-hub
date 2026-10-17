# common

Code both services share, kept deliberately small:

- `error/AppException.kt`: the one exception hierarchy with five codes (`NOT_FOUND`, `VALIDATION`, `CONFLICT`, `UNAVAILABLE`, `INTERNAL`). Services throw these; each edge (GraphQL, REST, gRPC) maps them to its own error shape (ADR 0007).
- `logging/`: the request id. `RequestIdFilter` reads or creates `X-Request-Id` on HTTP and puts it in the MDC; the two gRPC interceptors carry it from movie-service to people-service as metadata, so one request shows the same id in both logs.

No Spring Boot auto-configuration here; each service wires what it needs in its own `config` package.
