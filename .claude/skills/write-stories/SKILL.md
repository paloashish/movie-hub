---
name: write-stories
description: Write or extend Movie Hub epics and user stories from the BRD into docs/02-requirements/epics-and-stories.md in the fixed table format. Use when asked to write, add or update epics or stories.
---

# write-stories

## Input
- `docs/01-business/brd.md` (features F1-F8, goals G1-G4, non-functional N1-N5).
- The epic list in `docs/00-start-here/02-decisions-and-plan.md`.
- If extending: the existing `docs/02-requirements/epics-and-stories.md`.

## Output
`docs/02-requirements/epics-and-stories.md`, one section per epic:

```
## E<n> - <Epic name>
<One sentence scope.> Covers: F<x>, F<y>.

| ID | Priority | Story | Acceptance criteria |
|----|----------|-------|---------------------|
| E<n>-S<m> | Must / Should / Could | As a <persona>, I want <action>, so that <benefit>. | - Given/When/Then line<br>- Given/When/Then line |
```

## Rules
1. Personas are only those in the BRD: catalogue manager, visitor.
2. Every epic lists the BRD feature IDs it covers; every BRD feature F1-F8 appears in at least one epic.
3. Two or three acceptance criteria per story, each testable, written as Given/When/Then.
4. Feedback requirements from BRD N2 (loading, saving, progress, success, error, confirm before delete) appear as acceptance criteria on the stories they affect, not as separate stories.
5. No technical detail (no GraphQL, gRPC, table names). Stories describe user-visible behaviour only.
6. Stretch features (BRD F8) are priority Could. Everything in F1-F7 is Must or Should.
7. Keep the whole file under two pages. One line per story.
8. When the file already exists, existing rows are never changed: IDs, wording and ticks stay as they are, because ADRs, changeset names, tests and commits cite the IDs. Only append new rows or epics for BRD items not yet covered, and end by listing what was added. If everything is covered, say so and change nothing.
9. Finish by appending a row to the author's private AI usage log (`docs/05-ai/ai-usage.md`, git-ignored).

## After writing
Ask the author to review and edit, then run the `reviewer` agent on the file.
