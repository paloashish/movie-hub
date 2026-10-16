#!/usr/bin/env bash
# Build and prove the optional local Docker stack, including data and poster persistence.
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="${TMPDIR:-/tmp}/moviehub-docker-verify"
mkdir -p "$LOG_DIR"
FAILED=0
STACK_STARTED=0

pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; FAILED=1; }

finish() {
  if [ "$FAILED" -eq 0 ]; then
    echo "DOCKER VERIFY: ALL PASSED"
  else
    echo "DOCKER VERIFY: FAILED (logs in $LOG_DIR)"
  fi
  exit "$FAILED"
}

wait_for_url() {
  local label="$1"
  local url="$2"
  printf "Waiting for %s" "$label"
  for attempt in $(seq 1 90); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo " ready"
      return 0
    fi
    if ((attempt % 5 == 0)); then
      printf "."
    fi
    sleep 2
  done
  echo " timed out"
  return 1
}

stop_stack() {
  if [ "$STACK_STARTED" -eq 1 ]; then
    (cd "$ROOT_DIR" && docker compose down >/dev/null 2>&1) || true
  fi
}

interrupt() {
  echo
  echo "DOCKER VERIFY: INTERRUPTED (partial logs in $LOG_DIR)"
  exit 130
}

trap stop_stack EXIT
trap interrupt INT TERM

if ! command -v docker >/dev/null 2>&1; then
  fail "Docker CLI is not installed"
  finish
fi

if ! docker info >/dev/null 2>&1; then
  fail "Docker engine is not running"
  finish
fi

cd "$ROOT_DIR"
echo "STEP 1/4  Building images and starting containers"
echo "The first run downloads base images and may take several minutes."
if docker compose up --build -d 2>&1 | tee "$LOG_DIR/compose-up.log"; then
  STACK_STARTED=1
  pass "Docker images build and the stack starts"
else
  fail "Docker stack starts (see $LOG_DIR/compose-up.log)"
  docker compose logs > "$LOG_DIR/compose.log" 2>&1 || true
  finish
fi

echo "STEP 2/4  Waiting for services"
if wait_for_url "movie-service" "http://localhost:8080/actuator/health" \
  && wait_for_url "people-service" "http://localhost:8081/actuator/health" \
  && wait_for_url "frontend" "http://localhost:5173/"; then
  pass "all three containers become reachable"
else
  fail "containers become reachable"
  docker compose logs > "$LOG_DIR/compose.log" 2>&1 || true
  finish
fi

echo "STEP 3/4  Checking endpoints and writing persistence data"
curl -sf http://localhost:8080/actuator/health | grep -q '"UP"' \
  && pass "movie-service health UP" || fail "movie-service health"
curl -sf http://localhost:8081/actuator/health | grep -q '"UP"' \
  && pass "people-service health UP" || fail "people-service health"
curl -sf http://localhost:5173/ | grep -q "Movie Hub" \
  && pass "frontend serves the Movie Hub shell" || fail "frontend shell"
curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ ping }"}' | grep -q '"pong"' \
  && pass "GraphQL ping" || fail "GraphQL ping"
curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ people { id name } }"}' | grep -q '"people":\[' \
  && pass "people query crosses from movie-service to people-service over gRPC" || fail "gRPC-backed people query"
curl -s -D - -o /dev/null -X OPTIONS http://localhost:8080/graphql \
  -H 'Origin: http://localhost:5173' \
  -H 'Access-Control-Request-Method: POST' | grep -qi 'Access-Control-Allow-Origin: http://localhost:5173' \
  && pass "CORS allows the Docker frontend origin" || fail "Docker frontend CORS"

PERSON_ID=$(curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createPerson(input: { name: \"docker verify persistence\" }) { id } }"}' \
  | sed -n 's/.*"id":"\([0-9]*\)".*/\1/p')
[ -n "$PERSON_ID" ] \
  && pass "person is written to the people database (id $PERSON_ID)" || fail "create persistent person"

MOVIE_ID=$(curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createMovie(input: { title: \"docker verify persistence\", releaseYear: 2000 }) { id } }"}' \
  | sed -n 's/.*"id":"\([0-9]*\)".*/\1/p')
[ -n "$MOVIE_ID" ] \
  && pass "movie is written to the movie database (id $MOVIE_ID)" || fail "create persistent movie"

SMOKE_PNG="$LOG_DIR/smoke.png"
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82' > "$SMOKE_PNG"
[ -n "$MOVIE_ID" ] && curl -sf -X POST "http://localhost:8080/movies/$MOVIE_ID/poster" \
  -F "file=@$SMOKE_PNG;type=image/png" | grep -q "/posters/$MOVIE_ID.png?v=" \
  && pass "poster is written to the movie data volume" || fail "persistent poster upload"

echo "STEP 4/4  Recreating containers and checking persisted data"
if docker compose down > "$LOG_DIR/compose-down.log" 2>&1 \
  && docker compose up -d > "$LOG_DIR/compose-restart.log" 2>&1; then
  pass "containers are recreated without deleting volumes"
else
  fail "containers restart with existing volumes"
  finish
fi

if wait_for_url "movie-service" "http://localhost:8080/actuator/health" \
  && wait_for_url "people-service" "http://localhost:8081/actuator/health" \
  && wait_for_url "frontend" "http://localhost:5173/"; then
  pass "recreated containers become reachable"
else
  fail "recreated containers become reachable"
  finish
fi

[ -n "$PERSON_ID" ] && curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d "{\"query\":\"{ person(id: \\\"$PERSON_ID\\\") { name } }\"}" | grep -q '"name":"docker verify persistence"' \
  && pass "people database survives container recreation" || fail "people database persistence"
[ -n "$MOVIE_ID" ] && curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d "{\"query\":\"{ movie(id: \\\"$MOVIE_ID\\\") { title posterUrl } }\"}" | grep -q '"title":"docker verify persistence"' \
  && pass "movie database survives container recreation" || fail "movie database persistence"
[ -n "$MOVIE_ID" ] && [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:8080/posters/$MOVIE_ID.png")" = "200" ] \
  && pass "uploaded poster survives container recreation" || fail "poster persistence"

[ -n "$PERSON_ID" ] && curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d "{\"query\":\"mutation { deletePerson(id: \\\"$PERSON_ID\\\") }\"}" | grep -q '"deletePerson":true' \
  && pass "persistent person is cleaned up" || fail "persistent person cleanup"
[ -n "$MOVIE_ID" ] && curl -sf -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d "{\"query\":\"mutation { deleteMovie(id: \\\"$MOVIE_ID\\\") }\"}" | grep -q '"deleteMovie":true' \
  && pass "persistent movie and poster are cleaned up" || fail "persistent movie cleanup"

docker compose logs > "$LOG_DIR/compose.log" 2>&1 || true
finish