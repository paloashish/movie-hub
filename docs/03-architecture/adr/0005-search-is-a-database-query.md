# ADR 0005 - Search is a database query

| | |
|---|---|
| Status | Accepted |
| Relates to | BRD F6; story E5-S1 |

## Context

E5-S1 asks for one search box that finds movies by title and people by name, case-insensitive and partial, with a loading state and a "no results" message. The BRD assumes hundreds of records, not millions. Movies and people live in two databases (ADR 0001, ADR 0004).

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A - Case-insensitive `LIKE '%text%'` in each service's own database; movie-service runs the movie query and calls `SearchPeople` over gRPC, then returns both lists (chosen) | No new component. One Spring Data method per entity. Easy to test with edge cases (empty, special characters, no match) | Full scan on each search; no ranking or fuzzy matching. Fine at hundreds of rows, not at millions |
| B - A search engine (Elasticsearch, OpenSearch, Meilisearch) | Ranking, typo tolerance, scale | A server to run and index to keep in sync. Far beyond the brief |
| C - Database full-text search | Better than LIKE for large text | H2's full-text support is a separate module and differs from PostgreSQL's; adds engine-specific setup for no visible gain at this size |

## Decision

Search is `findByTitleContainingIgnoreCase` and `findByNameContainingIgnoreCase`. movie-service composes the two results into one GraphQL `SearchResult`. We do not add a search engine or full-text indexing.

## Consequences

- E5-S1 is a thin story: one repository method per service, one gRPC method, one resolver, one page.
- Results are unranked; the UI shows them in name or title order.
- Upgrade path: an index behind the same `search` query if data ever grows; the GraphQL contract does not change.
