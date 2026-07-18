# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Core Features pass — collection folder nesting

- **`collections.parent_id` + `sort_order`** (`V2__collection_folder_nesting.sql`) —
  a "folder" is now just a Collection whose `parentId` points at another Collection in
  the same workspace. Plain FK column, not a JPA relationship (avoids infinite-JSON-
  recursion / lazy-loading chain issues for a self-referencing entity).
- **`CollectionService`** gained: creating inside a parent (with same-workspace
  validation), moving a collection to a new parent (`PUT` with `parentId`), clearing a
  parent back to root (`clearParent: true`), and cycle prevention (`assertNoCycle`,
  also caps nesting at 25 levels as a defensive limit against pathological input).
- **Found and fixed a second instance of the shared-DTO validation bug** from the
  previous pass: `CollectionRequest.name` was `@NotBlank`, but a move-only `PUT` (just
  `{parentId}`, no `name`) is exactly the request shape the frontend's "move to folder"
  action sends. Same fix pattern as before — validation moved into
  `CollectionService.createCollection` explicitly, DTO relaxed for update/move.
- Tests: `CollectionFolderNestingIT` — nested creation, move, cycle rejection, move-
  without-name (the case that used to 400), clear-parent-to-root, create-without-name
  rejection.

### Phase 0 — API versioning, pagination, response DTOs (this pass)

- **API versioning**: every endpoint now answers on both `/api/v1/...` and the legacy
  unversioned `/api/...` (kept temporarily for a deprecation window — the frontend has
  been switched over to `/api/v1`). The public mock simulator endpoint
  (`/api/mock/simulator/{prefix}/**`) deliberately stays unversioned, since it's meant
  to be a stable, shareable URL.
- **Response DTOs everywhere**: every controller now returns a purpose-built response
  record (`WorkspaceResponse`, `CollectionResponse`, `RequestItemResponse`,
  `EnvironmentResponse`, `MockServerResponse`, `MockRouteResponse`, `MockLogResponse`,
  `RequestHistoryResponse`) via a new `ResponseMapper`, instead of serializing JPA
  entities directly. This was tracked as a known limitation in the previous pass —
  it's closed now. `Workspace.owner` (a full nested entity) no longer appears in any
  response; only `ownerId` does.
