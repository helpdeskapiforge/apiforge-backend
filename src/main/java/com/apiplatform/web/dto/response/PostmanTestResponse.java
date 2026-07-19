package com.apiplatform.web.dto.response;

public record PostmanTestResponse(
        Long id,
        String provider,
        String model,
        /** Deterministically-generated assertions (status, content-type, per-field type checks). Always present. */
        String deterministicAssertions,
        /** AI-suggested additional assertions (business rules, variable extraction). Null if no AI provider was available. */
        String aiSuggestedAssertions,
        /** Concatenation of both, ready to paste into a Postman "Tests" tab. */
        String combinedScript
) {
}
