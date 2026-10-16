---
name: verify
description: Prove nothing is broken after a code change or story: runs make verify (build, all tests, live smoke check of both services) and reports PASS/FAIL per check. Use after implementing a story, after any backend or frontend change, or when asked "does it still work".
---

# verify

## What it does
Runs `make verify` from the repo root (`scripts/verify.sh`), which:
1. Builds the backend and runs every backend test.
2. Runs the frontend type check (svelte-check), lint (prettier + eslint) and tests.
3. Starts both services and the frontend dev server, then checks: Actuator health on :8080 and :8081, the GraphQL `ping` query, gRPC listening on :9090, the request ID echoed in the response header, the CORS preflight for the dev origin, Swagger UI, GraphiQL, and the dev server serving the layout. Then stops everything.

## Steps
1. Make sure nothing is already running on 8080/8081/9090 (`make down` if needed).
2. Run `make verify` and wait for `VERIFY: ALL PASSED` or `VERIFY: FAILED`.
3. Report every line that starts with PASS, FAIL or SKIP, plus the test count.
4. On FAIL: open the named log, quote the first real error (not the stack), and name the file most likely at fault. Do not mark the story done.
5. Append a row to `docs/05-ai/ai-usage.md` only when verify was run as part of a story; a bare check needs no row.

## Rules
- Never report success without the literal `VERIFY: ALL PASSED` line.
- Never "fix" the verifier to make it pass; fix the code or raise the problem.
- When a story adds a new endpoint or page, add a smoke line for it to `scripts/verify.sh` in the same change.
