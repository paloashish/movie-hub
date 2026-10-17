# movie-service

Owns movies, posters and credits, and hosts the whole GraphQL API: the frontend has one door, this service, and it fetches people from people-service over gRPC when a query asks for them (ADR 0002). There is no separate gateway.

- HTTP :8080: `/graphql` (GraphiQL at `/graphiql`), `POST /movies/{id}/poster` for the upload (Swagger at `/swagger-ui.html`), `/posters/**` to serve them, `/actuator/health`
- Database `backend/data/movie.mv.db`, tables `movie` and `credit`; a credit holds a `person_id` by value, no foreign key, because the person lives in the other service's database

Packages under `com.moviehub.movie`:

| Package | What |
|---------|------|
| `api/graphql` | controllers: one method per schema field, no logic |
| `api/http` | `PosterController`, the one REST endpoint (the upload) |
| `client` | `PeopleClient`, the only class that sees gRPC types: five-second deadline, and `GrpcStatusMapper` turns a gRPC status into our exceptions |
| `service` | the rules: `MovieService`, `CreditService`, `PosterService`, `SearchService` |
| `domain`, `repository` | JPA entities and Spring Data repositories |
| `common` | the two edge mappers: `GraphQlExceptionResolver` and `RestExceptionHandler` |
| `config` | CORS, the poster folder and its static serving, the request id filter |

Resources: `graphql/schema.graphqls` (the contract with the frontend), `db/changelog/` (Liquibase XML, one changeset per story, `0099` is the seed), `seed-posters/` (ten generated SVGs for the sample catalogue). Start with `./gradlew :movie-service:bootRun` after people-service, or `make up` from the root.
