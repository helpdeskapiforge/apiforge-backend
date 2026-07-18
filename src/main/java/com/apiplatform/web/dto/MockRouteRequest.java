package com.apiplatform.web.dto;

public record MockRouteRequest(
        Long mockServerId,
        String method,
        String path,
        Integer statusCode,
        String contentType,
        String responseBody,
        String responseHeaders,
        Integer delayMs,
        Boolean isEnabled,
        Boolean chaosEnabled,
        Double failureRate
) {
}
