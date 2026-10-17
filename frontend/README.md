# Movie Hub frontend

The web app: SvelteKit 3 + Svelte 5 (runes) + TypeScript, built and served by Vite. It talks GraphQL to movie-service on :8080 and uses one REST call for the poster upload. No UI library, no state library, no CSS framework: one global stylesheet and a handful of components.

## Run and test

Normally from the repo root: `make up` starts it with both services, `make test` runs its tests. On its own:

```
npm install
npm run dev      # http://localhost:5173, needs movie-service on :8080
npm test         # Vitest + Testing Library, 191 tests
npm run check    # svelte-check (TypeScript)
npm run lint     # Prettier + ESLint
```

Node 26 (`.nvmrc`), 24 also works. The backend address is `API_URL`, default `http://localhost:8080`; copy `.env.example` to `.env` to change it.

## Where things are

| Folder | What |
|--------|------|
| `src/routes/` | one folder per URL: `/` movies, `/movies/[id]`, `/movies/new`, `/movies/[id]/edit`, the same four for `/people`, and `/search`; `+layout.svelte` is the header, footer and toast |
| `src/lib/api/` | the only code that speaks HTTP: `client.ts` (one `fetch`, GraphQL errors become `ApiError` with a code), one module per entity, `upload.ts` for the multipart poster upload with progress |
| `src/lib/components/` | `MovieCard`, `MovieForm`, `PersonForm`, `CreditPicker`, `PosterUpload`, and the shared feedback pieces `Spinner`, `EmptyState`, `ErrorState`, `Toast`, `ConfirmDialog`, `Pagination` |
| `src/lib/stores/` | the toast store, the one piece of shared state |
| `src/lib/types/` | TypeScript mirrors of the GraphQL types |
| `src/app.css` | the global stylesheet; components add no colours of their own |
| `src/test-support/` | helpers for page tests |

Tests sit next to the file they test: `X.test.ts` runs in Node, `X.svelte.test.ts` renders in jsdom. The two Vitest projects are defined in `vite.config.ts`.

Every list page follows one shape: `loading`, `empty`, `error`, `ready`. Patterns and the reasons are in `docs/03-architecture/application-architecture.md` section 5, the screens in `docs/03-architecture/ui-design.md`, the stack rules in `docs/04-engineering/coding-standards.md`.
