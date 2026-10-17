# .claude - the AI tooling for Movie Hub

Two agents under `agents/` (`reviewer` for documents, `code-reviewer` for code, both read-only) and six skills under `skills/` (`write-stories`, `write-adr`, `implement-story`, `gen-tests`, `verify`, `ship-story`). Every file is plain Markdown; the root `CLAUDE.md` holds the conventions every run reads first.

Which one runs at which step, what each reads and what it produces: `docs/05-ai/ai-tooling-map.md`.
