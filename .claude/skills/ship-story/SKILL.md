---
name: ship-story
description: Deliver one Movie Hub story end to end in one go - implement, code-review, fix, verify, then start the app and hand the author a walkthrough card to check the screen. Use when asked to ship, deliver or "do the whole loop for" a story ID such as E2-S2.
---

# ship-story

The story loop as one command. It chains the existing pieces; it does not replace them. The author stops the loop at two points only: choosing which review findings to apply, and checking the screen.

## Input
- The story ID (for example `E2-S2`).

## Steps

1. **Implement** - follow the `implement-story` skill for the ID, all of its steps including its own `make verify`. Report the files created and changed, the test counts and the verify result, in the usual end-of-step format. Stop here if verify is red.

2. **Review** - launch the `code-reviewer` agent on the story (same prompt shape as before: story row, acceptance criteria, standards, the list of files changed, "run make verify, return verdict, criteria met, numbered findings with severity"). Wait for it.

3. **Fix** - show the findings to the author with a one-line recommendation each (apply / defer with reason / disagree with reason). Wait for the author's answer: `apply`, `apply 1,3` or `skip`. Apply what was chosen. Every finding not applied goes into `docs/04-engineering/backlog.md` with its reason. Never apply silently.

4. **Verify** - run `make verify` again. Stop here if red.

5. **Walkthrough** - `make down` if anything is running, then `make up` in the background, wait for health on :8080 and :8081 and the dev server on :5173, then give the author a **walkthrough card**:

   ```
   Walkthrough E2-S2 - open http://localhost:5173
   1. <criterion 1 as an action>   -> you should see: <what the story promises>
   2. <criterion 2 ...>            -> you should see: ...
   3. To see the error state: <how to break it, e.g. stop people-service>  -> you should see: ...
   Say "ok" when every line matches, or "step N: <what you saw>".
   ```
   One numbered line per acceptance criterion, in the story's words, each with the URL or click and the expected feedback (loading, saving, success, error, confirm). Include the "not found" and error states when the story has them.

6. **Close** - on "ok": `make down`, tick the story in `docs/02-requirements/epics-and-stories.md`, add the rows to `docs/05-ai/ai-usage.md` (story, review), update `docs/00-start-here/02-decisions-and-plan.md` status and `docs/06-learning/project-journey.md` (node, table row, next step), and name the next story. On "step N: ...": treat it as a finding, fix, and go back to step 4.

## Rules
- Each step reports in the step-0 format from CLAUDE.md (what was done, how many files, how to verify, what is next) before the next one starts, so the author can follow along.
- No step is skipped because the previous one "looked fine". Verify runs twice on purpose: once after implementing, once after the fixes.
- The walkthrough is done by the author in a real browser. Do not replace it with an automated check; the point is that a person sees the screen.
- Everything here reuses `implement-story`, `verify`, `code-reviewer` and the backlog rule. If one of those changes, this skill inherits it.
