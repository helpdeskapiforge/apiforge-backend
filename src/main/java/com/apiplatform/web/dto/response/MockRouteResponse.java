package com.apiplatform.web.dto.response;

public record MockRouteResponse(
        Long id,
        String method,
        String path,
        Integer statusCode,
        String contentType,
        String responseBody,
        String responseHeaders,
        Integer delayMs,
        boolean isEnabled,
        boolean chaosEnabled,
        double failureRate,
        String description,
        Long mockServerId
) {
}
