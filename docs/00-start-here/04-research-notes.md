# Early Research Notes

These are the early, pre-project notes used to understand the brief and make the first design choices. They were cleaned from a longer exploration (tutorial material and images removed) and are kept to show the reasoning that fed into the decisions in [02-decisions-and-plan.md](02-decisions-and-plan.md). Nothing here is a requirement.

## 1. Reading the brief

The brief asks for a small movie catalogue. Interpretations settled on:

| Requirement | Interpretation |
|-------------|----------------|
| List movies with artwork | A grid or list of movies showing poster, title and year. "Artwork" means the poster or cover image |
| Add, update, delete movies | Standard CRUD on a movie record (title, year, synopsis, and so on) |
| Upload movie artwork | Upload a JPG or PNG, store the file, keep a reference on the movie record, display it |
| List cast and creators | Cast are actors; creators are directors, writers, producers. Both are "people" with a role in a movie |
| Add, update, delete cast and creators | CRUD on people records |
| Search | A search box that finds movies by title and people by name |

One modelling point stood out early: a movie has many people, and a person can be in many movies. People should be independent records linked to movies through a credit (person + movie + role), not fields on the movie.

The brief does not specify where artwork is stored. Local disk with a URL reference on the movie record is enough for this scope.

## 2. How to split the two services

Three options were considered.

### Option A - split by domain (chosen)

`movie-service` handles movies, artwork and credits. `people-service` handles cast and creators. Each has its own CRUD and search. They talk over gRPC when a movie page needs people data.

- Clear business ownership, easy to explain.
- Both services need read and write, which is normal for domain services.
- No data synchronisation between services.

### Option B - split by read and write (CQRS-style)

A write service for all mutations and a read service for all queries.

- Clean command/query separation and independent scaling.
- The read service must obtain fresh data after every write, which means either shared storage or a replicated read model with eventual consistency.
- More architecture than this product needs. CQRS can also be applied inside a single service without a second deployable.

### Option C - split by user role (normal vs admin)

A browse service for normal users and an admin service for management.

- A sensible product idea, but it is an authorisation concern, not a service boundary. Both roles use the same data and the same operations.
- Would still need both services to share movie and people data.

**Conclusion:** Option A. The brief has two natural business areas, both need CRUD and search, and the domain split demonstrates gRPC without introducing synchronisation. Roles and authentication stay out unless added as an extra feature.

## 3. One repository or several

A monorepo was chosen: frontend, both services, contracts, infrastructure and docs in one git repository.

- The whole solution is reviewable in one place, which matters for an assessment.
- Shared contracts (proto files) live next to the services that use them.
- One README, one docker-compose, one CI workflow.

The first-draft layout had `frontend/`, `services/`, `contracts/` and `infrastructure/` as top-level folders. It was later revised to a `backend/` Gradle root so that each top-level folder has exactly one build tool. See [03-repo-layout.md](03-repo-layout.md).

## 4. Where GraphQL lives

GraphQL needs a backend entry point. Either one of the existing services hosts it, or a separate gateway does. A third Spring Boot service only for GraphQL was judged unnecessary at this scale, so `movie-service` hosts the GraphQL API and calls `people-service` over gRPC. A gateway remains the documented upgrade path.

## 5. Suggested build order (from the notes)

1. Define the data model: movies, people, credits and artwork.
2. Build the two Kotlin Spring Boot services and their data operations.
3. Implement gRPC between the services.
4. Expose operations through GraphQL.
5. Build the Svelte + TypeScript frontend.
6. Add artwork upload, responsive layout, loading states and error handling.
7. Test end-to-end flows and service-to-service communication.

This became the Phase 1 and Phase 2 plan, with the change that features are delivered as vertical slices per story rather than layer by layer.

## 6. Reference images (added 2026-10-11)

Collected while reading the brief, kept in `brief/reference/` for the walkthrough. They are references only; the posters are copyrighted and are not used as seed data.

| File | What it showed | What it confirmed or changed |
|------|----------------|------------------------------|
| `poster-wall.jpeg`, `movie-grid-example.jpeg` | A wall of posters; a catalogue grid with title, genres, rating | Home page is a poster grid with title and year. Genres and ratings stay out: not in the brief |
| `cast-poster-grand-budapest.jpeg` | Cast with character names | "Cast" means actors. Character name is a possible F8 extra, not a requirement |
| `admin-edit-form-example.jpeg` | A streaming CMS edit form: video URL, two images, genre, rating, featured, actor multi-select | A warning, not a model. Our form is title, year, synopsis; credits carry a role, which a multi-select cannot |
| `moviehub-screen-collection.png` | Nine-screen concept with header, nav, footer, search in the header, upload progress, people table, mobile view | Matched `ui-design.md` almost entirely. Adopted: search box in the header, Back links. Rejected: role on the person, upload on the create form, tabs, link-heavy footer |
