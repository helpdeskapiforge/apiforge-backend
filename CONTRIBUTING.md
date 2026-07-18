# Contributing to APIForge

Thanks for considering a contribution! This document covers the basics of getting
set up and the conventions this repo follows.

## Getting Started

```bash
git clone https://github.com/helpdeskapiforge/apiforge-backend.git
cd apiforge-backend
cp .env.example .env      # then edit values as needed
docker compose up --build
```

The API will be available at `http://localhost:8080`, with interactive API docs at
`http://localhost:8080/swagger-ui.html`.

Running without Docker requires a local PostgreSQL 16 instance; point `DB_URL`,
`DB_USERNAME`, and `DB_PASSWORD` at it, then:

```bash
./mvnw spring-boot:run
```

## Running Tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database (see `src/test/resources/application-test.properties`)
and never touch a real Postgres instance.

## Project Structure

```
src/main/java/com/apiplatform/
├── controller/     # Thin HTTP layer: request/response mapping only
├── service/        # Business logic + authorization checks
├── repository/     # Spring Data JPA repositories (persistence only)
├── model/          # JPA entities
├── payload/        # Legacy request/response DTOs
├── web/dto/        # Newer request/response DTOs (records)
├── security/       # JWT, authentication, ownership guards, rate limiting, SSRF guard
├── exception/      # Custom exceptions + global exception handler
└── config/         # Cross-cutting @Configuration classes
```

See `docs/ARCHITECTURE.md` for a deeper walkthrough and request-flow diagram.

## Database Migrations

Schema changes go through Flyway, not `hibernate.ddl-auto`. To add a migration:

1. Add a new file to `src/main/resources/db/migration`, named
   `V<next-number>__short_description.sql` (e.g. `V2__add_user_roles.sql`).
2. Never edit a migration that has already been merged — add a new one instead.
3. Keep migrations backward-compatible where possible (e.g. add nullable columns,
   backfill, then tighten constraints in a later migration) so rolling deploys
   don't break mid-rollout.

## Coding Conventions

- **Constructor injection only** — no `@Autowired` field injection in new code.
- **Controllers stay thin.** Validation of business rules and authorization
  checks belong in the `service` layer, not the controller.
- **Every workspace-scoped resource must go through `OwnershipGuard`** (or a
  service method that calls it) before being read, updated, or deleted. This is
  the most important rule in the codebase — see `AUDIT.md` for why.
- **Never return a JPA entity that carries secrets** (e.g. `User.password`) —
  use a DTO instead, or make sure the field is `@JsonIgnore`d as defense in depth.
- Prefer Java records for new request/response DTOs.
- Add or update tests for any behavior change, especially around authorization.

## Pull Requests

- Keep PRs focused — one logical change per PR.
- Update `CHANGELOG.md` under "Unreleased".
- Make sure `./mvnw test` passes and add tests for new behavior.
- Fill out the PR template; link any related issue.

## Code of Conduct

By participating, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).
