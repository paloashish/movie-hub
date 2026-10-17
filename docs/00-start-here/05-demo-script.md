# Demo script - the interview walkthrough

About 20 minutes. Two parts: the app for five minutes, then the process and the code for the rest, which is what the brief says they are judging ("software engineers, not prompt engineers"). Have `make up` running before the call and GraphiQL open in a second tab.

## Part 1 - The app, five minutes

Click in this order; each line is one screen and one sentence to say.

| # | Screen | Click | Say |
|---|--------|-------|-----|
| 1 | Movies grid `/` | nothing yet | "Two services behind this page: movies here, people over gRPC. Seeded on first run; `make reset` empties it." |
| 2 | Sort | pick "Newest first" | "Sort and paging are server-side; the grid asks for one page of twelve." |
| 3 | Movie page | open Inception | "Cast and Creators come from the other service. One GraphQL query, one gRPC batch call for the names." |
| 4 | Poster upload | pick a JPG from `docs/00-start-here/brief/reference/` | "Real progress: it is the one non-GraphQL call, multipart over XMLHttpRequest, because only XHR reports upload progress." |
| 5 | Add a credit | type "nol", pick Nolan, add as Writer again | "Already credited: the rule is a unique constraint and a check, and the error carries a code the UI switches on." |
| 6 | Remove a credit | Remove, then Cancel, then Remove and confirm | "Every delete confirms first and the dialog stays busy while the call runs." |
| 7 | Person page | open Christopher Nolan | "Their movies newest first. Delete here removes the credits in movie-service first, then the person over gRPC, in one transaction." |
| 8 | Search | type "alien" in the header | "One box, two groups, case-insensitive partial match on both sides, no search engine." |
| 9 | Phone width | narrow the window | "Nav and search wrap, posters two columns, nothing scrolls sideways. A script checks every page at three widths." |
| 10 | Error state | `lsof -ti :8081 \| xargs kill`, reload a movie page | "people-service is down: the page shows the error with Retry, because the gRPC client maps the status to one of five error codes. Bring it back with `make up`." |

## Part 2 - The process and the code, fifteen minutes

Open files as they come up; every one is short.

1. **The plan and its status.** `docs/00-start-here/02-decisions-and-plan.md`. "Brief to business requirements to stories to architecture to foundation to stories as vertical slices. The status line is kept current after every step."
2. **One story end to end.** `docs/02-requirements/epics-and-stories.md`, row E4-S1. Then the slice: `0002-E4-S1-create-credit.xml`, `Credit.kt`, `CreditService.kt`, `CreditController.kt`, `schema.graphqls`, `CreditPicker.svelte`. "Changeset, entity, service, GraphQL edge, UI. Same shape for all 21 stories; E1-S1 was the reference."
3. **Where the rules live.** `CreditService.add`: "Both sides must exist, no duplicate role. Services throw one exception hierarchy; the edges map it to a code."
4. **The error codes, both sides.** `GraphQlExceptionResolver.kt`, `GrpcStatusMapper.kt`, `frontend/src/lib/api/client.ts`. "Five codes, the same vocabulary over GraphQL, REST and gRPC. The frontend never sees a stack trace."
5. **The gRPC boundary.** `PeopleClient.kt`. "The only class that sees gRPC types. Five-second deadline, status mapped to our exceptions. The reviewer caught the missing deadline on the first story."
6. **The cross-service rule proven.** `CreditServiceRollbackTest.kt`. "Deleting a person removes credits first, then calls the other service, in one transaction. This test runs it on a real database and checks the credits survive a failed call."
7. **How AI was used.** `docs/05-ai/ai-tooling-map.md` and `.claude/skills/ship-story/SKILL.md`. "One loop per story: implement, an agent reviews against the definition of done, I choose which findings to apply, verify again, then I check the screen against a card. Findings I did not apply are in the backlog with a reason."
8. **What the loop caught.** The README's "How AI tools were used" paragraph and `docs/04-engineering/backlog.md`. Pick three: the gRPC deadline, write-then-delete for posters, the retry that reloaded the wrong page.
9. **The verifier.** Run `make verify` live if time allows (90 seconds). "50 checks: build, every test, both services up, one smoke line per story."

## Likely questions and where the answer is

| Question | Answer in one line | File |
|----------|-------------------|------|
| Why two services split this way? | Two business nouns, movie-service hosts GraphQL so the frontend has one door | ADR 0001, 0002 |
| Why H2 and not Postgres? | Zero setup for a reviewer; Liquibase XML is database-agnostic, Postgres is a driver and config change | ADR 0004 |
| Why no GraphQL for the upload? | Multipart over XHR gives the progress percentage the brief asks for | ADR 0003 |
| Why blocking gRPC stubs from Kotlin? | Fewer moving parts than coroutine stubs; the brief asks for Kotlin services, not Kotlin stubs | ADR 0006 |
| How do credits reference people across databases? | By value, no foreign key; one batch gRPC call resolves names | `CreditService.listForMovie` |
| What happens if people-service dies mid-delete? | The transaction rolls the credit delete back; proven by a test | `CreditServiceRollbackTest.kt` |
| Where is a line you changed by hand? | Any file; the comments say why, and the backlog shows what was declined and why | `docs/04-engineering/backlog.md` |
| What would you do with more time? | The backlog, with reasons for each row | `docs/04-engineering/backlog.md` |

## Before the call

- `make reset && make up`, open http://localhost:5173 and http://localhost:8080/graphiql.
- Have `docs/00-start-here/brief/reference/poster-wall.jpeg` ready for the upload.
- Keep this page open on a second screen.
