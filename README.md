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
container — no local Java or Postgres install required.

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

The mock simulator endpoint (`/api/mock/simulator/...`) is deliberately **not**
versioned — it's meant to be a stable, shareable URL for whoever's testing against a
given mock server, and versioning it would mean every mock server's public URL changes
every time the API version bumps.

A ready-to-import Postman collection will live at `docs/postman_collection.json`
(generate one from `/v3/api-docs` via Postman's "Import from OpenAPI" if it's not
present yet in your checkout).

---

## 🐳 Docker

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
