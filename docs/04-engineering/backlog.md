# Backlog - deferred findings and enhancements

Things we chose not to do now. Each row says where it came from and why it waits, so the walkthrough can show the scope was held on purpose, not by accident. Review findings that are applied are not listed here; they are in the AI usage log.

Rule: a review finding is either applied in the same step or lands here with a reason. Nothing is silently dropped.

## Action items (parked by the author, to do before submission)

| Date parked | Item                                                       | What it involves                                                                                                                                                                                                                        | Trigger                |
| ----------- | ---------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| 2026-10-12  | Demo script and walkthrough outline for the interview      | One page: the screens in the order a reviewer would click, what to say at each, which file to open when asked "why". Draft from the project journey and the review catches in the AI log                                                | Author says "demo"     |
| 2026-10-12  | `git init` and first commit                                | Check `.gitignore` covers `backend/data/`, `node_modules`, build outputs; one initial commit with a Conventional Commits message; then one commit per story is not possible retroactively, so the AI log and journey carry that history | Author says "git init" |
| 2026-10-12  | Complete the local Docker walkthrough                      | In the browser, create a movie, person, credit and poster; run `make docker-reset`; confirm the seeded catalogue returns. The automated `make docker-verify` is green                                                                   | Before submission      |
| 2026-10-11  | Run the `reviewer` agent on this backlog before submission | Decide each deferred row: apply or keep with its reason                                                                                                                                                                                 | Before the demo script |

## Deferred review findings

| Date       | Source                       | Item                                                                                               | Why deferred                                                                                                                          | When                      |
| ---------- | ---------------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| 2026-10-09 | Foundation review, finding 5 | Poster properties, upload-size handler and the validation starter exist before their stories       | Harmless and needed soon; removing and re-adding is churn                                                                             | Used by E2-S3 and E3-S1   |
| 2026-10-11 | UI design review, finding 9  | `ui-design.md` runs over the two-page default                                                      | Section 1 (brief to screens) and the wireframes are the doc's value; trimming them would cost more than it saves                      | Revisit before submission |
| 2026-10-11 | E3-S1 review, finding 5      | `deleteMovie` in the GraphQL controller sequences two service calls (row delete, then file delete) | Accepted: the file must go after the commit, and a second service to hold that one line would be ceremony; the why-comment carries it | Keep                      |

## Enhancement ideas (not in the brief)

| Date       | Source                 | Idea                                                                                                   | Why not now                                                                                     |
| ---------- | ---------------------- | ------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------- |
| 2026-10-11 | Reference images       | Optional character name on an ACTOR credit (the Grand Budapest poster shows cast with character names) | Not in the brief; the cheapest candidate for "anything else" (F8) if time remains after E6      |
| 2026-10-11 | Reference images       | Genre, runtime, ratings, "known for" on people                                                         | Not in the brief; each adds a field, a story and tests for nothing asked                        |
| 2026-10-09 | Plan, Phase 1 optional | GitHub Actions workflow running all tests                                                              | Git is deferred by the author; add once the repo exists                                         |
| 2026-10-11 | Testing strategy       | Automated browser tests (Playwright)                                                                   | The brief asks for unit tests with edge cases; a demo script covers the end-to-end walk by hand |

## Process ideas for a next version

| Date       | Idea                                                               | Note                                                          |
| ---------- | ------------------------------------------------------------------ | ------------------------------------------------------------- |
| 2026-10-11 | A live create-then-delete smoke line per entity once delete exists | Proves the write path end to end without polluting local data |
