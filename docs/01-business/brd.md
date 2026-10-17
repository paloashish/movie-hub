# Business Requirements Document - Movie Hub

| | |
|---|---|
| Version | 0.1 (draft) |
| Status | Reviewed, draft accepted |
| Source | `docs/00-start-here/brief/Technical-Assessment.pdf` |

## 1. Purpose

Movie Hub is a small web application for managing a movie catalogue: the movies themselves, their poster artwork, and the people who made them (cast and creators). It exists to demonstrate, within a technical assessment, that a small product can be delivered with a clear process: requirements, design, build, test and documentation.

## 2. Background

The assessment brief asks for a Svelte + TypeScript frontend and at least two Kotlin Spring Boot services that talk to each other over gRPC and to the frontend over GraphQL. The functional scope is fixed by the brief; the product shape, data model and user experience are ours to define.

## 3. Users

| Persona | Needs |
|---------|-------|
| **Catalogue manager** | Add and maintain movies, upload posters, maintain cast and creators, link people to movies. Needs clear feedback that each action worked |
| **Visitor** | Find a movie or a person quickly and see the poster, details and credits on any device |

There is no login. Both personas use the same screens. Roles and authentication are out of scope.

## 4. Goals

| ID | Goal | Measure |
|----|------|---------|
| G1 | A manager can keep the catalogue up to date without technical help | Every create, update, delete and upload is possible from the UI with clear success or error feedback |
| G2 | A visitor can find any movie or person in seconds | Search by title or name returns results with a loading state and a clear empty state |
| G3 | The app is usable on phone, tablet and desktop | Layout adapts; no horizontal scrolling at phone width |
| G4 | The solution is explainable and testable | Unit tests with edge cases on both frontend and backend; every file explainable in a walkthrough |

## 5. Scope

### In scope

| ID | Feature | Brief item |
|----|---------|------------|
| F1 | List movies with poster artwork, in pages | 1 |
| F2 | Create, update and delete movies | 2 |
| F3 | Upload poster artwork for a movie with visible progress | 3 |
| F4 | List cast and creators | 4 |
| F5 | Create, update and delete cast and creators | 5 |
| F6 | Search movies and people by title or name | 6 |
| F7 | Link people to movies with a role (actor, director, writer, producer) and show credits on the movie page | 7 (our addition) |
| F8 | Sorting on lists, and sample seed data | 7 (stretch, not required for completion) |

### Out of scope

Authentication and roles, ratings and reviews, streaming or playback, importing data from external movie databases, multi-language content, cloud deployment.

## 6. Non-functional requirements

| ID | Requirement |
|----|-------------|
| N1 | Responsive layout for phone, tablet and desktop |
| N2 | User feedback for every operation: loading, saving, upload progress with percentage, success, error, and confirmation before delete |
| N3 | Unit tests with edge cases for backend services and frontend components |
| N4 | Runs locally with one documented command, described in the README |
| N5 | Basic accessibility: visible form labels, keyboard-reachable actions, alt text on images |

## 7. Constraints

Svelte + TypeScript frontend. At least two Spring Boot services in Kotlin. gRPC between services. GraphQL between frontend and backend. AI tools may be used, but all code must be explainable by the author.

## 8. Success criteria

The submission is complete when F1 to F7 work end to end, N1 to N5 are met, the solution is delivered as a git repository, the README explains setup, run and AI usage, and the author can walk a reviewer through any file. F8 is a stretch goal.

## 9. Assumptions

- Posters are JPG or PNG files up to 10 MB.
- A release year is valid from 1888 (the first film) to five years after the current year.
- The movie list shows 12 movies per page.
- The data volume is small (hundreds of records); no external storage or search service is expected.
- One catalogue; no multi-tenancy.
