-- V1__init.sql
-- Baseline schema for APIForge. This mirrors what Hibernate previously auto-generated
-- via ddl-auto=update, now made explicit, reviewable, and versioned. From this point
-- forward, all schema changes go through a new Flyway migration file (see CONTRIBUTING.md)
-- rather than relying on Hibernate to guess the right DDL against a live database.

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255),
    avatar_url  VARCHAR(255),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE workspaces (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255),
    description VARCHAR(1000),
    owner_id    BIGINT REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_workspaces_owner_id ON workspaces (owner_id);

CREATE TABLE collections (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    workspace_id BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE
);
CREATE INDEX idx_collections_workspace_id ON collections (workspace_id);

CREATE TABLE request_items (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    method        VARCHAR(10)  NOT NULL,
    url           TEXT         NOT NULL,
    headers       TEXT,
    body          TEXT,
    auth_config   TEXT,
    collection_id BIGINT NOT NULL REFERENCES collections (id) ON DELETE CASCADE,
    workspace_id  BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE
);
CREATE INDEX idx_request_items_collection_id ON request_items (collection_id);
CREATE INDEX idx_request_items_workspace_id ON request_items (workspace_id);

CREATE TABLE environments (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255),
    variables    TEXT,
    workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE
);
CREATE INDEX idx_environments_workspace_id ON environments (workspace_id);

CREATE TABLE mock_servers (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255),
    port         INTEGER,
    path_prefix  VARCHAR(64),
    workspace_id BIGINT REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT uq_mock_servers_path_prefix UNIQUE (path_prefix)
);
CREATE INDEX idx_mock_servers_workspace_id ON mock_servers (workspace_id);

CREATE TABLE mock_routes (
    id                BIGSERIAL PRIMARY KEY,
    method            VARCHAR(10),
    path              VARCHAR(500),
    status_code       INTEGER,
    response_body     TEXT,
    content_type      VARCHAR(255),
    response_headers  TEXT,
    delay_ms          INTEGER DEFAULT 0,
    is_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    is_chaos_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    failure_rate      DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    description       TEXT,
    mock_server_id    BIGINT REFERENCES mock_servers (id) ON DELETE CASCADE
);
CREATE INDEX idx_mock_routes_server_id ON mock_routes (mock_server_id);
-- Speeds up the exact match lookup MockRouteRepository.findMatchingRoute performs on
-- every single simulated request.
CREATE INDEX idx_mock_routes_match ON mock_routes (mock_server_id, method, path) WHERE is_enabled = TRUE;

CREATE TABLE mock_logs (
    id                  BIGSERIAL PRIMARY KEY,
    method              VARCHAR(10),
    path                VARCHAR(500),
    status_code         INTEGER,
    duration_ms         BIGINT,
    timestamp           TIMESTAMP,
    request_body        TEXT,
    response_body       TEXT,
    is_chaos_triggered  BOOLEAN NOT NULL DEFAULT FALSE,
    mock_server_id      BIGINT REFERENCES mock_servers (id) ON DELETE CASCADE
);
CREATE INDEX idx_mock_logs_server_id_timestamp ON mock_logs (mock_server_id, timestamp DESC);

CREATE TABLE request_history (
    id          BIGSERIAL PRIMARY KEY,
    method      VARCHAR(10),
    url         VARCHAR(2048),
    status      INTEGER,
    duration_ms BIGINT,
    timestamp   TIMESTAMP,
    user_id     BIGINT REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_request_history_user_id_timestamp ON request_history (user_id, timestamp DESC);
