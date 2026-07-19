-- V3__ai_generations.sql
-- Persists every AI tool invocation (cURL generator, Postman test generator, mock data
-- generator, JSON validator, ...) for history/audit purposes. See AIGeneration.java and
-- README.md > AI Providers > Logging.

CREATE TABLE ai_generations (
    id            BIGSERIAL PRIMARY KEY,
    feature       VARCHAR(40)  NOT NULL,
    provider      VARCHAR(40)  NOT NULL,
    model         VARCHAR(120),
    prompt        TEXT,
    result        TEXT,
    tokens_used   INTEGER,
    latency_ms    BIGINT,
    success       BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_generations_user_id_created_at ON ai_generations (user_id, created_at DESC);
CREATE INDEX idx_ai_generations_feature ON ai_generations (feature);
