package com.apiplatform.ai;

/**
 * Every AI-backed capability exposed by {@code AIController}. Persisted on
 * {@code AIGeneration} rows and used to key rate limits / prompt templates, so it stays
 * a closed enum rather than a free-text string.
 */
public enum AIFeature {
    CURL_GENERATOR,
    POSTMAN_TEST_GENERATOR,
    MOCK_DATA_GENERATOR,
    JSON_VALIDATOR,
    REGEX_GENERATOR,
    SQL_GENERATOR,
    ERROR_LOG_EXPLAINER
}