- **Pagination** added to the three list endpoints that can genuinely grow unbounded:
  - `GET /requests/collection/{id}` (a collection can accumulate hundreds of saved requests)
  - `GET /logs/server/{id}` (mock traffic logs)
  - `GET /history/me` (request history)

  All three now return a `PageResponse<T>` envelope (`{ data, page, size,
  totalElements, totalPages, hasNext }`) instead of a raw array, with `page`/`size`
  query parameters (size capped at 200 server-side regardless of what's requested).
  Deliberately offset-based, not cursor/keyset — see `PageResponse`'s Javadoc for why
  that's the right call at this data volume. Collections, environments, mock servers,
  and mock routes are left unpaginated for now (workspace-scoped, naturally small).
- **Two new endpoints** added to close gaps the frontend was working around with
  fragile client-side searches: `GET /logs/{id}` and `GET /history/{id}`, both
  ownership-checked. Previously the frontend fetched an entire page of logs/history and
  searched it client-side for a single item by id — which silently failed for anything
  not on the first page, and would have hard-broken the moment the endpoints above
  became paginated instead of returning raw arrays.

### Fixed

- Two validation rules from the last pass (`ApiRequestDto.url` `@NotBlank`,
  `CollectionRequest`/`ApiRequestDto` `@NotNull` on parent IDs) would have broken
  "create new request" and "rename collection" respectively, because those DTOs are
  shared between create and update and the frontend's update calls don't send every
  field create requires. Fixed by relaxing the DTOs and validating explicitly, only on
  create, in the service layer.
- Added `PUT /api/v1/workspaces/{id}` (rename) — the frontend's Settings screen already
  called this; it never existed.

### Known limitations carried forward

- Pagination is offset-based; revisit as keyset/cursor pagination if a table's size or
  write-concurrency ever makes that the better trade-off (see `PageResponse` Javadoc).
- Collections/environments/mock-servers/mock-routes lists remain unpaginated by design
  for this phase — flag if any workspace in practice accumulates hundreds of them.

### Security
- **Fixed Broken Object Level Authorization (IDOR/BOLA) across every resource type**
  (workspaces, collections, requests, environments, mock servers, mock routes, mock
  logs). Every endpoint now verifies the authenticated user owns the resource (or its
  parent workspace) before returning, modifying, or deleting it.
- Added SSRF protection (`UrlSafetyValidator`) to `/api/proxy/execute`, blocking
  loopback, private, link-local, and cloud-metadata targets, plus non-HTTP(S) schemes.
- Fixed CORS configuration: removed the contradictory `@CrossOrigin(origins = "*")`
  annotations scattered across controllers; origins are now controlled from a single
  configurable allow-list (`CORS_ALLOWED_ORIGINS`).
- Added connect/read timeouts to the outbound proxy HTTP client (previously unbounded,
  a trivial thread-exhaustion DoS vector).
- Added a startup check that rejects a JWT signing secret shorter than 256 bits.
- Added a per-IP+email rate limiter on `/api/auth/signin` (documented as
  single-instance; see `SECURITY.md` for scaling guidance).
- `User.password` is now `@JsonIgnore`d as defense in depth.
- Generic 401 response for bad credentials — no longer distinguishes "wrong password"
  from "unknown email" in a way that would aid account enumeration.

### Added
- Global exception handling (`GlobalExceptionHandler`) with a consistent JSON error
  envelope (`ApiError`) across the entire API, including field-level validation errors.
- Service layer (`com.apiplatform.service`) separating business logic and authorization
  from HTTP concerns — controllers are now thin.
- Flyway migrations (`db/migration/V1__init.sql`) with explicit indexes on every
  foreign key and the mock-route lookup path; `hibernate.ddl-auto` changed from
  `update` to `validate`.
- Spring Boot Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`).
- OpenAPI/Swagger UI documentation (`/swagger-ui.html`).
- Docker Compose stack for local development (API + Postgres).
- `.env.example` documenting every configuration variable.
- GitHub Actions CI workflow (build + test on every push/PR).
- `AUDIT.md`, `docs/ARCHITECTURE.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`,
  `SECURITY.md`, issue templates, and a pull request template.
- Test suite: SSRF-guard unit tests and an auth-flow integration test (H2, no external
  services required).

### Changed
- Bumped `jjwt` from 0.11.5 (deprecated builder API) to 0.12.6 (current fluent API).
- `AuthTokenFilter` now uses an explicit SLF4J logger instead of the inherited
  commons-logging `logger` field, which silently ignored SLF4J-style `{}` placeholders.
- `POST /api/auth/signup` now returns `201 Created` with no body (previously returned
  a plain-text `200 OK` message) — **breaking change** for existing frontend clients.
- `POST /api/workspaces/create`, `/api/collections/create`, `/api/requests/create`,
  `/api/environments/create`, `/api/mocks/servers/create`, `/api/mocks/routes/create`
  now return `201 Created` instead of `200 OK`.
- Dockerfile now runs as a non-root user and includes a `HEALTHCHECK`.

### Known Limitations (tracked as follow-up work, see `AUDIT.md`)
- Entities are still returned directly from several endpoints instead of dedicated
  response DTOs (acceptable for now since sensitive fields are `@JsonIgnore`d, but a
  future pass should decouple the API contract from the persistence model).
- No role-based access control yet — every user is simply the sole owner of their own
  workspaces; sharing/collaboration is out of scope for this pass.
- `LoginRateLimiter` does not coordinate across multiple instances.
