#!/usr/bin/env bash
# One command that proves nothing is broken: build + tests + a live smoke check of both services.
# Used by `make verify`, the `verify` skill, the code-reviewer agent and (later) CI.
# Exit code 0 = everything passed. Each check prints PASS or FAIL on its own line.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="${TMPDIR:-/tmp}/moviehub-verify"; mkdir -p "$LOG_DIR"
FAILED=0
pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; FAILED=1; }

# 1. Backend build + tests
if (cd "$ROOT/backend" && ./gradlew build --console=plain -q > "$LOG_DIR/backend.log" 2>&1); then
  pass "backend build and tests ($(grep -rho 'tests="[0-9]*"' "$ROOT"/backend/*/build/test-results 2>/dev/null | awk -F'"' '{s+=$2} END {print s+0}') tests)"
else
  fail "backend build and tests (see $LOG_DIR/backend.log)"
fi

# 2. Frontend tests (skipped until frontend/ exists)
if [ -f "$ROOT/frontend/package.json" ]; then
  if (cd "$ROOT/frontend" && npm run check --silent > "$LOG_DIR/frontend-check.log" 2>&1); then
    pass "frontend type check (svelte-check)"
  else
    fail "frontend type check (see $LOG_DIR/frontend-check.log)"
  fi
  if (cd "$ROOT/frontend" && npm run lint --silent > "$LOG_DIR/frontend-lint.log" 2>&1); then
    pass "frontend lint (prettier + eslint)"
  else
    fail "frontend lint (see $LOG_DIR/frontend-lint.log)"
  fi
  if (cd "$ROOT/frontend" && npm test --silent > "$LOG_DIR/frontend.log" 2>&1); then
    pass "frontend tests ($(grep -oE 'Tests +[0-9]+ passed' "$LOG_DIR/frontend.log" | grep -oE '[0-9]+' | head -1) tests)"
  else
    fail "frontend tests (see $LOG_DIR/frontend.log)"
  fi
else
  echo "SKIP  frontend checks (frontend/ not scaffolded yet)"
fi

# 3. Smoke: start both services, hit health + GraphQL ping, stop
cleanup() { pkill -f "people-service:bootRun|movie-service:bootRun|PeopleServiceApplication|MovieServiceApplication|vite dev" 2>/dev/null || true; }
trap cleanup EXIT
if lsof -iTCP:8080 -sTCP:LISTEN -n -P >/dev/null 2>&1 || lsof -iTCP:8081 -sTCP:LISTEN -n -P >/dev/null 2>&1; then
  fail "smoke: ports 8080/8081 already in use; run 'make down' first"
