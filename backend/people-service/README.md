# people-service

Owns cast and creators, called "people" in the code. Its only business API is gRPC on :9090, defined in `backend/contracts`; the browser never talks to it directly. HTTP :8081 serves `/actuator/health` only.

- Database `backend/data/people.mv.db`, one table `person`
- Deleting a person is driven by movie-service, which removes the person's credits first and then calls `DeletePerson` here, in one transaction on its side

Packages under `com.moviehub.people`:

| Package | What |
|---------|------|
| `grpc` | `PeopleGrpcService`: one method per RPC, converts to and from protobuf, no logic |
| `service` | `PersonService`: validation and the rules |
| `domain`, `repository` | the JPA entity and Spring Data repository |
| `common`, `config` | `GrpcExceptionAdvice` maps `AppException` to a gRPC status; `RequestIdConfig` registers the request id interceptor |

Resources: `db/changelog/` (Liquibase XML, `0001` creates the table, `0099` is the seed). Start with `./gradlew :people-service:bootRun`, or `make up` from the root.
