# AI Assistant

A local Java/Spring AI assistant with a React chat, CDQ product RAG, and country and weather MCP tools.

## Requirements

- JDK 25.
- Node.js 22.12 or newer, npm and Git.
- Docker with Docker Compose v2.
- [Ollama](https://docs.ollama.com/quickstart).
- A [REST Countries](https://restcountries.com/) API key for its free tier.
- A [WeatherAPI](https://www.weatherapi.com/) API key for its free tier.

## Local setup

Run commands from the repository root.

### 1. Environment variables

Copy `.env.example` to `.env` and fill in all values. The `.env` file is the single
local source of API keys and machine-specific paths; it is ignored by Git.

```bash
cp .env.example .env
```

**VS Code / Cursor:** the included Java launch configurations in
`assistant-app.code-workspace` load `.env` automatically.

**Terminal:** load `.env` into the current shell before starting services or running
backend integration tests:

```bash
set -a
source .env
set +a
```

| Variable                 | Used by            | Value                                       |
| ------------------------ | ------------------ | ------------------------------------------- |
| `REST_COUNTRIES_API_KEY` | Country MCP server | REST Countries API key                      |
| `WEATHER_API_KEY`        | Backend            | WeatherAPI key                              |
| `WEATHER_MCP_SCRIPT`     | Backend            | Absolute path to `mcp-weather/src/index.ts` |

### 2. Local weather MCP server

Clone [semdin/mcp-weather](https://github.com/semdin/mcp-weather) next to this project and install its dependencies.

The backend starts the weather MCP server automatically using `WEATHER_MCP_SCRIPT`
and `WEATHER_API_KEY`.

### 3. PostgreSQL

Start PostgreSQL with pgvector:

```bash
docker compose up -d --wait postgres
```

### 4. Ollama

Start Ollama locally and download the chat and embedding models:

```bash
ollama pull qwen3:4b
ollama pull mxbai-embed-large
```

### 5. Start the application

Start the country MCP server first (port 8081):

```bash
./backend/mvnw -f country-mcp-server/pom.xml spring-boot:run
```

Then start the backend in a separate terminal (port 8080). On the first run, load the
CDQ knowledge into pgvector:

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments=--app.rag.ingestion.enabled=true
```

In an IDE, add `--app.rag.ingestion.enabled=true` to the backend's program arguments.
Wait for `Ingested ... CDQ knowledge documents`. Repeat ingestion after changing the
knowledge file; it replaces existing CDQ embeddings. For subsequent starts, omit the argument:

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
```

Start the frontend in another terminal:

```bash
npm --prefix frontend ci
npm --prefix frontend run dev
```

Open [localhost:5173](http://localhost:5173).

## Tests

Run the automated tests:

```bash
npm --prefix frontend test
./backend/mvnw -f country-mcp-server/pom.xml test
./backend/mvnw -f backend/pom.xml test
```

Backend tests require PostgreSQL, Ollama, the country MCP server and the backend
environment variables from setup. To also run the RAG integration tests against
previously ingested knowledge:

```bash
./backend/mvnw -f backend/pom.xml '-Dtest=Cdq*IT' test
```

## Assistant answers

[ANSWERS.md](ANSWERS.md) contains the recruiting task questions and placeholders for
answers from the running assistant.

## AI usage

I used an AI coding assistant mainly to generate tests, build the frontend, and write documentation.
