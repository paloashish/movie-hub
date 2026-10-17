# Movie Hub - working conventions for Claude Code

Movie Hub is a small technical-assessment project. The goal is to show a clean, explainable software development lifecycle (SDLC) with AI as a productivity aid. Keep everything small, plain and explainable in a walkthrough.

## Ground rules

- Simple over clever. No framework, library or pattern that the brief does not need.
- Every file must be explainable line by line by the author. Prefer obvious code and short comments on the "why".
- Documents are short. One page is the default; two is the maximum. The one exception is `docs/03-architecture/application-architecture.md`, which the foundation is built from.
- Code follows `docs/04-engineering/coding-standards.md` (Kotlin, Spring, GraphQL, gRPC, Liquibase, TypeScript, Svelte). Read it before writing code.
- Never invent requirements. The brief is `docs/00-start-here/brief/Technical-Assessment.pdf`; the plan is `docs/00-start-here/02-decisions-and-plan.md`.
- Log every AI-assisted task in `docs/05-ai/ai-usage.md` (one row: date, task, how AI was used, what was changed by hand). The log is the author's private record, git-ignored and not part of the submission.
- At the end of every working step, tick the checkbox and update the Status line in `docs/00-start-here/02-decisions-and-plan.md`.
- The author also keeps private notes that are git-ignored and not part of the submission: `docs/06-learning/` (learning journal, project journey) and `docs/07-guide/` (study guides). Keep maintaining them locally as before: a journey row and diagram node per step, a dated learning-notes entry per concept question, a numbered guide when the question belongs to the study series.

## Where things go

| What | Where |
|------|-------|
| Business requirements | `docs/01-business/brd.md` |
| Epics and stories | `docs/02-requirements/epics-and-stories.md` |
| Architecture docs | `docs/03-architecture/` (system-design, technical-architecture, application-architecture) |
| Decisions | `docs/03-architecture/adr/NNNN-short-title.md` |
| Engineering guides | `docs/04-engineering/` (foundation-checklist, coding-standards, testing-strategy, definition-of-done) |
| Deferred findings and ideas | `docs/04-engineering/backlog.md` (a review finding is applied in the same step or lands here with a reason; never silently dropped) |
| Author's private notes (git-ignored) | `docs/05-ai/ai-usage.md` AI usage log; `docs/06-learning/` learning notes and project journey; `docs/07-guide/` study guides |
| Backend (Gradle root) | `backend/` with modules `contracts`, `common`, `movie-service`, `people-service` |
| Frontend (SvelteKit) | `frontend/` |
| Local data and seed | `infra/seed/`, H2 files under `backend/data/` (git-ignored) |

Naming rules live in `docs/00-start-here/03-repo-layout.md`. Follow them.

## Stack (fixed by the brief and ADRs)

- Frontend: SvelteKit + TypeScript, npm, Vitest + Testing Library.
- Backend: Kotlin + Spring Boot, Gradle Kotlin DSL, JUnit 6 + MockK.
- Frontend to backend: GraphQL (served by movie-service).
- Service to service: gRPC (movie-service calls people-service).
- Database: H2 file database, one file per service, Liquibase migrations in XML. H2 in-memory for tests.

## Process

0. After every step, report to the author: what was done, how many files were created or changed, how to verify it, and what is next.
1. Work one story at a time, referenced by ID (for example `E2-S1`), normally through `/ship-story <ID>`.
2. Draft with a prompt or skill, the author reviews and edits by hand, then the `reviewer` agent checks it.
   Findings are applied or recorded in `docs/04-engineering/backlog.md` with a reason.
   A story is checked by the author in the browser against a walkthrough card (one line per acceptance criterion) before it is ticked.
3. Add a row to the author's private AI usage log.
4. Commit messages (once git is in use): Conventional Commits with the story ID, for example `feat(movie): E2-S1 create movie mutation`.

## AI tooling in this repo

Agents (`.claude/agents/`):
- `reviewer` - read-only second opinion on any document (BRD, stories, ADRs, architecture).
- `code-reviewer` - reviews a code change against its story and runs the tests.

Skills (`.claude/skills/`):
- `write-stories` - BRD in, `epics-and-stories.md` out, fixed table format.
- `write-adr` - one decision in, one short ADR out.
- `implement-story` - vertical-slice checklist for one story ID; copies the shape of reference story E1-S1 and builds screens from `ui-design.md`.
- `gen-tests` - edge-case checklist and conventions for Kotlin and Svelte tests, with E1-S1 tests as the reference shapes.
- `verify` - runs `make verify` (build, tests, live smoke check) and reports PASS/FAIL. Run after every code change.
- `ship-story` - the whole loop for one story ID: implement, code-review, apply chosen findings, verify, start the app and hand the author a walkthrough card, then close the logs. The default way to deliver a story from E2-S2 on.

Agents and skills are normally picked up live after a short delay. If a new one does not appear, restart Claude Code.
