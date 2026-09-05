# assistant-app

Monorepo: Spring Boot / Maven (`backend`) + React / Vite / npm (`frontend`).
Each application manages its own dependencies; npm commands run in `frontend`.

## Requirements

- JDK 25 (`java -version`; set `JAVA_HOME` to this JDK).
- Node.js 22.12 or newer and npm.
- Docker with Docker Compose v2, running locally.
- [Ollama](https://docs.ollama.com/quickstart), running locally.

Maven is provided by `backend/mvnw` (`mvnw.cmd` on Windows).
The first setup requires internet access to download dependencies and the model.

## Local setup

Run Docker commands from the repository root.

### 1. PostgreSQL

```bash
docker compose up -d --wait postgres
```

PostgreSQL is available at `localhost:5432`, with database `assistant` and
local development credentials `postgres` / `postgres`.
On a fresh data volume, `docker/init-pgvector.sql` enables the `vector` extension.
The mount requires an existing script file and exposes it read-only.

For a database volume created before this script was added, apply it once without
deleting existing data:

```bash
docker compose exec -T postgres psql -U postgres -d assistant -v ON_ERROR_STOP=1 < docker/init-pgvector.sql
```

Verify the extension:

```bash
docker compose exec -T postgres psql -U postgres -d assistant -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';"
```

### 2. Ollama

Start the Ollama application or run `ollama serve` in a separate terminal if the
service is not already running. The backend connects to `http://localhost:11434`.
Download the configured chat model:

```bash
ollama pull qwen3:4b
```

### 3. Backend

In a new terminal, starting from the repository root:

```bash
cd backend
./mvnw spring-boot:run
```

The backend listens on `http://localhost:8080` and exposes `POST /api/chat`.

### 4. Frontend

In another terminal, starting from the repository root:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` requests to the backend on port
8080 during development. The UI sends messages to `POST /api/chat` and shows
the conversation.

## Verification

From the repository root:

```bash
docker compose config --quiet
npm --prefix frontend run lint
npm --prefix frontend run build
```

Compile the backend without starting external services:

```bash
cd backend
./mvnw compile
```

Run `./mvnw test` from `backend` with PostgreSQL and Ollama running; the existing
test loads the Spring application context.

## VS Code

Open `assistant-app.code-workspace` to load the backend, frontend, and repository
folders. Install the Java and Maven extensions to use the backend launch
configuration and the `backend: compile` / `backend: spring-boot:run` tasks.
Shared editor configuration lives in `.vscode/settings.json`,
`.vscode/tasks.json`, and `.vscode/launch.json`; these files and the workspace
file should be included when committing the repository setup.

## Stopping local services

Stop backend and frontend terminals with Ctrl+C. From the repository root:

```bash
docker compose stop postgres
```

The database data remains in the named Docker volume.
