# One-word commands for the whole project. Docker remains an optional local alternative (ADR 0004).
.PHONY: install up test verify reset down docker-up docker-down docker-reset docker-logs docker-verify

install:       ## one-time: install frontend dependencies (backend needs nothing, the Gradle wrapper downloads itself)
	cd frontend && npm install

up:            ## start people-service, movie-service and the frontend
	@echo "Starting people-service (gRPC :9090, health :8081), movie-service (:8080) and frontend (:5173)"
	@cd backend && ./gradlew :people-service:bootRun -q & \
	 cd backend && ./gradlew :movie-service:bootRun -q & \
	 cd frontend && npm run dev & \
	 wait

test:          ## run every backend and frontend test
	cd backend && ./gradlew test
	cd frontend && npm test

verify:        ## build + tests + live smoke check of both services; the one command to prove nothing is broken
	./scripts/verify.sh

reset:         ## remove the local databases and posters so the next start seeds the sample catalogue (E6-S2)
	rm -rf backend/data
	@echo "backend/data removed; the next make up seeds 10 movies, 20 people and their credits"

down:          ## stop anything started by make up
	-pkill -f "moviehub|movie-service|people-service|vite" 2>/dev/null || true

docker-up:     ## build and start the local Docker stack in the background
	docker compose up --build -d

docker-down:   ## stop containers; keep Docker databases and uploaded posters
	docker compose down

docker-reset:  ## stop containers and delete Docker databases and uploaded posters
	docker compose down -v

docker-logs:   ## follow logs from all three containers
	docker compose logs -f

docker-verify: ## build, start and verify the local Docker stack, including persistence
	./scripts/verify-docker.sh
