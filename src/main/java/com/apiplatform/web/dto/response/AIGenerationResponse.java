package com.apiplatform.web.dto.response;

import java.time.Instant;

public record AIGenerationResponse(
        Long id,
        String feature,
        String provider,
        String model,
        String result,
        Integer tokensUsed,
        Long latencyMs,
        Instant createdAt
) {
}
