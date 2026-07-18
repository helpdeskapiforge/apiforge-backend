package com.apiplatform.web.dto.response;

import java.time.LocalDateTime;

public record MockLogResponse(
        Long id,
        String method,
        String path,
        Integer statusCode,
        Long durationMs,
        LocalDateTime timestamp,
        String requestBody,
        String responseBody,
        boolean chaosTriggered,
        Long mockServerId
) {
}
