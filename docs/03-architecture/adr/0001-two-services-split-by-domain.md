# ADR 0001 - Two services split by domain

| | |
|---|---|
| Status | Accepted |
| Date | 2026-10-09 |
| Relates to | Brief constraints (BRD section 7: at least two Spring Boot services, gRPC between them); BRD F1-F7; epics E1, E2, E4 |

## Context

The brief requires at least two Spring Boot services that talk to each other over gRPC. The product has two natural business areas: movies (with posters and credits) and people (cast and creators), and every area needs create, read, update, delete and search. We need a split that is simple to explain in a walkthrough and does not create data synchronisation work.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A - Split by domain: `movie-service` and `people-service` (chosen) | Each service owns one business area and its own data. No copies of data to keep in sync. Boundaries match the words in the brief | Both services need read and write; a movie page needs a cross-service call for its people |
| B - Split by read and write (CQRS style): a write service and a read service | Clean command and query separation; reads and writes could scale independently | The read service must get fresh data after every write, which means shared storage or an eventually consistent copy. More to build and explain than a catalogue of this size needs |
| C - Split by user role: admin service and browse service | Mirrors the two personas | Authorisation is not a service boundary. Both roles use the same data and operations, so both services would share one database |

## Decision

We build `movie-service` (movies, posters, credits) and `people-service` (cast and creators), each with its own H2 database. We do not split by read/write or by role, and we do not add a third service.

## Consequences

- Each service is small and has a single reason to change; a reviewer can map each brief item to one service.
- Credits store a `person_id` by value with no cross-database foreign key. Deleting a person is a two-step operation (credits first, then the person) orchestrated by movie-service; see system design 3.3.
- Reading a movie with its people costs one gRPC call. Acceptable at this scale; a cache would be the upgrade path if ever needed.