else
  (cd "$ROOT/backend" && ./gradlew :people-service:bootRun -q > "$LOG_DIR/people.log" 2>&1 &)
  (cd "$ROOT/backend" && ./gradlew :movie-service:bootRun -q > "$LOG_DIR/movie.log" 2>&1 &)
  for _ in $(seq 1 60); do
    curl -sf localhost:8080/actuator/health >/dev/null 2>&1 && curl -sf localhost:8081/actuator/health >/dev/null 2>&1 && break
    sleep 2
  done
  curl -sf localhost:8080/actuator/health | grep -q '"UP"' && pass "movie-service health UP"   || fail "movie-service health"
  curl -sf localhost:8081/actuator/health | grep -q '"UP"' && pass "people-service health UP"  || fail "people-service health"
  curl -sf -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"{ ping }"}' | grep -q '"pong"' \
    && pass "GraphQL ping" || fail "GraphQL ping"
  lsof -iTCP:9090 -sTCP:LISTEN -n -P >/dev/null 2>&1 && pass "gRPC server listening on 9090" || fail "gRPC server on 9090"
  curl -s -D - -o /dev/null -X POST localhost:8080/graphql -H 'Content-Type: application/json' -H 'X-Request-Id: verify-1' -d '{"query":"{ ping }"}' | grep -qi "X-Request-Id: verify-1" \
    && pass "request id echoed in response header" || fail "request id header"
  # E1-S1: people query crosses into people-service over gRPC; the same request id must appear in both logs
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -H 'X-Request-Id: verify-e1s1' -d '{"query":"{ people { id name } }"}' | grep -q '"people":\[' \
    && pass "E1-S1 GraphQL people query" || fail "E1-S1 GraphQL people query"
  sleep 1
  grep -q '"requestId":"verify-e1s1"' "$LOG_DIR/people.log" && grep -q '"requestId":"verify-e1s1"' "$LOG_DIR/movie.log" \
    && pass "request id carried over gRPC into people-service log" || fail "request id across gRPC (check $LOG_DIR/people.log)"
  # E1-S2: createPerson is wired and validated; a blank name must answer VALIDATION (and writes nothing).
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"mutation { createPerson(input: { name: \"  \" }) { id } }"}' | grep -q '"code":"VALIDATION"' \
    && pass "E1-S2 createPerson rejects a blank name with VALIDATION" || fail "E1-S2 createPerson validation"
  # E1-S3: updatePerson reaches the people database over gRPC; an unknown id must answer NOT_FOUND (writes nothing)
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"mutation { updatePerson(id: \"999999\", input: { name: \"x\" }) { id } }"}' | grep -q '"code":"NOT_FOUND"' \
    && pass "E1-S3 updatePerson answers NOT_FOUND for an unknown id" || fail "E1-S3 updatePerson not-found path"
  # E2-S1 + E2-S6: the movies query answers one page with its total
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"{ movies(page: 0, size: 2) { items { id title } page size total } }"}' | grep -q '"page":0,"size":2,"total":' \
    && pass "E2-S1/E2-S6 GraphQL movies page query" || fail "E2-S1/E2-S6 GraphQL movies page query"
  # E6-S2: the sample catalogue's posters ship with movie-service and are served under /posters/
  curl -s -D - -o /dev/null localhost:8080/posters/seed-100001.svg | grep -qi "content-type: image/svg+xml" \
    && pass "E6-S2 seed posters are served from the classpath as image/svg+xml" || fail "E6-S2 seed poster not served"
  # E6-S1: the sort argument is accepted; an unknown one is rejected by the schema
  YEARS=$(curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"{ movies(size: 2, sort: YEAR_DESC) { items { releaseYear } } }"}' | grep -o '"releaseYear":[0-9]*' | grep -o '[0-9]*$' | tr '\n' ' ')
  set -- $YEARS; [ -n "$1" ] && { [ -z "$2" ] || [ "$1" -ge "$2" ]; } \
    && pass "E6-S1 movies come newest first when asked (years: $YEARS)" || fail "E6-S1 movies sort order (years: $YEARS)"
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"{ movies(sort: RATING) { page } }"}' | grep -q '"errors"' \
    && pass "E6-S1 an unknown sort is rejected" || fail "E6-S1 unknown sort accepted"
  # E2-S2: the movie query reaches the database; an unknown id must answer NOT_FOUND
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"{ movie(id: \"999999\") { id } }"}' | grep -q '"code":"NOT_FOUND"' \
    && pass "E2-S2 movie query answers NOT_FOUND for an unknown id" || fail "E2-S2 movie not-found path"
  # E2-S3: createMovie is wired and validated; a year before 1888 must answer VALIDATION (and writes nothing).
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"mutation { createMovie(input: { title: \"x\", releaseYear: 1887 }) { id } }"}' | grep -q '"code":"VALIDATION"' \
    && pass "E2-S3 createMovie rejects year 1887 with VALIDATION" || fail "E2-S3 createMovie validation"
  # E2-S4: updateMovie reaches the database; an unknown id must answer NOT_FOUND (writes nothing)
  curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"mutation { updateMovie(id: \"999999\", input: { title: \"x\", releaseYear: 2000 }) { id } }"}' | grep -q '"code":"NOT_FOUND"' \
    && pass "E2-S4 updateMovie answers NOT_FOUND for an unknown id" || fail "E2-S4 updateMovie not-found path"
  # E2-S3 + E2-S5: the live write path for movies. Create a throwaway movie, delete it, and prove it is gone.
  MOVIE_ID=$(curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"mutation { createMovie(input: { title: \"verify smoke\", releaseYear: 2000 }) { id } }"}' | sed -n 's/.*"id":"\([0-9]*\)".*/\1/p')
  [ -n "$MOVIE_ID" ] && pass "E2-S3 createMovie writes a movie (id $MOVIE_ID)" || fail "E2-S3 createMovie live create"
  # E3-S1: upload a 1x1 PNG to the throwaway movie, fetch it back as a static file
  SMOKE_PNG="$LOG_DIR/smoke.png"
  printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82' > "$SMOKE_PNG"
  [ -n "$MOVIE_ID" ] && curl -s -X POST "localhost:8080/movies/$MOVIE_ID/poster" -F "file=@$SMOKE_PNG;type=image/png" | grep -q "\"posterUrl\":\"/posters/$MOVIE_ID.png?v=" \
    && pass "E3-S1 poster upload answers the poster url" || fail "E3-S1 poster upload"
  [ -n "$MOVIE_ID" ] && [ "$(curl -s -o /dev/null -w '%{http_code}' "localhost:8080/posters/$MOVIE_ID.png")" = "200" ] \
    && pass "E3-S1 uploaded poster is served as a static file" || fail "E3-S1 static poster"
  [ -n "$MOVIE_ID" ] && curl -s -X POST "localhost:8080/movies/$MOVIE_ID/poster" -F "file=@$SMOKE_PNG;type=image/gif" | grep -q '"code":"VALIDATION"' \
    && pass "E3-S1 poster upload rejects a GIF with VALIDATION" || fail "E3-S1 poster type validation"
  # E3-S3: replace it as a JPEG; the old PNG goes, the JPG is served, the url carries a new version
  [ -n "$MOVIE_ID" ] && curl -s -X POST "localhost:8080/movies/$MOVIE_ID/poster" -F "file=@$SMOKE_PNG;type=image/jpeg" | grep -q "\"posterUrl\":\"/posters/$MOVIE_ID.jpg?v=" \
    && pass "E3-S3 replacing the poster answers the new url" || fail "E3-S3 poster replace"
  [ -n "$MOVIE_ID" ] && [ ! -f "$ROOT/backend/data/posters/$MOVIE_ID.png" ] && [ "$(curl -s -o /dev/null -w '%{http_code}' "localhost:8080/posters/$MOVIE_ID.jpg")" = "200" ] \
    && pass "E3-S3 old file removed, new file served" || fail "E3-S3 old poster file left behind or new one missing"
  [ -n "$MOVIE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"{ movie(id: \\\"$MOVIE_ID\\\") { posterUrl } }\"}" | grep -q "/posters/$MOVIE_ID.jpg?v=" \
    && pass "E3-S3 the grid query returns the versioned poster url" || fail "E3-S3 stored poster url"
  # E4-S1: a credit links the throwaway movie to a throwaway person across both services
  CREDIT_PERSON=$(curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"mutation { createPerson(input: { name: \"verify credit\" }) { id } }"}' | sed -n 's/.*"id":"\([0-9]*\)".*/\1/p')
  [ -n "$MOVIE_ID" ] && [ -n "$CREDIT_PERSON" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { addCredit(movieId: \\\"$MOVIE_ID\\\", personId: \\\"$CREDIT_PERSON\\\", role: ACTOR) { id person { name } } }\"}" | grep -q '"name":"verify credit"' \
    && pass "E4-S1 addCredit links movie and person over gRPC" || fail "E4-S1 addCredit"
  [ -n "$MOVIE_ID" ] && [ -n "$CREDIT_PERSON" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { addCredit(movieId: \\\"$MOVIE_ID\\\", personId: \\\"$CREDIT_PERSON\\\", role: ACTOR) { id } }\"}" | grep -q '"code":"CONFLICT"' \
    && pass "E4-S1 the same credit twice answers CONFLICT" || fail "E4-S1 duplicate credit"
  [ -n "$MOVIE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"{ movie(id: \\\"$MOVIE_ID\\\") { credits { role person { name } } } }\"}" | grep -q '"role":"ACTOR","person":{"name":"verify credit"}' \
    && pass "E4-S1 movie query resolves the credit's person from people-service" || fail "E4-S1 credits on the movie query"
  # E5-S1: one search finds the throwaway movie by title and the throwaway person by name, case-insensitive
  SEARCH=$(curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"{ search(text: \"VERIFY\") { movies { title } people { name } } }"}')
  echo "$SEARCH" | grep -q '"title":"verify smoke"' \
    && pass "E5-S1 search finds the movie by partial title, ignoring case" || fail "E5-S1 search movies"
  echo "$SEARCH" | grep -q '"name":"verify credit"' \
    && pass "E5-S1 search finds the person by name over gRPC" || fail "E5-S1 search people"
  # E4-S3: the person query lists the movie, newest first
  [ -n "$CREDIT_PERSON" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"{ person(id: \\\"$CREDIT_PERSON\\\") { credits { role movie { title } } } }\"}" | grep -q '"role":"ACTOR","movie":{"title":"verify smoke"}' \
    && pass "E4-S3 person query lists their movies with the role" || fail "E4-S3 credits on the person query"
  # E4-S2: remove the credit; the person is untouched; a second remove answers NOT_FOUND
  CREDIT_ID=$([ -n "$MOVIE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"{ movie(id: \\\"$MOVIE_ID\\\") { credits { id } } }\"}" | sed -n 's/.*"credits":\[{"id":"\([0-9]*\)".*/\1/p')
  [ -n "$CREDIT_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { removeCredit(id: \\\"$CREDIT_ID\\\") }\"}" | grep -q '"removeCredit":true' \
    && pass "E4-S2 removeCredit removes the link" || fail "E4-S2 removeCredit"
  [ -n "$CREDIT_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { removeCredit(id: \\\"$CREDIT_ID\\\") }\"}" | grep -q '"code":"NOT_FOUND"' \
    && pass "E4-S2 removing it again answers NOT_FOUND" || fail "E4-S2 second remove"
  [ -n "$CREDIT_PERSON" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"{ person(id: \\\"$CREDIT_PERSON\\\") { name } }\"}" | grep -q '"name":"verify credit"' \
    && pass "E4-S2 the person still exists after the credit is removed" || fail "E4-S2 person vanished with the credit"
  # E4-S3 (system-design 3.3): credit the person again, delete the person, and the credit must be gone too
  [ -n "$MOVIE_ID" ] && [ -n "$CREDIT_PERSON" ] && curl -s -o /dev/null -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { addCredit(movieId: \\\"$MOVIE_ID\\\", personId: \\\"$CREDIT_PERSON\\\", role: WRITER) { id } }\"}"
  [ -n "$CREDIT_PERSON" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { deletePerson(id: \\\"$CREDIT_PERSON\\\") }\"}" | grep -q '"deletePerson":true' \
    && pass "E4-S3 deletePerson removes the person across both services" || fail "E4 throwaway person left behind ($CREDIT_PERSON)"
  [ -n "$MOVIE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"{ movie(id: \\\"$MOVIE_ID\\\") { credits { id } } }\"}" | grep -q '"credits":\[\]' \
    && pass "E4-S3 their credits went with them" || fail "E4-S3 credits of a deleted person left behind"
  [ -n "$MOVIE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { deleteMovie(id: \\\"$MOVIE_ID\\\") }\"}" | grep -q '"deleteMovie":true' \
    && pass "E2-S5 deleteMovie removes it" || fail "E2-S5 deleteMovie live delete (movie $MOVIE_ID may be left behind)"
  [ -n "$MOVIE_ID" ] && [ ! -f "$ROOT/backend/data/posters/$MOVIE_ID.jpg" ] \
    && pass "E3-S1 poster file removed with the movie" || fail "E3-S1 poster file left behind"
  [ -n "$MOVIE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { deleteMovie(id: \\\"$MOVIE_ID\\\") }\"}" | grep -q '"code":"NOT_FOUND"' \
    && pass "E2-S5 deleting it again answers NOT_FOUND" || fail "E2-S5 second delete should be NOT_FOUND"
  # E1-S2 + E1-S4: the live write path end to end. Create a throwaway person, delete it, and prove it is gone.
  SMOKE_ID=$(curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d '{"query":"mutation { createPerson(input: { name: \"verify smoke\" }) { id } }"}' | sed -n 's/.*"id":"\([0-9]*\)".*/\1/p')
  [ -n "$SMOKE_ID" ] && pass "E1-S2 createPerson writes a person (id $SMOKE_ID)" || fail "E1-S2 createPerson live create"
  [ -n "$SMOKE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { deletePerson(id: \\\"$SMOKE_ID\\\") }\"}" | grep -q '"deletePerson":true' \
    && pass "E1-S4 deletePerson removes it" || fail "E1-S4 deletePerson live delete (person $SMOKE_ID may be left behind)"
  [ -n "$SMOKE_ID" ] && curl -s -X POST localhost:8080/graphql -H 'Content-Type: application/json' -d "{\"query\":\"mutation { deletePerson(id: \\\"$SMOKE_ID\\\") }\"}" | grep -q '"code":"NOT_FOUND"' \
    && pass "E1-S4 deleting it again answers NOT_FOUND" || fail "E1-S4 second delete should be NOT_FOUND"
  curl -s -D - -o /dev/null -X OPTIONS localhost:8080/graphql -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST' | grep -qi "Access-Control-Allow-Origin: http://localhost:5173" \
    && pass "CORS allows the frontend dev origin" || fail "CORS preflight"
  [ "$(curl -s -o /dev/null -w '%{http_code}' -L localhost:8080/swagger-ui.html)" = "200" ] && pass "Swagger UI reachable" || fail "Swagger UI (/swagger-ui.html)"
  [ "$(curl -s -o /dev/null -w '%{http_code}' -L localhost:8080/graphiql)" = "200" ] && pass "GraphiQL reachable" || fail "GraphiQL (/graphiql)"
  if [ -f "$ROOT/frontend/package.json" ]; then
    (cd "$ROOT/frontend" && npm run dev --silent > "$LOG_DIR/vite.log" 2>&1 &)
    for _ in $(seq 1 30); do curl -sf localhost:5173/ >/dev/null 2>&1 && break; sleep 1; done
    curl -sf localhost:5173/ | grep -q "Movie Hub" && pass "frontend dev server serves the layout" || fail "frontend dev server"
  fi
  cleanup; sleep 1
fi

echo
if [ "$FAILED" -eq 0 ]; then echo "VERIFY: ALL PASSED"; else echo "VERIFY: FAILED (logs in $LOG_DIR)"; fi
exit $FAILED
