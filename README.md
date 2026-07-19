# APIForge — Backend 🛡️

[![CI](https://github.com/helpdeskapiforge/apiforge-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/helpdeskapiforge/apiforge-backend/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

The **APIForge Backend** is the engine behind the APIForge platform: API design,
collaborative collections, a dynamic mock server with fault injection, and a
CORS-bypassing request proxy — all behind a stateless, JWT-secured REST API.

> **Looking for what changed recently, or why?** See [`AUDIT.md`](AUDIT.md) for a full
> security/architecture review of this codebase, and [`CHANGELOG.md`](CHANGELOG.md)
> for the running list of fixes and improvements.

---

## ⚡️ Core Engines

### 1. 🎭 Dynamic Mock Simulator
When a request hits `/api/mock/simulator/{prefix}/**`:
- **Route matching** against the user-defined routes for that mock server.
- **Latency injection** — simulate slow networks with a configurable delay (capped at
  30s to prevent a misconfigured route from tying up a server thread indefinitely).
- **Chaos Monkey** — an optional configurable probability of returning a 500 to test
  client resilience.
- **Configured response** — user-defined status code, headers, and body.

This endpoint is intentionally public (no auth) so a shared mock URL can be used by
anyone testing against it — see [`AUDIT.md`](AUDIT.md) for why that's a deliberate
trade-off, not an oversight.

### 2. 🌐 Request Proxy Service
Bypasses browser CORS restrictions when testing third-party APIs:
- The frontend sends request details to the backend.
- The backend validates the target URL (`UrlSafetyValidator` blocks internal/private
  network and cloud-metadata targets — see [`AUDIT.md`](AUDIT.md)), executes the call
  with bounded timeouts, and returns status/timing/headers/body.

### 3. 🔐 Security & Auth
- Stateless JWT bearer authentication (Spring Security).
- BCrypt password hashing.
- Per-resource ownership checks on every workspace-scoped entity (`OwnershipGuard`).
- Rate-limited login endpoint.

### 4. 🤖 AI Tools
Seven AI-backed developer tools behind a pluggable provider abstraction (see
[AI Providers](#-ai-providers) below):
- **cURL Generator** — natural language ("create a user") → a runnable curl command.
- **Postman Test Generator** — pastes an actual response and gets back real
  `pm.test()` assertions. Status/content-type/field-type checks are generated
  **deterministically** from the response itself (not hallucinated); the AI only adds
  what static analysis can't (business-rule checks, variable extraction).
- **Mock Data Generator** — natural language or an example JSON → realistic, nested,
  large-batch, edge-case, or intentionally-invalid test data. Falls back to a built-in
  heuristic "faker" (no AI required) when given a JSON shape and no provider is configured.
- **JSON Validator** — syntax and structural (missing/extra fields, type mismatches)
  validation is deterministic (Jackson-based, not AI); the AI only turns the findings
  into a plain-English explanation and a suggested fix.
- **Regex Generator** — natural language → a regex pattern, explanation, and example
  matches/non-matches.
- **SQL Generator** — natural language → a single SQL statement for MySQL, PostgreSQL,
  or SQLite.
- **Error Log Explainer** — paste a Java/Spring/Node/Docker/Kubernetes/Postgres/
  Redis/NGINX stack trace or log excerpt → cause, fix, and an example.

Every call is rate-limited per user, logged (provider/model/latency/tokens), and
persisted to `ai_generations` for history.

---

## 🛠️ Tech Stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JJWT |
| API Docs | springdoc-openapi (Swagger UI) |
| Build | Maven |

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for a deeper walkthrough, including
a request-flow diagram and a description of every package.

---

## 🚀 Getting Started

### Option A — Docker Compose (recommended)

```bash
git clone https://github.com/helpdeskapiforge/apiforge-backend.git
cd apiforge-backend
cp .env.example .env
docker compose up --build
```

The API comes up on `http://localhost:8080`, backed by a disposable Postgres
container — no local Java or Postgres install required. This also starts a bundled
Ollama container with `gemma3` auto-pulled, so **AI Tools work out of the box too** —
see [AI Providers](#-ai-providers) below (first startup takes a few minutes while the
model downloads; subsequent ones are instant).

Then, in the separate `apiforge-frontend` repo: `cp .env.example .env.local && docker
compose up --build` (or `npm install && npm run dev`) brings up the UI on
`http://localhost:3000`, already pointed at this API.

### Option B — Run locally with your own Postgres

**Prerequisites:** JDK 21+, a running PostgreSQL 16 instance.

```bash
cp .env.example .env   # then edit DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET
export $(grep -v '^#' .env | xargs)  # load them into your shell
./mvnw spring-boot:run
```

Every configuration value has a sane local-dev default baked into
`application.properties`; see [`.env.example`](.env.example) for the full list and
[`SECURITY.md`](SECURITY.md) for what **must** be changed before deploying anywhere
real.

### Running Tests

```bash
./mvnw test
```

Tests run fully in-memory against H2 — no external services required.

### API Documentation

Once running, interactive docs are available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Health & Metrics

- `GET /actuator/health` — liveness/readiness (used by the Docker `HEALTHCHECK`)
- `GET /actuator/info`
- `GET /actuator/metrics`

---

## 🔌 API Reference (Overview)

All endpoints are versioned under `/api/v1` and (except where noted) require an
`Authorization: Bearer <token>` header. The previous unversioned `/api/...` paths still
work during a deprecation window but new code should target `/api/v1`.

Endpoints marked **paginated** return a `PageResponse` envelope —
`{ data: [...], page, size, totalElements, totalPages, hasNext }` — and accept
`?page=0&size=50` query params (size is capped at 200 server-side).

| Module | Method | Endpoint | Description |
|---|---|---|---|
| Auth | `POST` | `/api/v1/auth/signup` | Register a new user *(public)* |
| Auth | `POST` | `/api/v1/auth/signin` | Log in, receive a JWT *(public, rate-limited)* |
| User | `GET` | `/api/v1/user/me` | Current user's profile |
| User | `PUT` | `/api/v1/user/profile` | Update name/password |
| Workspaces | `GET` | `/api/v1/workspaces/my-workspaces` | List your workspaces |
| Workspaces | `POST` | `/api/v1/workspaces/create` | Create a workspace |
| Workspaces | `PUT` \| `DELETE` | `/api/v1/workspaces/{id}` | Rename / delete a workspace |
| Collections | `GET` | `/api/v1/collections/workspace/{workspaceId}` | List collections |
| Collections | `POST` \| `PUT` \| `DELETE` | `/api/v1/collections/...` | Manage collections |
| Requests | `GET` | `/api/v1/requests/collection/{collectionId}` | List saved requests **(paginated)** |
| Requests | `GET` | `/api/v1/requests/{id}` | Get a single saved request |
| Requests | `POST` \| `PUT` \| `DELETE` | `/api/v1/requests/...` | Manage saved requests |
| Environments | `GET` | `/api/v1/environments/workspace/{workspaceId}` | List environments |
| Environments | `POST` \| `PUT` \| `DELETE` | `/api/v1/environments/...` | Manage environments |
| Mock Servers | `GET` \| `POST` \| `PUT` \| `DELETE` | `/api/v1/mocks/servers/...` | Manage mock servers |
| Mock Routes | `GET` \| `POST` \| `PUT` \| `DELETE` | `/api/v1/mocks/routes/...` | Manage mock routes |
| Mock Logs | `GET` | `/api/v1/logs/server/{serverId}` | Traffic logs for a mock server **(paginated)** |
| Mock Logs | `GET` | `/api/v1/logs/{id}` | Get a single log entry |
| Simulator | `ALL` | `/api/mock/simulator/{prefix}/**` | The simulated endpoint itself *(public, unversioned — see below)* |
| Proxy | `POST` | `/api/v1/proxy/execute` | Execute a real HTTP request server-side |
| History | `GET` | `/api/v1/history/me` | Your proxied-request history **(paginated)** |
| History | `GET` | `/api/v1/history/{id}` | Get a single history entry |
| AI Tools | `POST` | `/api/v1/ai/curl` | Natural language → curl command |
| AI Tools | `POST` | `/api/v1/ai/postman-tests` | Actual response → `pm.test()` assertions |
| AI Tools | `POST` | `/api/v1/ai/mock-data` | Description/shape → generated JSON test data |
| AI Tools | `POST` | `/api/v1/ai/json-validate` | Validate JSON syntax + structure, get an AI-suggested fix |
| AI Tools | `POST` | `/api/v1/ai/regex` | Natural language → regex pattern + explanation |
| AI Tools | `POST` | `/api/v1/ai/sql` | Natural language → SQL (MySQL/PostgreSQL/SQLite) |
| AI Tools | `POST` | `/api/v1/ai/explain-error` | Stack trace / log excerpt → cause, fix, example |
| AI Tools | `GET` | `/api/v1/ai/history` | Your past AI generations **(paginated)** |
| AI Tools | `GET` | `/api/v1/ai/providers/status` | Which AI providers are configured/available right now |

The mock simulator endpoint (`/api/mock/simulator/...`) is deliberately **not**
versioned — it's meant to be a stable, shareable URL for whoever's testing against a
given mock server, and versioning it would mean every mock server's public URL changes
every time the API version bumps.

A ready-to-import Postman collection will live at `docs/postman_collection.json`
(generate one from `/v3/api-docs` via Postman's "Import from OpenAPI" if it's not
present yet in your checkout).

---

## 🤖 AI Providers

**Zero setup, using Docker (recommended):** `docker compose up` starts everything —
Postgres, the API, **and** a local Ollama server with `gemma3` auto-pulled on first
run. Nothing to install, no API key required. First startup takes a few minutes while
the model downloads (a few GB); `docker compose logs -f ollama-pull` shows progress.
After that it's cached in a volume, so future startups are instant.

AI Tools resolve providers in priority order (`AI_PROVIDER_PRIORITY`, default
`ollama,lmstudio,openrouter,gemini`) and use the **first one that's actually
available** — local/free providers are preferred so nothing leaves your machine and no
external quota is spent unless nothing local is running. If the first available
provider's actual call fails (not just its reachability check — e.g. Ollama is up but
the requested model was never pulled), the request automatically falls back to the
next available provider in the chain rather than failing outright; only if every
available provider fails does the request return an error, listing what was tried.
Every provider is optional: with zero configuration the app still boots and every
non-AI feature works normally; `GET /api/v1/ai/providers/status` reports what's
currently usable, and AI endpoints return `502` with a clear message if none are.

| Provider | Cost | Setup |
|---|---|---|
| **Ollama** (default #1) | Free, local | **Via Docker: already done** (see above). Running the backend bare instead? Install [ollama.com](https://ollama.com), run `ollama pull gemma3`, leave it running on `:11434`. |
| **LM Studio** (default #2) | Free, local | Install from [lmstudio.ai](https://lmstudio.ai), load a model, start the local server (default `:1234`). No env vars needed. |
| **OpenRouter** (default #3) | Free tier | Create a key at [openrouter.ai/keys](https://openrouter.ai/keys), set `OPENROUTER_API_KEY`. Defaults to a `:free`-suffixed model. |
| **Gemini** (default #4) | Free tier | Create a key at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey), set `GEMINI_API_KEY`. Easiest to get running with no local install. |

### ⚠️ If AI Tools say "no provider available" (the #1 cause)

**`OLLAMA_BASE_URL=http://localhost:11434` almost never works if the backend itself is
running in a container.** Inside a container, `localhost` means *that container*, not
your host machine and not a sibling `ollama` container — this is the single most common
reason Ollama "won't connect" even though `ollama run gemma3` works fine in your terminal.

- **Running the whole stack via this repo's `docker compose up`**: already handled —
  `docker-compose.yml` points the `api` service at `http://ollama:11434` (the Docker
  service name, resolved via Docker's internal DNS) automatically. Nothing to configure.
- **Running the backend in Docker some other way, against an Ollama install on your host**:
  set `OLLAMA_BASE_URL=http://host.docker.internal:11434` (Docker Desktop on
  Mac/Windows) — on Linux, add `extra_hosts: ["host.docker.internal:host-gateway"]` to
  your service, or use your host's LAN IP.
- **Running the backend bare** (`./mvnw spring-boot:run` / `java -jar`, no Docker) with
  Ollama installed directly on the same machine: the default `http://localhost:11434`
  is correct as-is.
- **Model name mismatch**: `OLLAMA_MODEL` must exactly match a model you've actually
  pulled (`ollama list` to check). The default is `gemma3`; if you're using a different
  local model, set `OLLAMA_MODEL` accordingly.
- **Gemini "invalid model id"**: Google periodically retires older Gemini model
  versions (e.g. `gemini-2.0-flash` was retired in 2026). If `GEMINI_MODEL` stops
  working, check [ai.google.dev/gemini-api/docs/models](https://ai.google.dev/gemini-api/docs/models)
  for the current free-tier model name and update `GEMINI_MODEL`.

All settings live under `ai.*` in `application.yml`, bound from the env vars in
[`.env.example`](.env.example) (`AI_REQUEST_TIMEOUT_SECONDS`, `AI_MAX_PROMPT_CHARS`,
`AI_RATE_LIMIT_MAX` / `AI_RATE_LIMIT_WINDOW_SECONDS` for the per-user rate limit).

### How it's wired

Every feature service talks only to the `AIProvider` interface
(`com.apiplatform.ai.AIProvider`), never to a concrete HTTP client — `AIProviderResolver`
picks the first available implementation by name from `ai.provider-priority`.
Prompts live in dedicated classes under `com.apiplatform.ai.prompt` (never inline in a
service), so they can be reviewed/iterated on independently of request-handling code.

### Adding a new provider

1. Implement `AIProvider` (`complete`, `isAvailable`, `getProviderName`, `getModel`) in
   `com.apiplatform.ai.provider`, annotated `@Component`.
2. Add its config block to `AIProperties` and `application.yml` (base URL / API key /
   model, following the existing pattern).
3. Add its name to `AI_PROVIDER_PRIORITY` (or the default in `application.yml`).

Nothing else changes — no feature service, controller, or prompt template references a
concrete provider.

---



```bash
docker build -t apiforge-backend .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/apiforge \
  -e DB_USERNAME=apiforge -e DB_PASSWORD=apiforge \
  -e JWT_SECRET=$(openssl rand -base64 48) \
  apiforge-backend
```

The image runs as a non-root user and exposes a container `HEALTHCHECK` against
`/actuator/health`.

---

## 🤝 Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for setup, coding conventions, and how
database migrations work. Please also read our [Code of Conduct](CODE_OF_CONDUCT.md).

## 🔒 Security

Found a vulnerability? Please see [`SECURITY.md`](SECURITY.md) for how to report it
responsibly — not as a public GitHub issue.

## 📄 License

MIT — see [`LICENSE`](LICENSE).

---

Originally created by [Sumit Shresht](https://github.com/sumitshresht).
