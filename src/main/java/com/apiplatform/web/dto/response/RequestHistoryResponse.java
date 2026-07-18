package com.apiplatform.web.dto.response;

import java.time.LocalDateTime;

public record RequestHistoryResponse(
        Long id,
        String method,
        String url,
        Integer status,
        Long durationMs,
        LocalDateTime timestamp
) {
}
