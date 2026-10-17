# 00 - Start Here

This folder is the entry point for anyone new to the Movie Hub project. Read it top to bottom; it takes about ten minutes.

| Order | Document | What you get |
|-------|----------|--------------|
| 1 | [01-assessment-summary.md](01-assessment-summary.md) | What we were asked to build and how it will be judged |
| 2 | [02-decisions-and-plan.md](02-decisions-and-plan.md) | The key technical decisions and the phased delivery plan |
| 3 | [03-repo-layout.md](03-repo-layout.md) | How the repository is organised and why things are named the way they are |
| 4 | [04-research-notes.md](04-research-notes.md) | Cleaned early notes showing how the first design choices were reached |
| 5 | [05-demo-script.md](05-demo-script.md) | The interview walkthrough: the app in ten clicks, then the process and code, with likely questions |
| - | [brief/Technical-Assessment.pdf](brief/Technical-Assessment.pdf) | The original assessment brief |
| - | [brief/reference/](brief/reference/) | Five reference images used to sanity-check scope and UI design (see research notes section 6) |

## How this project is run

Movie Hub is a technical assessment, but it is deliberately built the way a small real product would be:

1. **Business requirements first** - a short BRD says what the product is for.
2. **Epics and user stories** - the BRD is broken into work items with acceptance criteria.
3. **Architecture before code** - system design, technical and application architecture, and Architecture Decision Records (ADRs) explain the shape of the solution.
4. **Foundation, then features** - the monorepo, services, database and frontend skeleton come first, then features are delivered story by story.
5. **AI as a productivity aid** - Claude Code is used with repo-local skills and agents, and every use is logged so each line of code can be explained in a walkthrough.

The architecture is intentionally small. The goal is to show a sound software development lifecycle (SDLC), not a large distributed system.

## Where everything lives

```
docs/
├── 00-start-here/      <- you are here
├── 01-business/        BRD
├── 02-requirements/    epics and user stories
├── 03-architecture/    system design, tech arch, app arch, ADRs
├── 04-engineering/     coding standards, testing strategy, definition of done, foundation checklist, backlog
└── 05-ai/              how AI tools were used: the tooling map
```

The original brief is kept unchanged at `docs/00-start-here/brief/Technical-Assessment.pdf`.
