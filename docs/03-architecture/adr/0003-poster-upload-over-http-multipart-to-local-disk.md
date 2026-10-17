# ADR 0003 - Poster upload over HTTP multipart, files on local disk

| | |
|---|---|
| Status | Accepted |
| Relates to | BRD F3, N2; stories E3-S1, E3-S2, E3-S3 |

## Context

Users upload a JPG or PNG poster of up to 10 MB and must see a progress percentage (E3-S1). The brief says frontend to backend communication is GraphQL, but GraphQL has no standard for binary upload, and browser progress events come from the plain HTTP upload, not from the query layer. We also need somewhere to store the files with zero infrastructure, the same constraint that drove ADR 0004.

## Options considered

| Option | Pros | Cons |
|--------|------|------|
| A - One plain HTTP multipart endpoint `POST /movies/{id}/poster` on movie-service; files stored in a local folder and served as static URLs (chosen) | Browser `XMLHttpRequest` gives real progress events for free. Standard Spring multipart handling. Simple to explain | One endpoint sits outside the GraphQL schema and must be documented separately |
| B - GraphQL multipart request specification | Everything stays in GraphQL | Needs an extra library on both sides, is not part of the GraphQL spec, and progress still has to come from the underlying HTTP upload |
| C - Base64 the image inside a GraphQL mutation | No extra endpoint | Payload grows by a third, no progress, 10 MB strings through the query parser. Poor fit |
| D - Store files in the database as BLOBs | One storage | Bloats the H2 file, slower to serve, harder to inspect. Local disk is simpler and is the common pattern before object storage |

## Decision

Poster upload is the one non-GraphQL call from the frontend: a multipart POST handled by `PosterController`, validated for type and size, written to `backend/data/posters/` and served under `/posters/{file}`. The GraphQL `Movie.posterUrl` field points at it. We do not use GraphQL multipart or database BLOBs.

## Consequences

- E3-S1's percentage progress is straightforward: `upload.ts` listens to XHR progress events.
- Validation happens twice, in the browser before upload and on the server, so E3-S2 is satisfied without a round trip.
- The endpoint returns RFC 9457 problem details on error, using the same error codes as GraphQL (ADR 0007).
- Upgrade path: swap the folder for object storage behind `PosterService`; the endpoint and URL shape stay.
- The stored URL carries a version query (`?v=<millis>`) because the file name never changes on replace; without it the browser shows the cached old poster.
