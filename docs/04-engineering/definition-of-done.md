# Definition of Done

A story is done when every line below is true. The `implement-story` skill covers them through its steps; the `code-reviewer` agent checks them before APPROVE.

## Behaviour

- [ ] Every acceptance criterion of the story (`docs/02-requirements/epics-and-stories.md`) is implemented and visible in the UI where it describes feedback.
- [ ] Nothing beyond the story was added: no extra fields, endpoints, abstractions or libraries.

## Code

- [ ] Follows `coding-standards.md`: constructor injection, no logic in controllers, services throw `AppException` subtypes only, schema-first GraphQL, four-state pages, labels on inputs, alt text on images.
- [ ] Names, packages and paths follow `docs/00-start-here/03-repo-layout.md`.
- [ ] Database change is a Liquibase XML changeset named `NNNN-<story>-<desc>.xml` and included from the master; applied changesets were not edited.
- [ ] Errors reach the user through the shared mappers; no new error shapes.
- [ ] The author can explain every line in a walkthrough. Any line that cannot be explained is rewritten or removed.

## Tests

- [ ] Unit tests for every service rule and every error path; component tests for the edge and the UI states (see `testing-strategy.md`).
- [ ] Edge cases from the `gen-tests` checklist that apply are covered.
- [ ] `make verify` prints `VERIFY: ALL PASSED`, including a smoke line for any new endpoint or page.

## Process

- [ ] Author reviewed and edited the AI draft by hand.
- [ ] `code-reviewer` agent returned APPROVE, or its findings were applied or consciously declined with a reason.
- [ ] The author can say what AI drafted and what was changed by hand.
- [ ] Docs touched by the story are updated: the story row marked done, `foundation-checklist.md` or architecture docs if the story changed a pattern.
- [ ] Once git is in use: one commit per story, Conventional Commits with the story ID.

## Not required

Pixel-perfect styling, performance tuning, accessibility beyond BRD N5, browser testing beyond the author's own, documentation beyond the rows above.
