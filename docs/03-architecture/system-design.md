# System Design - Movie Hub

| | |
|---|---|
| Version | 0.1 |
| Status | Reviewed, draft accepted |
| Date | 2026-10-09 |
| Inputs | `docs/01-business/brd.md`, `docs/02-requirements/epics-and-stories.md` |

System design answers one question: what are the big pieces and how do they talk? Details of technology and code structure live in the technical and application architecture documents.

## 1. Context - who uses the system and what it depends on

```mermaid
flowchart LR
    CM([Catalogue manager]):::person
    V([Visitor]):::person
    MH[Movie Hub<br/>Browse and manage movies,<br/>posters, cast and creators]:::system
    FS[(Local disk<br/>poster files)]:::ext

    CM -- "add, edit, delete,<br/>upload posters" --> MH
    V -- "browse, search" --> MH
    MH -- "store and serve posters" --> FS

    classDef person fill:#4F46E5,stroke:#312E81,color:#fff
    classDef system fill:#0EA5E9,stroke:#0C4A6E,color:#fff
    classDef ext fill:#94A3B8,stroke:#334155,color:#fff
```

Two personas, one system, no external services. Posters are files on local disk, kept outside the database.

## 2. Containers - the deployable pieces

```mermaid
flowchart TB
    subgraph Browser
        FE[Web app<br/>SvelteKit + TypeScript]:::fe
    end

    subgraph Backend
        MS[movie-service<br/>Kotlin + Spring Boot<br/>GraphQL API, movies, posters, credits]:::svc
        PS[people-service<br/>Kotlin + Spring Boot<br/>cast and creators]:::svc
    end

    subgraph Data
        DBM[(movie.mv.db<br/>H2 file)]:::db
        DBP[(people.mv.db<br/>H2 file)]:::db
        FS[(Poster files<br/>local folder)]:::db
    end

    FE -- "GraphQL over HTTP<br/>queries and mutations" --> MS
    FE -- "HTTP multipart<br/>poster upload with progress" --> MS
    MS -- "gRPC<br/>people lookup and CRUD" --> PS
    MS -- "JDBC" --> DBM
    PS -- "JDBC" --> DBP
    MS -- "read and write files" --> FS

    classDef fe fill:#F59E0B,stroke:#78350F,color:#fff
    classDef svc fill:#10B981,stroke:#064E3B,color:#fff
    classDef db fill:#64748B,stroke:#1E293B,color:#fff
```

| Container | Owns | Talks to |
|-----------|------|----------|
| Web app | Screens, forms, search box, upload progress, all user feedback | movie-service only |
| movie-service | Movies, posters, credits (movie + person + role). Hosts the single GraphQL API for the frontend | people-service over gRPC, its own H2 file, poster files |
| people-service | People (cast and creators): create, read, update, delete, search | Its own H2 file |
| H2 files | One file database per service. A service never opens the other's file | - |
| Poster files | JPG or PNG files named by movie ID, served as static URLs | - |

Why one GraphQL entry point: the frontend needs one place to ask for a movie with its people. movie-service already owns the credits that link the two, so it is the natural host. people-service stays a pure domain service with a gRPC interface only. See ADR 0002.

## 3. Key flows

### 3.1 Open a movie page (read across both services)

```mermaid
sequenceDiagram
    autonumber
    actor V as Visitor
    participant FE as Web app
    participant MS as movie-service
    participant PS as people-service
    participant DBM as movie db
    participant DBP as people db

    V->>FE: open /movies/42
    FE->>FE: show loading state
    FE->>MS: GraphQL query movie(id: 42) { title, year, posterUrl, credits { role, person { name } } }
    MS->>DBM: select movie 42 and its credits
    MS->>PS: gRPC GetPeople(ids: [7, 9, 12])
    PS->>DBP: select people by ids
    PS-->>MS: people
    MS-->>FE: movie with credits and people
    FE->>V: render poster, details, Cast, Creators
```

### 3.2 Upload a poster (write with progress)

```mermaid
sequenceDiagram
    autonumber
    actor CM as Catalogue manager
    participant FE as Web app
    participant MS as movie-service
    participant FS as Poster files
    participant DB as movie db

    CM->>FE: pick poster.jpg for movie 42
    FE->>FE: validate type and size (JPG/PNG, max 10 MB)
    FE->>MS: HTTP multipart POST /movies/42/poster
    loop upload progress events
        FE->>CM: progress bar 0 to 100 %
    end
    MS->>MS: validate type and size again
    MS->>FS: write file 42.jpg
    MS->>DB: update movie 42 poster_url
    MS-->>FE: 200 { posterUrl }
    FE->>CM: show poster and success message
    FE-->>CM: on failure show error and Retry, old poster unchanged
```

### 3.3 Delete a person (write across both services)

Deleting a person must also remove their credits, which live in movie-service. The GraphQL mutation in movie-service deletes the credits in its own database first, then calls people-service over gRPC to delete the person. If the gRPC call fails the mutation returns an error and the credits deletion is rolled back.

## 4. Decisions this design depends on

| ADR | Decision |
|-----|----------|
| 0001 | Two services split by domain: movies and people |
| 0002 | GraphQL hosted in movie-service, people-service exposes gRPC only |
| 0003 | Poster upload over plain HTTP multipart, files on local disk |
| 0004 | H2 file database per service, Liquibase migrations in XML; PostgreSQL as the upgrade path |
| 0006 | Java blocking gRPC stubs called from Kotlin |
| 0007 | One error hierarchy with one error code per boundary |
| 0005 | Search is a database query, no search engine |

## 5. Out of scope for this design

Authentication, multiple instances of a service, message queues, caching layers, cloud storage, a server database. All are possible later without changing the container boundaries.
