# Assessment Summary

Source: [brief/Technical-Assessment.pdf](brief/Technical-Assessment.pdf).

## What to build

A small movie catalogue web application.

| Layer | Required technology |
|-------|---------------------|
| Frontend | Svelte with TypeScript |
| Backend | At least two Spring Boot services written in Kotlin |
| Frontend to backend | GraphQL |
| Service to service | gRPC |
| Tests | Unit tests with complete cases, including edge cases |

## Functional requirements

1. List movies with artwork (poster image).
2. Add, update and delete movies.
3. Upload movie artwork.
4. List cast (actors) and creators (directors, writers, producers).
5. Add, update and delete cast and creators.
6. Search movies, cast and creators.
7. Any other functionality we think the product should have.

## Non-functional requirements

- **Responsive** web app that works on desktop, tablet and mobile.
- **Sufficient user feedback** for every operation:

| Action | Expected feedback |
|--------|-------------------|
| Listing | Loading indicator, then results or an empty state |
| Adding or updating | Saving indicator, then success or error message |
| Deleting | Confirmation first, then result feedback |
| Searching | Loading indicator, matching results, or a "no results" message |
| Uploading artwork | Progress indicator (percentage where possible), completion status, error handling |

## Submission and how it is judged

- A git repository containing the solution.
- A README that explains setup, how to run the project, and **how AI tools were used**.
- A follow-up walkthrough session where the candidate explains the solution.

The brief is explicit: AI tools such as Claude Code are allowed as a productivity aid, but every line of submitted code must be explainable. The reviewers are "looking for software engineers, not prompt engineers". This is why the project documents its process, keeps the architecture small, and logs AI usage.

## What was decided before this folder existed

Early research notes ([04-research-notes.md](04-research-notes.md)) compared service splits and repository styles and concluded:

- Split services by business domain (movies vs people) rather than by read/write (CQRS). Domain split is simpler to explain and needs no data synchronisation.
- Use a single monorepo for frontend, services, contracts and docs.
- Do not add admin/user roles or authentication unless the brief asks for it. It is a candidate for the "any other functionality" item, not a requirement.

Those conclusions are carried forward in [02-decisions-and-plan.md](02-decisions-and-plan.md).
