---
name: write-adr
description: Write an Architecture Decision Record for Movie Hub in docs/03-architecture/adr/ using the fixed short template. Use when a technical decision is made or asked to be recorded.
---

# write-adr

## Input
- The decision to record and the options that were considered.
- Existing ADRs in `docs/03-architecture/adr/` (to pick the next number and avoid contradiction).
- The decisions table in `docs/00-start-here/02-decisions-and-plan.md` for the already-agreed ones.

## Output
`docs/03-architecture/adr/NNNN-short-kebab-title.md` (four-digit number, next in sequence):

```
# ADR NNNN - <Title>

| | |
|---|---|
| Status | Accepted |
| Date | YYYY-MM-DD |
| Relates to | <story IDs or BRD IDs> |

## Context
<Two to four sentences: the problem and the constraints from the brief.>

## Options considered
| Option | Pros | Cons |
|--------|------|------|
| A - <chosen> | | |
| B - | | |

## Decision
<One or two sentences. Say what we do, and what we deliberately do not do.>

## Consequences
- <What becomes easier>
- <What becomes harder or is deferred, and the upgrade path if any>
```

## Rules
1. One decision per ADR. Under one page.
2. Context must cite the brief or a BRD/story ID.
3. Always include at least one rejected option with a real reason.
4. Plain words; a beginner must be able to follow it.
5. Add the ADR to the lists in `docs/03-architecture/README.md` and `system-design.md` section 4.
6. Never edit or renumber an accepted ADR. A changed decision is a new ADR whose Status says `Supersedes NNNN`; the old one gets `Superseded by NNNN` and keeps its text, so the history of the decision stays readable.
7. Append a row to the author's private AI usage log (`docs/05-ai/ai-usage.md`, git-ignored).
